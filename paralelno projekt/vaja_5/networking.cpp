#include "networking.h"
#include "blockchain.h"
#include "rendering.h"
#include <iostream>
#include <algorithm>
#include <format>

namespace networking {

    bool run_app = true;
    std::string local_uuid;
    int local_port = -1;
    tcp_server* server = nullptr;
    std::thread server_thread;
    std::mutex receive_threads_mtx;
    std::vector<std::thread> receive_threads;

    std::mutex connections_mtx;
    std::vector<tcp_conn> connections;

    // Sends a packet to a single TCP connection.
    bool unicast(const tcp_connection* c, const packet_view& msg) {
        bool status = false;

        try {
            size_t msg_size = msg.size();
            c->send_t(&msg_size, sizeof(size_t));
            c->send_t(msg.data(), msg_size);
            status = true;
        }
        catch (const tcp_ex::socket_disconnected& e) {
            std::cout << "Client disconnected: " << e.what() << std::endl;
        }
        catch (const tcp_ex::send_error& e) {
            std::cout << "Send error: " << e.what() << std::endl;
        }

        return status;
    }

    // Broadcasts a packet to all connected TCP peers.
    void broadcast(const packet_view& msg) {
        connections_mtx.lock();

        for (const tcp_conn& c : connections) {
            unicast(c.conn, msg);
        }

        connections_mtx.unlock();
    }

    // Main server thread that accepts incoming TCP connections.
    void server_thread_proc() {
        for (int i = SERVER_START_PORT; i < 0x10000; i++) {
            server = new tcp_server(i);

            if (server->init() == true) {
                break;
            }

            delete server;
        }

        if (server == nullptr) {
            std::cout << "Server start-up failed!" << std::endl;
            return;
        }

        local_port = (int)server->get_port();

        rendering::add_log(std::format("Local node listening on port {}", local_port));

        while (run_app) {
            tcp_socket remote_node_conn;

            if (server->wait_for_connection(remote_node_conn) == false) {
                std::cout << "Failed to accept connection!" << std::endl;
                continue;
            }

            connections_mtx.lock();
            receive_threads_mtx.lock();

            tcp_connection* conn = new tcp_connection(server, remote_node_conn);
            connections.push_back({ conn });
            receive_threads.emplace_back(receive_thread_proc, conn);

            receive_threads_mtx.unlock();
            connections_mtx.unlock();
        }

        connections_mtx.lock();

        for (tcp_conn& c : connections) {
            delete c.conn;
        }

        connections.clear();
        connections_mtx.unlock();

        receive_threads_mtx.lock();

        for (auto& t : receive_threads) {
            t.join();
        }

        receive_threads.clear();

        receive_threads_mtx.unlock();

        delete server;
    }

    // Removes a TCP connection from the active connections list.
    void remove_connection(tcp_connection* conn) {
        auto self_it = std::find_if(connections.begin(), connections.end(), [&](const tcp_conn& e) { return e.conn == conn; });

        if (self_it == connections.end()) {
            std::cout << "Error: Connection not present in list!" << std::endl;
            return;
        }

        connections.erase(self_it);
        delete conn;
    }

    // Receives and processes packets from a TCP connection.
    void receive_thread_proc(tcp_connection* conn) {
        uint8_t* msg_buf = (uint8_t*)malloc(MAX_PACKET_SIZE);

        if (msg_buf == nullptr) {
            std::cout << "Error: out of memory!" << std::endl;
            return;
        }

        try {
            packet_view id_msg(sizeof(opcode) + packet_view::get_str_size(local_uuid));
            id_msg.write<opcode>(opcode::id);
            id_msg.write<std::string>(local_uuid);
            unicast(conn, id_msg);

            rendering::add_log("Sent uuid");

            while (run_app) {
                conn->recieve_t(msg_buf, sizeof(size_t), true);
                size_t msg_size = *(size_t*)msg_buf;

                conn->recieve_t(msg_buf, msg_size, true);
                packet_view msg(msg_buf, msg_size);

                opcode op = msg.read<opcode>();

                switch (op) {
                    case opcode::id:
                    {
                        std::lock_guard<std::mutex> conn_guard(connections_mtx);
                        std::string conn_id = msg.read<std::string>();

                        if (conn_id == local_uuid) {
                            conn->disconnect();
                            rendering::add_log("ERROR", "Can't connect to self!", { 255, 0, 0, 255 });
                            break;
                        }

                        for (auto it = connections.begin(); it != connections.end(); it++) {
                            if (it->conn != conn) {
                                if (it->id == conn_id) {
                                    conn->disconnect();
                                    rendering::add_log("ERROR", std::format("Connection with {} already exists!", conn_id), { 255, 0, 0, 255 });
                                    break;
                                }
                                continue;
                            }

                            it->id = conn_id;
                            rendering::add_log(std::format("Set connection id {} for {:p}, {}", conn_id, (void*)conn, connections.size()));
                            break;
                        }
                    }
                    break;

                    case opcode::sync:
                    {
                        auto conn_it = std::find_if(connections.begin(), connections.end(), [&](const tcp_conn& c) { return c.conn == conn; });

                        if (conn_it == connections.end()) {
                            std::cout << "Error: Received sync from an unidentified node!" << std::endl;
                            conn->disconnect();
                            break;
                        }

                        blockchain::handle_sync_packet(conn_it->id, msg);
                    }
                    break;

                    default:
                    {
                        std::cout << "Error: unknown packet type!" << std::endl;
                        conn->disconnect();
                    }
                    break;
                }
            }
        }
        catch (const read_exception& e) {
            std::cout << "Packet read exception: " << e.what() << std::endl;
        }
        catch (const write_exception& e) {
            std::cout << "Packet write exception: " << e.what() << std::endl;
        }
        catch (const tcp_ex::socket_disconnected& e) {
            std::cout << "Client disconnected: " << e.what() << std::endl;
        }
        catch (const tcp_ex::recv_error& e) {
            std::cout << "Receive error: " << e.what() << std::endl;
        }
        catch (const tcp_ex::send_error& e) {
            std::cout << "Send error: " << e.what() << std::endl;
        }

        std::lock_guard<std::mutex> conn_guard(connections_mtx);
        remove_connection(conn);
        free(msg_buf);
    }

    // Initiates a TCP connection to a remote node.
    void connect_to_node(const std::string& remote_ip, int remote_port) {
        tcp_client* client = new tcp_client();

        if (client->connect(remote_ip, remote_port) == false) {
            std::cout << std::format("Failed to connect to {}:{}", remote_ip, remote_port) << std::endl;
            delete client;
            return;
        }

        connections_mtx.lock();
        receive_threads_mtx.lock();

        tcp_connection* conn = new tcp_connection(client);
        connections.push_back({ conn });
        receive_threads.emplace_back(receive_thread_proc, conn);

        receive_threads_mtx.unlock();
        connections_mtx.unlock();
    }

    // Shuts down the TCP server and closes all connections.
    void shutdown() {
        run_app = false;
        if (server != nullptr) { 
            server->shutdown(); 
        }
    }

}

