#include "mining.h"
#include "blockchain.h"
#include "networking.h"
#include "rendering.h"
#include "mpi_networking.h"
#include <format>
#include <chrono>
#include <atomic>
#include <vector>

namespace mining {

    std::thread mining_thread;
    std::thread sync_thread;
    
    static std::atomic<bool> solution_found{false};
    unsigned int num_mining_threads = -1;
    
    struct mining_result {
        size_t nonce;
        sha256_hash hash;
        
        mining_result(size_t n, const sha256_hash& h) : nonce(n), hash(h) {}
    };
    
    static std::atomic<mining_result*> mining_result_ptr{nullptr};

    // Validates and adds a mined block to the blockchain.
    void on_block_mined(block* b) {
        blockchain::block_chain_mtx.lock();

        const block* previous_block = (blockchain::block_list.size() > 0) ? blockchain::block_list.back() : nullptr;

        if (blockchain::is_block_valid(b, previous_block) == true) {
            blockchain::block_list.push_back(b);
            rendering::add_log(std::format("Validated and added block {}", b->data));
            
            if (mpi_networking::is_initialized()) {
                mpi_networking::broadcast_block(b);
            }
        }
        else {
            rendering::add_log("Mined block invalid!");
        }

        if (blockchain::adjust_difficulty() == true) {
            rendering::add_log(std::format("Adjusted difficulty to {}", blockchain::current_difficulty), { 0, 255, 0, 255 });
        }

        blockchain::block_chain_mtx.unlock();
    }
    
    // Worker thread that attempts to find a valid nonce for a block.
    void mining_worker(
        size_t thread_id,
        size_t num_threads,
        size_t block_index,
        const std::string& block_data,
        std::chrono::system_clock::time_point block_ts,
        const sha256_hash& block_previous_hash,
        int block_difficulty,
        size_t* out_worker_hash_rate
    ) {
        size_t nonce = thread_id;
        size_t local_hash_rate = 0;
        
        while (!solution_found.load(std::memory_order_relaxed) && networking::run_app) {
            sha256_hash hash = block::hash_block_data(block_index, block_ts, block_data, block_previous_hash, block_difficulty, nonce);
            local_hash_rate++;
            
            if (hash.is_of_difficulty(block_difficulty)) {
                mining_result* result = new mining_result(nonce, hash);
                mining_result* expected = nullptr;
                
                if (mining_result_ptr.compare_exchange_strong(expected, result, std::memory_order_acq_rel)) {
                    solution_found.store(true, std::memory_order_release);
                    break;
                } else {
                    delete result;
                    break;
                }
            }
            
            if (solution_found.load(std::memory_order_acquire)) {
                break;
            }
            
            nonce += num_threads;
        }

        *out_worker_hash_rate = local_hash_rate;
    }

    // Continuously mines new blocks using multiple worker threads.
    void mine() {
        
        while (networking::run_app) {
            blockchain::block_chain_mtx.lock();

            //Check if we have data to process
            if (blockchain::mining_request_list.empty()) {
                blockchain::block_chain_mtx.unlock();

                std::this_thread::sleep_for(std::chrono::milliseconds(100));
                continue;
            }

            const blockchain::mining_request req = blockchain::mining_request_list.front();
            blockchain::mining_request_list.pop_front();

            size_t block_index = blockchain::block_list.size();
            auto block_ts = std::chrono::system_clock::now();
            std::string block_data = req.data;
            sha256_hash block_previous_hash = (blockchain::block_list.size() > 0) ? blockchain::block_list.back()->hash : sha256_hash{};
            int block_difficulty = blockchain::current_difficulty;

            blockchain::block_chain_mtx.unlock();

            solution_found.store(false, std::memory_order_relaxed);
            mining_result* old_result = mining_result_ptr.exchange(nullptr, std::memory_order_relaxed);
            if (old_result != nullptr) {
                delete old_result;
            }

            //Start configured amount of mining threads
            unsigned int num_threads = num_mining_threads;
            
            if (num_threads == -1) {
                num_threads = std::thread::hardware_concurrency(); //Default fallback - max possible threads
            }

            if (num_threads <= 0) {
                num_threads = 1;
            }

            std::vector<std::thread> workers;
            workers.reserve(num_threads);

            auto start_time = std::chrono::high_resolution_clock::now();
            std::vector<size_t> worker_hash_rates(num_threads, 0);

            for (unsigned int i = 0; i < num_threads; ++i) {
                workers.emplace_back(
                    mining_worker,
                    i,
                    num_threads,
                    block_index,
                    block_data,
                    block_ts,
                    block_previous_hash,
                    block_difficulty,
                    &worker_hash_rates[i]
                );
            }

            for (auto& worker : workers) {
                worker.join();
            }

            //Sum hash rates
            auto end_time = std::chrono::high_resolution_clock::now();
            int64_t elapsed_time = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();
            size_t total_hash_sum = 0;

            for (size_t rate : worker_hash_rates) {
                total_hash_sum += rate;
            }

            elapsed_time = std::max<int64_t>(1, elapsed_time); //Prevent division by zero

            double hash_rate = (double)total_hash_sum / ((double)elapsed_time * 0.001);

            blockchain::local_hash_rate.store(hash_rate); //Store for sending later

            rendering::add_log(std::format("Current hash rate: {:.2f}", hash_rate), { 255, 0, 200, 255 });
            printf("[%d] Current hash rate: %.2f\n", GetCurrentProcessId(), hash_rate);
            std::fflush(stdout);

            //Send local hashrate
            if (mpi_networking::is_initialized()) {
                mpi_networking::broadcast_local_hashrate(hash_rate);
            }


            mining_result* result = mining_result_ptr.load(std::memory_order_acquire);
            if (result != nullptr && solution_found.load(std::memory_order_acquire)) {
                block* new_block = new block(
                    block_index,
                    block_data,
                    block_ts,
                    result->hash,
                    block_previous_hash,
                    block_difficulty,
                    result->nonce
                );
                on_block_mined(new_block);
                
                delete result;
                mining_result_ptr.store(nullptr, std::memory_order_relaxed);
            }
        }
    }

    // Starts the blockchain synchronization thread.
    void start_sync_thread() {
        sync_thread = std::thread(blockchain::sync);
        sync_thread.detach();
    }

    // Stops the mining thread and waits for it to complete.
    void stop_mining() {
        if (mining_thread.joinable()) {
            mining_thread.join();
        }
    }

    void stop_sync() {
    }

};

