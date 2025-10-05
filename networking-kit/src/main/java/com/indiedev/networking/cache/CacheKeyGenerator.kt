package com.indiedev.networking.cache

import okhttp3.Request
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8

/**
 * Generates cache keys for different HTTP methods
 */
object CacheKeyGenerator {

    /**
     * Generate cache key for a request
     */
    fun generateKey(request: Request): String {
        return when (request.method.uppercase()) {
            "GET" -> generateGetKey(request)
            "POST", "PUT", "PATCH" -> generateBodyBasedKey(request)
            else -> generateFallbackKey(request)
        }
    }

    /**
     * Generate key for GET requests (URL + query params)
     */
    private fun generateGetKey(request: Request): String {
        val url = request.url.toString()
        return "GET:${url.hashSHA256()}"
    }

    /**
     * Generate key for POST/PUT/PATCH requests (URL + body hash)
     */
    private fun generateBodyBasedKey(request: Request): String {
        val url = request.url.toString()
        val bodyHash = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray().contentHashCode().toString()
        } ?: "nobody"

        return "${request.method}:${url.hashSHA256()}:$bodyHash"
    }

    /**
     * Fallback key generation for other methods
     */
    private fun generateFallbackKey(request: Request): String {
        val url = request.url.toString()
        return "${request.method}:${url.hashSHA256()}"
    }

    /**
     * Generate SHA256 hash of string
     */
    private fun String.hashSHA256(): String {
        return this.encodeUtf8().sha256().hex()
    }
}
