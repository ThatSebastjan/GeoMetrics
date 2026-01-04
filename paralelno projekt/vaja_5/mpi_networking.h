#ifndef MPI_NETWORKING_INCLUDE
#define MPI_NETWORKING_INCLUDE

#include <mpi.h>
#include <vector>
#include <thread>
#include <atomic>
#include <mutex>
#include "block.h"

namespace mpi_networking {

    enum mpi_tag {
        MPI_TAG_BLOCK_FOUND = 1,
        MPI_TAG_CHAIN_SYNC = 2,
        MPI_TAG_CHAIN_REQUEST = 3,
        MPI_TAG_SHUTDOWN = 4
    };

    bool init(int* argc, char*** argv);
    void finalize();
    bool is_initialized();
    int get_rank();
    int get_size();
    void start_receive_thread();
    void stop_receive_thread();
    void broadcast_block(const block* b);
    void broadcast_chain(const std::vector<block*>& chain, int difficulty, size_t prev_diff_adjust_len);
    void broadcast_shutdown();
    bool should_shutdown();
    void process_messages();
    size_t calculate_cumulative_difficulty(const std::vector<block*>& chain);
    
    extern std::atomic<bool> mpi_run_app;

};

#endif

