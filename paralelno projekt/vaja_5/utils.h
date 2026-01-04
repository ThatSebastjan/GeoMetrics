#ifndef UTILS_INCLUDE
#define UTILS_INCLUDE

#include <cstdint>
#include <string>
#include <iomanip>

#pragma warning(disable : 4996)
#include <openssl/rand.h>
#include <openssl/sha.h>
#include <sstream>
#include <cstring>

struct sha256_hash {
	uint8_t bytes[SHA256_DIGEST_LENGTH];

	sha256_hash() : bytes{} {};

	bool operator==(const sha256_hash& other) const {
		return memcmp(bytes, other.bytes, SHA256_DIGEST_LENGTH) == 0;
	};

	std::string to_string() const {
		std::stringstream ss;
		for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
			ss << std::hex << std::setw(2) << std::setfill('0') << (int)bytes[i];
		};

		return ss.str();
	};

	bool is_of_difficulty(size_t n) const {
		static const uint8_t null_hash[SHA256_DIGEST_LENGTH] = {};

		if (n > SHA256_DIGEST_LENGTH * 2) {
			return false;
		};

		size_t offset = 0;

		while (offset < n) {
			size_t chunk_size = std::min<size_t>(n - offset, sizeof(size_t) * 2);
			uint64_t mask = (~0ui64) << (4 * (sizeof(size_t) * 2 - chunk_size));

			uint64_t hash_val = _byteswap_uint64(*(uint64_t*)(bytes + (offset >> 1)));

			if ((hash_val & mask) != 0) {
				return false;
			};

			offset += chunk_size;
		};

		return true;
	};
};

std::string generate_uuid_v4();
sha256_hash sha256(const void* data, size_t size);
sha256_hash sha256_str(const std::string& str);

#endif