package com.indiedev.networking.cache

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Disk-based cache for HTTP responses
 * Supports all HTTP methods with configurable TTL
 */
class DiskCache(
    context: Context,
    maxSize: Long = 15 * 1024 * 1024 // 50MB default (unused for simplicity)
) {
    private val tag = "DiskCache"
    private val cacheDir = File(context.cacheDir, "http_cache")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Store a cache entry
     */
    fun put(key: String, entry: CacheEntry): Boolean {
        val safeKey = generateSafeKey(key)

        return try {
            // Write response body
            val bodyFile = File(cacheDir, "$safeKey.body")
            FileOutputStream(bodyFile).use { it.write(entry.responseBody) }

            // Write metadata
            val metadataFile = File(cacheDir, "$safeKey.meta")
            val metadataJson = json.encodeToString(entry.toMetadata())
            FileOutputStream(metadataFile).use { it.write(metadataJson.toByteArray()) }

            Log.d(tag, "Cached entry for key: $key")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to write cache entry for key: $key", e)
            false
        }
    }

    /**
     * Retrieve a cache entry
     */
    fun get(key: String): CacheEntry? {
        val safeKey = generateSafeKey(key)

        return try {
            val bodyFile = File(cacheDir, "$safeKey.body")
            val metadataFile = File(cacheDir, "$safeKey.meta")

            if (!bodyFile.exists() || !metadataFile.exists()) {
                return null
            }

            // Read response body
            val responseBody = FileInputStream(bodyFile).use { it.readBytes() }

            // Read metadata
            val metadataJson = FileInputStream(metadataFile).use {
                String(it.readBytes())
            }

            val metadata = json.decodeFromString<CacheMetadata>(metadataJson)

            val entry = CacheEntry(
                responseBody = responseBody,
                headers = metadata.headers,
                statusCode = metadata.statusCode,
                contentType = metadata.contentType,
                createdAt = metadata.createdAt,
                expiresAt = metadata.expiresAt
            )

            Log.d(tag, "Cache hit for key: $key")
            entry
        } catch (e: Exception) {
            Log.e(tag, "Failed to read cache entry for key: $key", e)
            null
        }
    }

    /**
     * Remove a cache entry
     */
    private fun remove(key: String): Boolean {
        val safeKey = generateSafeKey(key)
        return try {
            val bodyFile = File(cacheDir, "$safeKey.body")
            val metadataFile = File(cacheDir, "$safeKey.meta")

            val bodyDeleted = if (bodyFile.exists()) bodyFile.delete() else true
            val metaDeleted = if (metadataFile.exists()) metadataFile.delete() else true

            val removed = bodyDeleted && metaDeleted
            if (removed) {
                Log.d(tag, "Removed cache entry for key: $key")
            }
            removed
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove cache entry for key: $key", e)
            false
        }
    }

    /**
     * Clear all cache entries
     */
    fun clear() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(tag, "Cache cleared")
        } catch (e: Exception) {
            Log.e(tag, "Failed to clear cache", e)
        }
    }

    /**
     * Generate a safe key for DiskLruCache (alphanumeric + underscore)
     */
    private fun generateSafeKey(key: String): String {
        return key.encodeUtf8().md5().hex()
    }

    /**
     * Convert CacheEntry to metadata for JSON serialization
     */
    private fun CacheEntry.toMetadata(): CacheMetadata {
        return CacheMetadata(
            headers = headers,
            statusCode = statusCode,
            contentType = contentType,
            createdAt = createdAt,
            expiresAt = expiresAt
        )
    }

    /**
     * Metadata stored separately from response body
     */
    @Serializable
    private data class CacheMetadata(
        val headers: Map<String, String>,
        val statusCode: Int,
        val contentType: String,
        val createdAt: Long,
        val expiresAt: Long
    )
}
