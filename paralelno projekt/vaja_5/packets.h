#ifndef PACKETS_INCLUDE
#define PACKETS_INCLUDE

#include <format>


constexpr auto MAX_PACKET_SIZE = 0x100000;


enum class opcode : uint8_t {
	id,
	sync,
};



class read_exception : public std::exception {
	std::string msg;

public:
	read_exception(size_t read_offset, size_t size, size_t length) {
		msg = std::format("Attempted to read at {} with size of {}, while the packet is only {} bytes long!", read_offset, size, length);
	};

	const char* what() const noexcept { return msg.c_str(); };
};



class write_exception : public std::exception {
	std::string msg;

public:
	write_exception(size_t write_offset, size_t size, size_t length) {
		msg = std::format("Out of bounds write write at {} with size of {}. Buffer size {}", write_offset, size, length);
	};

	const char* what() const noexcept { return msg.c_str(); };
};



class packet_view {

private:
	uint8_t* buffer;
	size_t length;
	size_t offset;

	bool self_allocated;

public:

	packet_view(uint8_t* _buf, size_t max_length) : buffer(_buf), length(max_length), offset(0), self_allocated(false) {};

	packet_view(size_t len) : length(len), offset(0), self_allocated(true) {
		buffer = (uint8_t*)malloc(len);
	};

	packet_view(const packet_view& other) {
		buffer = (uint8_t*)malloc(other.length);
		length = other.length;
		memcpy(buffer, other.buffer, length);
		offset = other.offset;
		self_allocated = true;
	};

	packet_view(packet_view&& other) {
		buffer = other.buffer;
		length = other.length;
		offset = other.offset;
		self_allocated = other.self_allocated;

		other.buffer = nullptr;
		other.length = 0;
		other.self_allocated = false;
	};


	~packet_view() {
		if (self_allocated == true) {
			free(buffer);
		};
	};


	static packet_view create_from_buffer(const uint8_t* buf, size_t length) {
		packet_view result(length);
		memcpy(result.data(), buf, length);

		return result;
	};


	packet_view& operator=(const packet_view& other) {
		if (this == &other) {
			return *this;
		};

		if (self_allocated == true) {
			free(buffer);
		};

		buffer = (uint8_t*)malloc(other.length);
		length = other.length;
		memcpy(buffer, other.buffer, length);
		offset = other.offset;
		self_allocated = true;
		return *this;
	};


	uint8_t* data() const { return buffer; };

	size_t size() const { return length; };

	size_t get_offset() const { return offset; };

	size_t remaining() const { return length - offset; };

	uint8_t* current_ptr() const { return buffer + offset; };


	bool read(void* out_buf, size_t size, size_t off) const {
		if ((off + size) > length) {
			return false;
		};

		memcpy(out_buf, buffer + off, size);
		return true;
	};


	bool write(const void* buf, size_t size, size_t off) {
		if ((off + size) > length) {
			return false;
		};

		memcpy(buffer + off, buf, size);
		return true;
	};


	template <typename T>
	T read() {
		T t_buf = {};

		if (read(&t_buf, sizeof(T), offset) == false) {
			throw read_exception(offset, sizeof(T), length);
		};

		offset += sizeof(T);
		return t_buf;
	};


	void write(void* buf, size_t size) {
		if (write(buf, size, offset) == false) {
			throw write_exception(offset, size, length);
		};

		offset += size;
	};


	template <typename T>
	void write(const T& data) {
		if (write(&data, sizeof(T), offset) == false) {
			throw write_exception(offset, sizeof(T), length);
		};
		
		offset += sizeof(T);
	};


	template <>
	std::string read() {
		size_t len = read<size_t>();

		if ((offset + len) > length) {
			throw read_exception(offset, len, length);
		};

		std::string res((char*)current_ptr(), len);
		offset += len;
		return res;
	};


	template <>
	void write(const std::string& data) {

		write<size_t>(data.length());

		if (write(data.data(), data.length(), offset) == false) {
			throw write_exception(offset, data.length(), length);
		};

		offset += data.length();
	};


	static size_t get_str_size(const std::string& s) {
		return sizeof(size_t) + s.length();
	};

};

#endif