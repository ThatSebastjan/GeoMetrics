#include <WinSock2.h>
#include <windows.h>
#include <string>
#include <iostream>
#include <chrono>

#include "networking.h"
#include "blockchain.h"
#include "mining.h"
#include "rendering.h"
#include "utils.h"
#include "mpi_networking.h"

int main(int argc, char* argv[]) {
    bool mpi_enabled = mpi_networking::init(&argc, &argv);
    
    if (mpi_enabled) {
        std::cout << "Running with MPI: rank " << mpi_networking::get_rank() 
                  << " of " << mpi_networking::get_size() << " nodes" << std::endl;
    }
    
    networking::local_uuid = generate_uuid_v4();
    std::cout << "Local UUID: " << networking::local_uuid << std::endl;

    if (rendering::init() == false) {
        std::cout << "Init error!" << std::endl;
        if (mpi_enabled) {
            mpi_networking::broadcast_shutdown();
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            mpi_networking::finalize();
        }
        return 1;
    }

    ShowWindow(GetConsoleWindow(), SW_HIDE);

    networking::server_thread = std::thread(networking::server_thread_proc);

    mining::start_sync_thread();
    
    if (mpi_enabled) {
        mpi_networking::start_receive_thread();
    }

    static bool is_mining = false;

    while (rendering::done == false && !mpi_networking::should_shutdown()) {
        if (rendering::handle_messages() == false) {
            continue;
        }

        rendering::begin_frame();

        rendering::render_ui(
            networking::local_port,
            networking::local_uuid,
            is_mining,
            []() {
                mining::mining_thread = std::thread(mining::mine);
            },
            [](const std::string& ip, int port) {
                networking::connect_to_node(ip, port);
            }
        );

        rendering::render_frame();
    }
    
    std::cout << "Rank " << (mpi_enabled ? mpi_networking::get_rank() : 0) << " exiting main loop, initiating shutdown..." << std::endl;
    
    if (mpi_enabled) {
        mpi_networking::broadcast_shutdown();
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }
    
    rendering::cleanup();

    std::cout << "Rank " << (mpi_enabled ? mpi_networking::get_rank() : 0) << " cleaning up..." << std::endl;

    networking::shutdown();
    if (networking::server_thread.joinable()) {
        networking::server_thread.join();
    }
    mining::stop_mining();
    
    if (mpi_enabled) {
        mpi_networking::stop_receive_thread();
        std::cout << "Rank " << mpi_networking::get_rank() << " calling MPI_Finalize..." << std::endl;
        mpi_networking::finalize();
        std::cout << "Rank " << mpi_networking::get_rank() << " MPI_Finalize completed" << std::endl;
    }

    std::cout << "Rank " << (mpi_enabled ? mpi_networking::get_rank() : 0) << " exiting normally" << std::endl;
    return 0;
}
