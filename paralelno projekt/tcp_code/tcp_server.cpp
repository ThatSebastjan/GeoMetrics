#include "tcp_server.h"
#include "tcp_exceptions.h"



size_t tcp_socket::_id = 0;



bool tcp_server::init() {
    int startup_res = WSAStartup(MAKEWORD(2, 2), &wsa_data); //NOTE: multiple calls to WSAStartup, later WSACleanup

    if (startup_res != 0) {
        std::cerr << "WSAStartup error: " << startup_res << std::endl;
        return false;
    };


    char port_str[6] = {};
    sprintf_s(port_str, "%d", port);

    addrinfo hints = {};
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_PASSIVE;

    addrinfo* addrinfo_result = nullptr;


    int addrinfo_res = getaddrinfo(NULL, port_str, &hints, &addrinfo_result);

    if (addrinfo_res != 0) {
        std::cerr << "getaddrinfo error: " << addrinfo_res << std::endl;
        return false;
    };


    server_socket = socket(addrinfo_result->ai_family, addrinfo_result->ai_socktype, addrinfo_result->ai_protocol);

    if (server_socket == INVALID_SOCKET) {
        std::cerr << "socket error: " << WSAGetLastError() << std::endl;
        freeaddrinfo(addrinfo_result);
        return false;
    };

    //Eexplicitly bind to 127.0.0.1
    sockaddr_in s_addr_in = *(sockaddr_in*)&addrinfo_result->ai_addr;
    //s_addr_in.sin_addr.S_un.S_addr = inet_addr("127.0.0.1");

    if (inet_pton(addrinfo_result->ai_family, "127.0.0.1", &s_addr_in.sin_addr) != 1) {
        std::cerr << "inet_pton error: " << WSAGetLastError() << std::endl;
        freeaddrinfo(addrinfo_result);
        return false;
    };
    
    int bind_res = bind(server_socket, addrinfo_result->ai_addr, (int)addrinfo_result->ai_addrlen);

    if (bind_res == SOCKET_ERROR) {
        std::cerr << "bind error: " << WSAGetLastError() << std::endl;
        freeaddrinfo(addrinfo_result);
        return false;
    };


    if (listen(server_socket, SOMAXCONN) == SOCKET_ERROR) {
        std::cerr << "listen error: " << WSAGetLastError() << std::endl;
        return false;
    };
 
    freeaddrinfo(addrinfo_result);
    return true;
};



//Listen for incoming connections
bool tcp_server::wait_for_connection(tcp_socket& client) const {
    int addr_len = sizeof(sockaddr);
    client.socket = accept(server_socket, &client.addr, &addr_len);

    if (client.socket == INVALID_SOCKET) {
        std::cerr << "accept error: " << WSAGetLastError() << std::endl;
        return false;
    };

    client.connected = true;

    return true;
};



//Read data from a client socket -> Does not throw
size_t tcp_server::recieve(tcp_socket& client, void* buf, size_t size, bool read_full) const {
    size_t bytes_read = 0;

    do {
        int recv_res = recv(client.socket, (char*)buf + bytes_read, (int)(size - bytes_read), 0);

        //socket closed
        if (recv_res == 0) {
            client.connected = false;
            return 0;
        }
        else if (recv_res == SOCKET_ERROR) {
            //std::cerr << "recv error: " << WSAGetLastError() << std::endl;
            //break;
            return -1;
        };

        bytes_read += (size_t)recv_res;

    } while (read_full && (bytes_read < size));

    return bytes_read;
};



//Read data from a client socket -> Throws exceptions!
size_t tcp_server::recieve_t(tcp_socket& client, void* buf, size_t size, bool read_full) const {

    if (client.connected == false) {
        throw tcp_ex::socket_disconnected();
    };

    size_t bytes_read = 0;

    do {
        int recv_res = recv(client.socket, (char*)buf + bytes_read, (int)(size - bytes_read), 0);

        //socket closed
        if (recv_res == 0) {
            client.connected = false;
            throw tcp_ex::socket_disconnected();
        }
        else if (recv_res == SOCKET_ERROR) {
            throw tcp_ex::recv_error(WSAGetLastError());
        };

        bytes_read += (size_t)recv_res;

    } while (read_full && (bytes_read < size));

    return bytes_read;
};



//Try to send data to a client
bool tcp_server::send(const tcp_socket& client, const void* buf, size_t size) const {
    size_t bytes_written = 0;

    do {
        int send_res = ::send(client.socket, (const char*)buf + bytes_written, (int)(size - bytes_written), 0);

        if (send_res == SOCKET_ERROR) {
            std::cerr << "send error: " << WSAGetLastError() << std::endl;
            return false;
        };

        bytes_written += (size_t)send_res;

    } while (bytes_written < size);

    return true;
};



//Try to send data to a client -> Throws
bool tcp_server::send_t(const tcp_socket& client, const void* buf, size_t size) const {
    size_t bytes_written = 0;

    do {
        int send_res = ::send(client.socket, (const char*)buf + bytes_written, (int)(size - bytes_written), 0);

        if (send_res == SOCKET_ERROR) {
            throw tcp_ex::send_error(WSAGetLastError());
        };

        bytes_written += (size_t)send_res;

    } while (bytes_written < size);

    return true;
};



bool tcp_server::disconnect(tcp_socket& client) const {

    if (client.connected == false) {
        return false;
    };

    int shutdown_res = ::shutdown(client.socket, SD_SEND);
    closesocket(client.socket);

    client.connected = false;

    if (shutdown_res == SOCKET_ERROR) {
        std::cerr << "shutdown error: " << WSAGetLastError() << std::endl;
        return false;
    };

    return true;
};
