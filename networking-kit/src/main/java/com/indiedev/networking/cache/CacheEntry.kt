package com.indiedev.networking.cache

/**
 * Represents a cached response entry with metadata
 */
data class CacheEntry(
    val responseBody: ByteArray,
    val headers: Map<String, String>,
    val statusCode: Int,
    val contentType: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheEntry

        if (!responseBody.contentEquals(other.responseBody)) return false
        if (headers != other.headers) return false
        if (statusCode != other.statusCode) return false
        if (contentType != other.contentType) return false
        if (createdAt != other.createdAt) return false
        if (expiresAt != other.expiresAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = responseBody.contentHashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + statusCode
        result = 31 * result + contentType.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }
}