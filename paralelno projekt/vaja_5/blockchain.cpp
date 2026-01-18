#include "blockchain.h"
#include "networking.h"
#include "rendering.h"
#include "mpi_networking.h"
#include <iostream>
#include <algorithm>
#include <format>
#include <thread>

namespace blockchain {

    std::mutex block_chain_mtx;
    int current_difficulty = 4;
    std::vector<block*> block_list;
    size_t prev_diff_adjustment_chain_length = 0;

    std::mutex pending_list_mtx;
    std::vector<pending_entry> pending_list; //List of pending blockchains for validation

    std::list<mining_request> mining_request_list; //Guarded by block_chain_mtx
    std::atomic<double> local_hash_rate = { 0 };
    std::atomic<double> global_hash_rate = { 0 };


    // Frees all blocks in a chain and clears the vector.
    void free_chain(std::vector<block*>& c) {
        for (block* b : c) {
            delete b;
        }
        c.clear();
    }

    // Validates a block against its previous block in the chain.
    bool is_block_valid(const block* b, const block* previous) {
        sha256_hash calculated_hash = block::calculate_hash(*b);
        if (calculated_hash != b->hash) {
            return false;
        }

        if (!b->hash.is_of_difficulty(b->difficulty)) {
            return false;
        }

        auto time_now = std::chrono::system_clock::now();
        int64_t delta_time = std::chrono::duration_cast<std::chrono::milliseconds>(b->timestamp - time_now).count();

        if (delta_time > 60 * 1000) {
            return false;
        }

        if (previous == nullptr) {
            if (b->index != 0) {
                return false;
            }
            return true;
        }

        if (b->index != previous->index + 1) {
            return false;
        }

        if (b->previous_hash != previous->hash) {
            return false;
        }

        int64_t delta_previous = std::chrono::duration_cast<std::chrono::milliseconds>(previous->timestamp - b->timestamp).count();
        if (delta_previous > 60 * 1000) {
            return false;
        }

        return true;
    }

    // Validates an entire blockchain by checking each block sequentially.
    bool is_chain_valid(const std::vector<block*> chain) {
        if (chain.empty()) {
            return true;
        }

        const block* prev = nullptr;

        for (const block* b : chain) {
            if (is_block_valid(b, prev) == false) {
                return false;
            }
            prev = b;
        }

        return true;
    }

    // Adjusts mining difficulty based on block generation rate.
    bool adjust_difficulty() {
        if ((block_list.size() - prev_diff_adjustment_chain_length) < DIFFICULTY_ADJUST_INTERVAL) {
            return false;
        }

        size_t last_adj_idx = (prev_diff_adjustment_chain_length == 0) ? 0 : (prev_diff_adjustment_chain_length - 1);
        
        prev_diff_adjustment_chain_length = block_list.size();

        const block* prev_adjustment_block = block_list[last_adj_idx];
        const block* current_block = block_list.back();

        constexpr int64_t expected_time = BLOCK_GEN_INTERVAL * DIFFICULTY_ADJUST_INTERVAL;
        
        int64_t time_taken = std::chrono::duration_cast<std::chrono::seconds>(
            current_block->timestamp - prev_adjustment_block->timestamp).count();

        int old_diff = current_difficulty;

        if (time_taken < (expected_time / 2)) {
            current_difficulty = current_difficulty + 1;
        }
        else if (time_taken > (expected_time * 2)) {
            if (current_difficulty > 1) {
                current_difficulty = current_difficulty - 1;
            }
        }

        return old_diff != current_difficulty;
    }

    // Broadcasts the local blockchain to all connected peers via TCP and MPI.
    void broadcast_local_chain() {
        auto compute_block_msg_len = [](const block* b) {
            constexpr auto static_elements = (sizeof(size_t) + sizeof(int64_t) + 2 * sizeof(sha256_hash) + sizeof(int) + sizeof(size_t));
            return static_elements + packet_view::get_str_size(b->data);
        };

        try {
            size_t chain_size = 0;

            for (const block* b : block_list) {
                chain_size += compute_block_msg_len(b);
            }

            packet_view sync_msg(sizeof(opcode) + sizeof(int) + 2 * sizeof(size_t) + chain_size);
            sync_msg.write<opcode>(opcode::sync);
            sync_msg.write<int>(current_difficulty);
            sync_msg.write<size_t>(block_list.size());
            sync_msg.write<size_t>(prev_diff_adjustment_chain_length);

            for (const block* b : block_list) {
                sync_msg.write<size_t>(b->index);
                sync_msg.write<std::string>(b->data);
                sync_msg.write<int64_t>(std::chrono::duration_cast<std::chrono::milliseconds>(b->timestamp.time_since_epoch()).count());
                sync_msg.write<sha256_hash>(b->hash);
                sync_msg.write<sha256_hash>(b->previous_hash);
                sync_msg.write<int>(b->difficulty);
                sync_msg.write<size_t>(b->nonce);
            }

            networking::broadcast(sync_msg);
            
            if (mpi_networking::is_initialized()) {
                mpi_networking::broadcast_chain(block_list, current_difficulty, prev_diff_adjustment_chain_length);
            }
        }
        catch (const write_exception& e) {
            std::cout << "Packet write exception: " << e.what() << std::endl;
        }
    }

