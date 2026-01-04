#include "mpi_networking.h"
#include "blockchain.h"
#include "rendering.h"
#include <iostream>
#include <format>
#include <cstring>
#include <thread>
#include <chrono>

namespace mpi_networking {

    std::atomic<bool> mpi_run_app{true};
    std::atomic<bool> mpi_shutdown_flag{false};
    std::thread mpi_receive_thread;
    std::mutex mpi_mtx;
    
    static bool mpi_initialized = false;
    static int mpi_rank = -1;
    static int mpi_size = -1;

    // Initializes the MPI environment with thread support.
    bool init(int* argc, char*** argv) {
        int provided;
        int result = MPI_Init_thread(argc, argv, MPI_THREAD_MULTIPLE, &provided);
        
        if (result != MPI_SUCCESS) {
            std::cerr << "MPI_Init_thread failed!" << std::endl;
            return false;
        }
        
        if (provided < MPI_THREAD_MULTIPLE) {
            std::cerr << "Warning: MPI_THREAD_MULTIPLE not available, got: " << provided << std::endl;
        }
        
        MPI_Comm_rank(MPI_COMM_WORLD, &mpi_rank);
        MPI_Comm_size(MPI_COMM_WORLD, &mpi_size);
        
        mpi_initialized = true;
        
        std::cout << "MPI initialized: rank " << mpi_rank << " of " << mpi_size << " nodes" << std::endl;
        
        return true;
    }

    // Finalizes and shuts down the MPI environment.
    void finalize() {
        if (mpi_initialized) {
            MPI_Finalize();
            mpi_initialized = false;
        }
    }

    bool is_initialized() {
        return mpi_initialized;
    }

    int get_rank() {
        return mpi_rank;
    }

    int get_size() {
        return mpi_size;
    }

    // Calculates the total cumulative difficulty of a blockchain.
    size_t calculate_cumulative_difficulty(const std::vector<block*>& chain) {
        size_t total_difficulty = 0;
        for (const block* b : chain) {
            total_difficulty += 1ui64 << b->difficulty;
        }
        return total_difficulty;
    }

