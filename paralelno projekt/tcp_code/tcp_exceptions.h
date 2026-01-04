#ifndef TCP_EXCEPTIONS_INCLUDE
#define TCP_EXCEPTIONS_INCLUDE

#include <stdexcept>
#include <sstream>
#include <format>


namespace tcp_ex {


	class socket_disconnected : public std::exception {
	public:
		const char* what() const noexcept override { return "Connection closed"; };
	};


	class recv_error : public std::exception {
		std::string msg;

	public:
		recv_error(int err_no) {
			msg = std::format("recv error: {}", err_no);
		};

		const char* what() const noexcept override { return msg.c_str(); };
	};


	class send_error : public std::exception {
		std::string msg;

	public:
		send_error(int err_no) {
			msg = std::format("send error: {}", err_no);
		};

		const char* what() const noexcept override { return msg.c_str(); };
	};

};


#endif