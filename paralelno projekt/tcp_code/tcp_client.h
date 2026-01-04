#ifndef TCP_CLIENT_INCLUDE
#define TCP_CLIENT_INCLUDE

#include <stdio.h>
#include <cstdint>
#include <string>

#include "winsock_include.h"



class tcp_client {

private:
	WSADATA wsa_data;
	SOCKET client_socket;

public:
	tcp_client() : wsa_data{}, client_socket(INVALID_SOCKET) {};

	~tcp_client() {
		if (client_socket != INVALID_SOCKET) {
			disconnect();
		};

		WSACleanup();
	};


	bool connect(const std::string& ip, uint16_t port);
	bool send(const void* buf, size_t size) const;
	bool send_t(const void* buf, size_t size) const; //With exceptions
	size_t recieve(void* buf, size_t size, bool read_full = true) const;
	size_t recieve_t(void* buf, size_t size, bool read_full = true) const; //With exceptions
	bool disconnect();

	bool send(const std::string& str) const { return send(str.data(), str.length()); };
};

#endif