    // Broadcasts a single mined block to all other MPI ranks.
    void broadcast_block(const block* b) {
        if (!mpi_initialized) return;
        
        try {
            size_t block_size = sizeof(size_t) + sizeof(int64_t) + 2 * sizeof(sha256_hash) + 
                              sizeof(int) + sizeof(size_t) + sizeof(size_t) + b->data.length();
            
            std::vector<uint8_t> combined_buffer(sizeof(size_t) + block_size);
            size_t offset = 0;
            
            memcpy(combined_buffer.data() + offset, &block_size, sizeof(size_t));
            offset += sizeof(size_t);
            
            memcpy(combined_buffer.data() + offset, &b->index, sizeof(size_t));
            offset += sizeof(size_t);
            
            int64_t timestamp_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                b->timestamp.time_since_epoch()).count();
            memcpy(combined_buffer.data() + offset, &timestamp_ms, sizeof(int64_t));
            offset += sizeof(int64_t);
            
            memcpy(combined_buffer.data() + offset, b->hash.bytes, sizeof(sha256_hash));
            offset += sizeof(sha256_hash);
            
            memcpy(combined_buffer.data() + offset, b->previous_hash.bytes, sizeof(sha256_hash));
            offset += sizeof(sha256_hash);
            
            memcpy(combined_buffer.data() + offset, &b->difficulty, sizeof(int));
            offset += sizeof(int);
            
            memcpy(combined_buffer.data() + offset, &b->nonce, sizeof(size_t));
            offset += sizeof(size_t);
            
            size_t data_len = b->data.length();
            memcpy(combined_buffer.data() + offset, &data_len, sizeof(size_t));
            offset += sizeof(size_t);
            
            memcpy(combined_buffer.data() + offset, b->data.data(), data_len);
            offset += data_len;
            
            for (int dest = 0; dest < mpi_size; dest++) {
                if (dest != mpi_rank) {
                    int result = MPI_Send(combined_buffer.data(), static_cast<int>(combined_buffer.size()), 
                                         MPI_BYTE, dest, MPI_TAG_BLOCK_FOUND, MPI_COMM_WORLD);
                    if (result != MPI_SUCCESS) {
                        std::cerr << "MPI_Send failed for block to rank " << dest << std::endl;
                    }
                }
            }
            
        } catch (const std::exception& e) {
            std::cerr << "Error broadcasting block via MPI: " << e.what() << std::endl;
        }
    }

    // Broadcasts the entire blockchain to all other MPI ranks for synchronization.
    void broadcast_chain(const std::vector<block*>& chain, int difficulty, size_t prev_diff_adjust_len) {
        if (!mpi_initialized) return;
        
        try {
            size_t total_size = sizeof(int) + 2 * sizeof(size_t);
            
            for (const block* b : chain) {
                total_size += sizeof(size_t) + sizeof(int64_t) + 2 * sizeof(sha256_hash) + 
                             sizeof(int) + sizeof(size_t) + sizeof(size_t) + b->data.length();
            }
            
            std::vector<uint8_t> combined_buffer(sizeof(size_t) + total_size);
            size_t offset = 0;
            
            memcpy(combined_buffer.data() + offset, &total_size, sizeof(size_t));
            offset += sizeof(size_t);
            
            memcpy(combined_buffer.data() + offset, &difficulty, sizeof(int));
            offset += sizeof(int);
            
            size_t chain_size = chain.size();
            memcpy(combined_buffer.data() + offset, &chain_size, sizeof(size_t));
            offset += sizeof(size_t);
            
            memcpy(combined_buffer.data() + offset, &prev_diff_adjust_len, sizeof(size_t));
            offset += sizeof(size_t);
            
            for (const block* b : chain) {
                memcpy(combined_buffer.data() + offset, &b->index, sizeof(size_t));
                offset += sizeof(size_t);
                
                int64_t timestamp_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                    b->timestamp.time_since_epoch()).count();
                memcpy(combined_buffer.data() + offset, &timestamp_ms, sizeof(int64_t));
                offset += sizeof(int64_t);
                
                memcpy(combined_buffer.data() + offset, b->hash.bytes, sizeof(sha256_hash));
                offset += sizeof(sha256_hash);
                
                memcpy(combined_buffer.data() + offset, b->previous_hash.bytes, sizeof(sha256_hash));
                offset += sizeof(sha256_hash);
                
                memcpy(combined_buffer.data() + offset, &b->difficulty, sizeof(int));
                offset += sizeof(int);
                
                memcpy(combined_buffer.data() + offset, &b->nonce, sizeof(size_t));
                offset += sizeof(size_t);
                
                size_t data_len = b->data.length();
                memcpy(combined_buffer.data() + offset, &data_len, sizeof(size_t));
                offset += sizeof(size_t);
                
                memcpy(combined_buffer.data() + offset, b->data.data(), data_len);
                offset += data_len;
            }
            
            for (int dest = 0; dest < mpi_size; dest++) {
                if (dest != mpi_rank) {
                    int result = MPI_Send(combined_buffer.data(), static_cast<int>(combined_buffer.size()), 
                                         MPI_BYTE, dest, MPI_TAG_CHAIN_SYNC, MPI_COMM_WORLD);
                    if (result != MPI_SUCCESS) {
                        std::cerr << "MPI_Send failed for chain to rank " << dest << std::endl;
                    }
                }
            }
            
        } catch (const std::exception& e) {
            std::cerr << "Error broadcasting chain via MPI: " << e.what() << std::endl;
        }
    }

    // Processes incoming MPI messages in a continuous loop until shutdown.
    void process_messages() {
        while (mpi_run_app.load()) {
            MPI_Status status;
            int flag = 0;
            
            int probe_result = MPI_Iprobe(MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &flag, &status);
            
            if (probe_result != MPI_SUCCESS) {
                std::cerr << "MPI_Iprobe failed with error code " << probe_result << std::endl;
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            }
            
            if (flag) {
                if (status.MPI_TAG == MPI_TAG_SHUTDOWN) {
                    uint8_t shutdown_signal;
                    int recv_result = MPI_Recv(&shutdown_signal, 1, MPI_BYTE, status.MPI_SOURCE, 
                            MPI_TAG_SHUTDOWN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                    
                    if (recv_result != MPI_SUCCESS) {
                        std::cerr << "MPI_Recv failed for shutdown signal" << std::endl;
                        continue;
                    }
                    
                    std::cout << "Rank " << mpi_rank << " received shutdown signal from rank " 
                              << status.MPI_SOURCE << std::endl;
                    mpi_shutdown_flag.store(true);
                    
                } else if (status.MPI_TAG == MPI_TAG_BLOCK_FOUND) {
                    int count = 0;
                    int count_result = MPI_Get_count(&status, MPI_BYTE, &count);
                    
                    if (count_result != MPI_SUCCESS || count <= 0) {
                        std::cerr << "MPI_Get_count failed or invalid count for block" << std::endl;
                        uint8_t dummy;
                        MPI_Recv(&dummy, 1, MPI_BYTE, status.MPI_SOURCE, 
                                MPI_TAG_BLOCK_FOUND, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                        continue;
                    }
                    
                    std::vector<uint8_t> combined_buffer(count);
                    int recv_result = MPI_Recv(combined_buffer.data(), count, MPI_BYTE, 
                            status.MPI_SOURCE, MPI_TAG_BLOCK_FOUND, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                    
                    if (recv_result != MPI_SUCCESS) {
                        std::cerr << "MPI_Recv failed for block data" << std::endl;
                        continue;
                    }
                    
                    size_t offset = 0;
                    size_t block_size = 0;
                    
                    if (combined_buffer.size() < sizeof(size_t)) {
                        std::cerr << "Invalid buffer size for block" << std::endl;
                        continue;
                    }
                    
                    memcpy(&block_size, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    if (offset + block_size != combined_buffer.size()) {
                        std::cerr << "Buffer size mismatch for block" << std::endl;
                        continue;
                    }
                    
                    size_t index;
                    memcpy(&index, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    int64_t timestamp_ms;
                    memcpy(&timestamp_ms, combined_buffer.data() + offset, sizeof(int64_t));
                    offset += sizeof(int64_t);
                    auto timestamp = std::chrono::system_clock::time_point(std::chrono::milliseconds(timestamp_ms));
                    
                    sha256_hash hash;
                    memcpy(hash.bytes, combined_buffer.data() + offset, sizeof(sha256_hash));
                    offset += sizeof(sha256_hash);
                    
                    sha256_hash previous_hash;
                    memcpy(previous_hash.bytes, combined_buffer.data() + offset, sizeof(sha256_hash));
                    offset += sizeof(sha256_hash);
                    
                    int difficulty;
                    memcpy(&difficulty, combined_buffer.data() + offset, sizeof(int));
                    offset += sizeof(int);
                    
                    size_t nonce;
                    memcpy(&nonce, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    size_t data_len;
                    memcpy(&data_len, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    if (offset + data_len > combined_buffer.size()) {
                        std::cerr << "Invalid data length for block" << std::endl;
                        continue;
                    }
                    
                    std::string data((char*)(combined_buffer.data() + offset), data_len);
                    offset += data_len;
                    
                    block* new_block = new block(index, data, timestamp, hash, previous_hash, difficulty, nonce);
                    
                    blockchain::block_chain_mtx.lock();
                    const block* previous_block = (blockchain::block_list.size() > 0) ? 
                        blockchain::block_list.back() : nullptr;
                    
                    if (blockchain::is_block_valid(new_block, previous_block)) {
                        blockchain::block_list.push_back(new_block);
                        rendering::add_log(std::format("Received valid block from MPI node {}", status.MPI_SOURCE), 
                                          { 0, 255, 0, 255 });
                        blockchain::adjust_difficulty();
                    } else {
                        delete new_block;
                        rendering::add_log(std::format("Received invalid block from MPI node {}", status.MPI_SOURCE), 
                                          { 255, 0, 0, 255 });
                    }
                    blockchain::block_chain_mtx.unlock();
                    
                } else if (status.MPI_TAG == MPI_TAG_CHAIN_SYNC) {
                    int count = 0;
                    int count_result = MPI_Get_count(&status, MPI_BYTE, &count);
                    
                    if (count_result != MPI_SUCCESS || count <= 0) {
                        std::cerr << "MPI_Get_count failed or invalid count for chain" << std::endl;
                        uint8_t dummy;
                        MPI_Recv(&dummy, 1, MPI_BYTE, status.MPI_SOURCE, 
                                MPI_TAG_CHAIN_SYNC, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                        continue;
                    }
                    
                    std::vector<uint8_t> combined_buffer(count);
                    int recv_result = MPI_Recv(combined_buffer.data(), count, MPI_BYTE, 
                            status.MPI_SOURCE, MPI_TAG_CHAIN_SYNC, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                    
                    if (recv_result != MPI_SUCCESS) {
                        std::cerr << "MPI_Recv failed for chain data" << std::endl;
                        continue;
                    }
                    
                    size_t offset = 0;
                    size_t chain_size_bytes = 0;
                    
                    if (combined_buffer.size() < sizeof(size_t)) {
                        std::cerr << "Invalid buffer size for chain" << std::endl;
                        continue;
                    }
                    
                    memcpy(&chain_size_bytes, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    if (offset + chain_size_bytes != combined_buffer.size()) {
                        std::cerr << "Buffer size mismatch for chain" << std::endl;
                        continue;
                    }
                    
                    int difficulty;
                    memcpy(&difficulty, combined_buffer.data() + offset, sizeof(int));
                    offset += sizeof(int);
                    
                    size_t chain_size;
                    memcpy(&chain_size, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    size_t prev_diff_adjust_len;
                    memcpy(&prev_diff_adjust_len, combined_buffer.data() + offset, sizeof(size_t));
                    offset += sizeof(size_t);
                    
                    blockchain::pending_entry p_entry;
                    p_entry.sender = std::format("MPI_node_{}", status.MPI_SOURCE);
                    p_entry.difficulty = difficulty;
                    p_entry.prev_diff_adjust_len = prev_diff_adjust_len;
                    p_entry.chain.reserve(chain_size);
                    
                    bool parse_error = false;
                    for (size_t i = 0; i < chain_size; i++) {
                        if (offset + sizeof(size_t) > combined_buffer.size()) {
                            std::cerr << "Buffer overflow while parsing chain block " << i << std::endl;
                            parse_error = true;
                            break;
                        }
                        
                        size_t index;
                        memcpy(&index, combined_buffer.data() + offset, sizeof(size_t));
                        offset += sizeof(size_t);
                        
                        int64_t timestamp_ms;
                        memcpy(&timestamp_ms, combined_buffer.data() + offset, sizeof(int64_t));
                        offset += sizeof(int64_t);
                        auto timestamp = std::chrono::system_clock::time_point(std::chrono::milliseconds(timestamp_ms));
                        
                        sha256_hash hash;
                        memcpy(hash.bytes, combined_buffer.data() + offset, sizeof(sha256_hash));
                        offset += sizeof(sha256_hash);
                        
                        sha256_hash previous_hash;
                        memcpy(previous_hash.bytes, combined_buffer.data() + offset, sizeof(sha256_hash));
                        offset += sizeof(sha256_hash);
                        
                        int block_difficulty;
                        memcpy(&block_difficulty, combined_buffer.data() + offset, sizeof(int));
                        offset += sizeof(int);
                        
                        size_t nonce;
                        memcpy(&nonce, combined_buffer.data() + offset, sizeof(size_t));
                        offset += sizeof(size_t);
                        
                        size_t data_len;
                        memcpy(&data_len, combined_buffer.data() + offset, sizeof(size_t));
                        offset += sizeof(size_t);
                        
                        if (offset + data_len > combined_buffer.size()) {
                            std::cerr << "Invalid data length for chain block " << i << std::endl;
                            parse_error = true;
                            break;
                        }
                        
                        std::string data((char*)(combined_buffer.data() + offset), data_len);
                        offset += data_len;
                        
                        block* b = new block(index, data, timestamp, hash, previous_hash, block_difficulty, nonce);
                        p_entry.chain.push_back(b);
                    }
                    
                    if (parse_error) {
                        for (block* b : p_entry.chain) {
                            delete b;
                        }
                        continue;
                    }
                    
                    blockchain::pending_list_mtx.lock();
                    blockchain::pending_list.push_back(p_entry);
                    blockchain::pending_list_mtx.unlock();
                }
            } else {
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
            }
        }
    }

    // Starts a background thread to receive and process MPI messages.
    void start_receive_thread() {
        if (!mpi_initialized) return;
        
        mpi_run_app.store(true);
        mpi_receive_thread = std::thread(process_messages);
    }

    // Stops the MPI message receiving thread and waits for it to complete.
    void stop_receive_thread() {
        mpi_run_app.store(false);
        if (mpi_receive_thread.joinable()) {
            mpi_receive_thread.join();
        }
    }

    // Sends a shutdown signal to all other MPI ranks.
    void broadcast_shutdown() {
        if (!mpi_initialized || mpi_size <= 1) return;
        
        try {
            mpi_shutdown_flag.store(true);
            
            uint8_t shutdown_signal = 1;
            for (int dest = 0; dest < mpi_size; dest++) {
                if (dest != mpi_rank) {
                    MPI_Request req;
                    MPI_Isend(&shutdown_signal, 1, MPI_BYTE, dest, 
                             MPI_TAG_SHUTDOWN, MPI_COMM_WORLD, &req);
                    MPI_Request_free(&req);
                }
            }
            
            std::cout << "Rank " << mpi_rank << " broadcasted shutdown signal" << std::endl;
        } catch (const std::exception& e) {
            std::cerr << "Error broadcasting shutdown: " << e.what() << std::endl;
        }
    }

    bool should_shutdown() {
        return mpi_shutdown_flag.load();
    }
}

