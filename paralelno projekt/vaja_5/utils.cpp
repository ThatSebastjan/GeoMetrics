#include "utils.h"
#include <cstring>
#include <cstdio>
#include <sstream>

// Generates a random UUID version 4 string.
std::string generate_uuid_v4() {
	union {
		struct {
			uint32_t time_low;
			uint16_t time_mid;
			uint16_t time_hi_and_version;
			uint8_t  clk_seq_hi_res;
			uint8_t  clk_seq_low;
			uint8_t  node[6];
		};
		uint8_t __rnd[16];
	} uuid;

	int rc = RAND_bytes(uuid.__rnd, sizeof(uuid));

	uuid.clk_seq_hi_res = (uint8_t)((uuid.clk_seq_hi_res & 0x3F) | 0x80);
	uuid.time_hi_and_version = (uint16_t)((uuid.time_hi_and_version & 0x0FFF) | 0x4000);

	char buffer[39] = {};

	snprintf(buffer, 38, "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
		uuid.time_low, uuid.time_mid, uuid.time_hi_and_version,
		uuid.clk_seq_hi_res, uuid.clk_seq_low,
		uuid.node[0], uuid.node[1], uuid.node[2],
		uuid.node[3], uuid.node[4], uuid.node[5]);

	return std::string(buffer);
}

// Computes the SHA256 hash of raw data.
sha256_hash sha256(const void* data, size_t size) {
	sha256_hash hash;
	SHA256_CTX sha256;
	SHA256_Init(&sha256);
	SHA256_Update(&sha256, data, size);
	SHA256_Final(hash.bytes, &sha256);

	return hash;
}

// Computes the SHA256 hash of a string.
sha256_hash sha256_str(const std::string& str) {
	return sha256(str.c_str(), str.size());
}

