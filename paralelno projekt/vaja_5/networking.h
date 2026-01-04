#ifndef NETWORKING_INCLUDE
#define NETWORKING_INCLUDE

#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include "tcp_server.h"
#include "tcp_client.h"
#include "tcp_connection.h"
#include "tcp_exceptions.h"
#include "packets.h"

namespace networking {

    extern bool run_app;
    constexpr int SERVER_START_PORT = 1234;
    extern std::string local_uuid;
    extern int local_port;
    extern tcp_server* server;
    extern std::thread server_thread;
    extern std::mutex receive_threads_mtx;
    extern std::vector<std::thread> receive_threads;

    struct tcp_conn {
        std::string id;
        tcp_connection* conn;

        tcp_conn(tcp_connection* c_conn) : id(""), conn(c_conn) {};
    };

    extern std::mutex connections_mtx;
    extern std::vector<tcp_conn> connections;

    void receive_thread_proc(tcp_connection* conn);
    bool unicast(const tcp_connection* c, const packet_view& msg);
    void broadcast(const packet_view& msg);
    void server_thread_proc();
    void remove_connection(tcp_connection* conn);
    void connect_to_node(const std::string& remote_ip, int remote_port);
    void shutdown();

};

#endif

