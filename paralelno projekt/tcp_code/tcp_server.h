#ifndef TCP_SERVER_INCLUDE
#define TCP_SERVER_INCLUDE

#include <stdio.h>
#include <cstdint>
#include <string>
#include <sstream>
#include <iostream>

#include "winsock_include.h"





//Wrapper around SOCKET structure
struct tcp_socket {
	static size_t _id;

	SOCKET socket;
	sockaddr addr;
	size_t id;
	bool connected;

	tcp_socket() : socket(INVALID_SOCKET), addr{}, id(tcp_socket::_id++), connected(false) {};


	std::string get_address() const {
		sockaddr_in* s_addr_in = (sockaddr_in*)&addr;
		std::stringstream ss;
		char tmp[64] = { 0 };

		if (inet_ntop(addr.sa_family, &s_addr_in->sin_addr, tmp, 64) == nullptr) {
			std::cerr << "inet_ntop error: " <<  WSAGetLastError() << std::endl;
			return "Error";
		};

		ss << tmp;
		ss << ":";
		ss << ((addr.sa_family == AF_INET) ? ntohs(s_addr_in->sin_port) : 0);
		return ss.str();
	};


	bool operator==(const tcp_socket& other) {
		return id == other.id;
	};
};





class tcp_server {

private:
	uint16_t port;
	WSADATA wsa_data;
	
	SOCKET server_socket;

public:
	tcp_server(int port) : port(port), wsa_data{}, server_socket(INVALID_SOCKET) {};

	~tcp_server() {
		shutdown();
		WSACleanup();
	};

	tcp_server(const tcp_server& other) = delete;
	tcp_server& operator=(const tcp_server& other) = delete;

	bool init();
	bool wait_for_connection(tcp_socket& client) const;

	size_t recieve(tcp_socket& client, void* buf, size_t size, bool read_full = true) const;
	template <typename T>
	size_t recieve(tcp_socket& client, T* buf) const { return recieve(client, buf, sizeof(T), true); };

	//recieve with exceptions
	size_t recieve_t(tcp_socket& client, void* buf, size_t size, bool read_full = true) const;
	template <typename T>
	size_t recieve_t(tcp_socket& client, T* buf) const { return recieve_t(client, buf, sizeof(T), true); };

	bool send(const tcp_socket& client, const void* buf, size_t size) const;
	bool send(const tcp_socket& client, const std::string& msg) const { return send(client, msg.data(), msg.length()); };

	//send with exceptions
	bool send_t(const tcp_socket& client, const void* buf, size_t size) const;
	bool send_t(const tcp_socket& client, const std::string& msg) const { return send_t(client, msg.data(), msg.length()); }

	bool disconnect(tcp_socket& client) const;


	bool shutdown() {
		int ret_val = 0;

		if (server_socket != INVALID_SOCKET) {
			ret_val = closesocket(server_socket);
		};

		server_socket = INVALID_SOCKET;

		return ret_val == 0;
	};


	uint16_t get_port() const { return port; };

	
};

#endif