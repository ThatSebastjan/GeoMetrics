#include "tcp_client.h"
#include "tcp_exceptions.h"
#include <iostream>



bool tcp_client::connect(const std::string& ip, uint16_t port) {

	if (client_socket != INVALID_SOCKET) {
		disconnect();
	};

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

	addrinfo* addrinfo_result = nullptr;


	int addrinfo_res = getaddrinfo(ip.c_str(), port_str, &hints, &addrinfo_result);

	if (addrinfo_res != 0) {
		std::cerr << "getaddrinfo error: " << addrinfo_res << std::endl;
		return false;
	};


	client_socket = socket(addrinfo_result->ai_family, addrinfo_result->ai_socktype, addrinfo_result->ai_protocol);

	if (client_socket == INVALID_SOCKET) {
		std::cerr << "socket error: " << WSAGetLastError() << std::endl;
		freeaddrinfo(addrinfo_result);
		return false;
	};


	//Disable sending delay (disable Nagle's algo)
	int opt_val = 1;
	int opt_res = setsockopt(client_socket, IPPROTO_TCP, TCP_NODELAY, (const char*)&opt_val, sizeof(int));

	if (opt_res == SOCKET_ERROR) {
		freeaddrinfo(addrinfo_result);
		return false;
	};


	//Try to connect
	bool connect_status = false;
	int connect_idx = 0;

	for (addrinfo* c_addrinfo = addrinfo_result; c_addrinfo != nullptr; c_addrinfo = c_addrinfo->ai_next) {

		if (::connect(client_socket, c_addrinfo->ai_addr, (int)c_addrinfo->ai_addrlen) == SOCKET_ERROR) {
			std::cerr << "connect " << connect_idx++ << " failed: " << WSAGetLastError() << std::endl;
			continue;
		};

		connect_status = true;
		break;
	};

	freeaddrinfo(addrinfo_result);

	if (connect_status == false) {
		return false;
	};


	return true;
};



bool tcp_client::send(const void* buf, size_t size) const {
	size_t bytes_written = 0;

	do {
		int send_res = ::send(client_socket, (const char*)buf + bytes_written, (int)(size - bytes_written), 0);

		if (send_res == SOCKET_ERROR) {
			std::cerr << "send error: " <<  WSAGetLastError() << std::endl;
			return false;
		};

		bytes_written += (size_t)send_res;

	} while (bytes_written < size);

	return true;
};



//NOTE: Throws
bool tcp_client::send_t(const void* buf, size_t size) const {
	size_t bytes_written = 0;

	do {
		int send_res = ::send(client_socket, (const char*)buf + bytes_written, (int)(size - bytes_written), 0);

		if (send_res == SOCKET_ERROR) {
			throw tcp_ex::send_error(WSAGetLastError());
		};

		bytes_written += (size_t)send_res;

	} while (bytes_written < size);

	return true;
};



size_t tcp_client::recieve(void* buf, size_t size, bool read_full) const {
	size_t bytes_read = 0;

	do {
		int recv_res = recv(client_socket, (char*)buf + bytes_read, (int)(size - bytes_read), 0);

		//socket closed
		if (recv_res == 0) {
			break;
		}
		else if (recv_res == SOCKET_ERROR) {
			std::cerr << "recv error: " <<  WSAGetLastError() << std::endl;
			break;
		};

		bytes_read += (size_t)recv_res;

	} while (read_full && (bytes_read < size));

	return bytes_read;
};



//NOTE: Throws
size_t tcp_client::recieve_t(void* buf, size_t size, bool read_full) const {
	size_t bytes_read = 0;

	do {
		int recv_res = recv(client_socket, (char*)buf + bytes_read, (int)(size - bytes_read), 0);

		//socket closed
		if (recv_res == 0) {
			throw tcp_ex::socket_disconnected();
		}
		else if (recv_res == SOCKET_ERROR) {
			throw tcp_ex::recv_error(WSAGetLastError());
		};

		bytes_read += (size_t)recv_res;

	} while (read_full && (bytes_read < size));

	return bytes_read;
};



bool tcp_client::disconnect() {
	int shutdown_res = shutdown(client_socket, SD_SEND);
	closesocket(client_socket);
	client_socket = INVALID_SOCKET;

	if (shutdown_res == SOCKET_ERROR) {
		std::cerr << "shutdown error: " << WSAGetLastError() << std::endl;
		return false;
	};

	return true;
};