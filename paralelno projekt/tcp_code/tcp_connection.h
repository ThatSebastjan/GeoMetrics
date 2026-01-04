#ifndef TCP_CONNECTION_INCLUDE
#define TCP_CONNECTION_INCLUDE

#include "tcp_server.h"
#include "tcp_client.h"



//A wrapper around tcp_client or tcp_server that provides uniform methods for sending and receiving
class tcp_connection {
private:

	//Remote connection -> local server
	tcp_server* server = nullptr;
	tcp_socket socket = {};

	//Local connection -> remote server
	tcp_client* client = nullptr;


public:

	//Connection from server and socket
	tcp_connection(tcp_server* owning_server, tcp_socket s) : server(owning_server), socket(s) {};

	//Connection from client
	tcp_connection(tcp_client* c) : client(c) {};

	tcp_connection(tcp_connection&& other) {
		server = other.server;
		socket = other.socket;
		client = other.client;

		other.server = nullptr;
		other.client = nullptr;
	};

	~tcp_connection() {
		disconnect();
	};


	tcp_connection(const tcp_connection& other) = delete;
	tcp_connection& operator=(const tcp_connection& other) = delete;


	bool is_server_owned() const { return server != nullptr; };

	bool is_client_owned() const { return client != nullptr; };


	//Receiving
	size_t recieve(void* buf, size_t size, bool read_full = true) {
		return server ? server->recieve(socket, buf, size, read_full) : client->recieve(buf, size, read_full);
	};

	template <typename T>
	size_t recieve(T* buf) const { return recieve(buf, sizeof(T), true); };

	//recieve with exceptions
	size_t recieve_t(void* buf, size_t size, bool read_full = true) {
		return server ? server->recieve_t(socket, buf, size, read_full) : client->recieve_t(buf, size, read_full);
	};

	template <typename T>
	size_t recieve_t(T* buf) const { return recieve_t(buf, sizeof(T), true); };


	//Sending
	bool send(const void* buf, size_t size) const {
		return server ? server->send(socket, buf, size) : client->send(buf, size);
	};

	bool send(const std::string& msg) const { return send(msg.data(), msg.length()); };

	//send with exceptions
	bool send_t(const void* buf, size_t size) const {
		return server ? server->send_t(socket, buf, size) : client->send_t(buf, size);
	};

	bool send_t(const std::string& msg) const { return send_t(msg.data(), msg.length()); }


	bool disconnect() {
		return server ? server->disconnect(socket) : client ? client->disconnect() : false;
	};
};

#endif