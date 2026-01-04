#ifndef BLOCK_INCLUDE
#define BLOCK_INCLUDE

#include <string>
#include <chrono>
#include <format>
#include <iostream>
#include "utils.h"

struct block {
	size_t index;
	std::string data;
	std::chrono::system_clock::time_point timestamp;
	sha256_hash hash;
	sha256_hash previous_hash;
	int difficulty;
	size_t nonce;

	block(
		size_t _index,
		const std::string& _data,
		std::chrono::system_clock::time_point ts,
		const sha256_hash& _hash,
		const sha256_hash& _prev_hash,
		int _difficulty,
		size_t _nonce
	) : index(_index), data(_data), timestamp(ts), hash(_hash), previous_hash(_prev_hash), difficulty(_difficulty), nonce(_nonce) {}


	// Calculates the SHA256 hash of a block.
	static sha256_hash calculate_hash(const block& b) {
		return hash_block_data(b.index, b.timestamp, b.data, b.previous_hash, b.difficulty, b.nonce);
	}


	// Computes the SHA256 hash from block data components.
	static sha256_hash hash_block_data(
		size_t index,
		std::chrono::system_clock::time_point ts,
		const std::string& data,
		const sha256_hash& prev_hash,
		int difficulty,
		size_t nonce
	) {

		thread_local uint8_t* hash_buf = nullptr;
		thread_local size_t buf_size = 0;

		size_t data_size = sizeof(size_t) + data.length() + sizeof(int64_t) + sizeof(sha256_hash) + sizeof(int) + sizeof(size_t);

		if (buf_size < data_size) {
			free(hash_buf);

			hash_buf = (uint8_t*)malloc(data_size * 2);

			if (hash_buf == nullptr) {
				buf_size = 0;
				std::cout << "Out of memory!" << std::endl;
				return {};
			}

			buf_size = data_size * 2;
		}


		size_t off = 0;

		*(size_t*)(hash_buf + off) = index;
		off += sizeof(size_t);

		memcpy(hash_buf + off, data.data(), data.length());
		off += data.length();

		*(int64_t*)(hash_buf + off) = std::chrono::duration_cast<std::chrono::milliseconds>(ts.time_since_epoch()).count();
		off += sizeof(int64_t);

		memcpy(hash_buf + off, prev_hash.bytes, sizeof(sha256_hash));
		off += sizeof(sha256_hash);

		*(int*)(hash_buf + off) = difficulty;
		off += sizeof(int);

		*(size_t*)(hash_buf + off) = nonce;
		off += sizeof(size_t);


		return sha256(hash_buf, data_size);
	}
};


#endif