#ifndef MINING_INCLUDE
#define MINING_INCLUDE

#include <thread>
#include <vector>
#include <atomic>
#include "block.h"

namespace mining {

    extern std::thread mining_thread;
    extern std::thread sync_thread;
    extern unsigned int num_mining_threads;

    void on_block_mined(block* b);
    void mine();
    void start_sync_thread();
    void stop_mining();
    void stop_sync();

};

#endif