    // Processes a blockchain synchronization packet received from a peer.
    void handle_sync_packet(const std::string& sender_id, packet_view& msg) {
        pending_entry p_entry;
        p_entry.sender = sender_id;
        p_entry.difficulty = msg.read<int>();

        size_t num_blocks = msg.read<size_t>();
        p_entry.chain.reserve(num_blocks);

        p_entry.prev_diff_adjust_len = msg.read<size_t>();

        for (size_t i = 0; i < num_blocks; i++) {
            size_t block_idx = msg.read<size_t>();
            std::string block_data = msg.read<std::string>();

            int64_t tse_ns = msg.read<int64_t>();
            auto block_ts = std::chrono::system_clock::time_point(std::chrono::milliseconds(tse_ns));

            sha256_hash block_hash = msg.read<sha256_hash>();
            sha256_hash block_prev_hash = msg.read<sha256_hash>();

            int block_difficulty = msg.read<int>();
            size_t block_nonce = msg.read<size_t>();

            block* b = new block(block_idx, block_data, block_ts, block_hash, block_prev_hash, block_difficulty, block_nonce);
            p_entry.chain.push_back(b);
        }

        pending_list_mtx.lock();
        pending_list.push_back(p_entry);
        pending_list_mtx.unlock();
    }

    // Periodically synchronizes the blockchain with peers by comparing chain difficulties.
    void sync() {
        while (networking::run_app) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10 * 1000));

            size_t local_difficulty = 0;

            block_chain_mtx.lock();

            for (const block* b : block_list) {
                local_difficulty += 1ui64 << b->difficulty;
            }

            block_chain_mtx.unlock();

            size_t best_pending_idx = -1;
            size_t best_pending_difficulty = local_difficulty;

            pending_list_mtx.lock();

            for (size_t i = 0; i < pending_list.size(); i++) {
                const pending_entry& c_entry = pending_list[i];
                size_t chain_difficluty = 0;

                if (is_chain_valid(c_entry.chain) == false) {
                    std::cout << "Chain from " << c_entry.sender << " is invalid!" << std::endl;
                    continue;
                }

                for (const block* b : c_entry.chain) {
                    chain_difficluty += 1ui64 << b->difficulty;
                }

                if (chain_difficluty > best_pending_difficulty) {
                    best_pending_difficulty = chain_difficluty;
                    best_pending_idx = i;
                }
            }

            block_chain_mtx.lock();

            if (best_pending_idx != -1) {
                free_chain(block_list);

                const pending_entry& p = pending_list[best_pending_idx];
                block_list = p.chain;
                current_difficulty = p.difficulty;
                prev_diff_adjustment_chain_length = p.prev_diff_adjust_len;

                rendering::add_log(std::format("Synced better chain from {} with {} blocks", p.sender, block_list.size()), { 0, 0, 255, 255 });
                rendering::add_log(std::format("Current difficulty: {}", current_difficulty), { 0, 255, 0, 255 });

                pending_list.erase(pending_list.begin() + best_pending_idx);
            }
            else if(block_list.size() > 0){
                broadcast_local_chain();
                rendering::add_log("Sent local chain as it seems to be the best", { 0, 0, 255, 255 });
            }

            block_chain_mtx.unlock();

            for (pending_entry& p : pending_list) {
                free_chain(p.chain);
            }

            pending_list.clear();
            pending_list_mtx.unlock();
        }
    }

    int get_connection_count() {
        std::lock_guard<std::mutex> lock(networking::connections_mtx);
        return (int)networking::connections.size();
    }

    size_t get_block_count() {
        std::lock_guard<std::mutex> lock(block_chain_mtx);
        return block_list.size();
    }

    int get_difficulty() {
        std::lock_guard<std::mutex> lock(block_chain_mtx);
        return current_difficulty;
    }

    const std::vector<block*>& get_block_list() {
        return block_list;
    }

};

