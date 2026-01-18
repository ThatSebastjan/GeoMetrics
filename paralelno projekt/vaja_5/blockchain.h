#ifndef BLOCKCHAIN_INCLUDE
#define BLOCKCHAIN_INCLUDE

#include <vector>
#include <list>
#include <mutex>
#include <chrono>
#include "block.h"
#include "packets.h"

namespace blockchain {

    constexpr int BLOCK_GEN_INTERVAL = 10;
    constexpr int DIFFICULTY_ADJUST_INTERVAL = 10;

    extern std::mutex block_chain_mtx;
    extern int current_difficulty;
    extern std::vector<block*> block_list;
    extern size_t prev_diff_adjustment_chain_length;

    struct pending_entry {
        int difficulty;
        std::vector<block*> chain;
        size_t prev_diff_adjust_len;
        std::string sender;
    };

    struct mining_request {
        std::string data;
    };

    extern std::mutex pending_list_mtx;
    extern std::vector<pending_entry> pending_list;

    extern std::list<mining_request> mining_request_list; //guarded by block_chain_mtx
    extern std::atomic<double> local_hash_rate;
    extern std::atomic<double> global_hash_rate;

    void free_chain(std::vector<block*>& c);
    bool is_block_valid(const block* b, const block* previous);
    bool is_chain_valid(const std::vector<block*> chain);
    bool adjust_difficulty();
    void broadcast_local_chain();
    void handle_sync_packet(const std::string& sender_id, packet_view& msg);
    void sync();
    int get_connection_count();
    size_t get_block_count();
    int get_difficulty();
    const std::vector<block*>& get_block_list();

};

#endif

