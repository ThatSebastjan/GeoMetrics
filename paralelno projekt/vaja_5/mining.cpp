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
        int block_difficulty
    ) {
        size_t nonce = thread_id;
        
        while (!solution_found.load(std::memory_order_relaxed) && networking::run_app) {
            sha256_hash hash = block::hash_block_data(block_index, block_ts, block_data, block_previous_hash, block_difficulty, nonce);
            
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
    }

    // Continuously mines new blocks using multiple worker threads.
    void mine() {
        
        while (networking::run_app) {
            blockchain::block_chain_mtx.lock();

            size_t block_index = blockchain::block_list.size();
            auto block_ts = std::chrono::system_clock::now();
            std::string block_data = std::format("Block#{}", block_index);
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

            for (unsigned int i = 0; i < num_threads; ++i) {
                workers.emplace_back(
                    mining_worker,
                    i,
                    num_threads,
                    block_index,
                    block_data,
                    block_ts,
                    block_previous_hash,
                    block_difficulty
                );
            }

            for (auto& worker : workers) {
                worker.join();
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

