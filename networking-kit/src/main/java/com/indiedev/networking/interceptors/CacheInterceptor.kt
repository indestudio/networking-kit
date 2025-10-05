package com.indiedev.networking.interceptor

import android.content.Context
import android.util.Log
import com.indiedev.networking.cache.AnnotationCacheRegistry
import com.indiedev.networking.cache.CacheEntry
import com.indiedev.networking.cache.CacheKeyGenerator
import com.indiedev.networking.cache.DiskCache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class CacheInterceptor(context: Context) : Interceptor {
    private val diskCache = DiskCache(context)
    private val cacheRegistry = AnnotationCacheRegistry()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Check for annotation-based cache config
        val cacheConfig = cacheRegistry.extractCacheConfig(request)

        return if (cacheConfig != null) {
            handleAnnotationBasedCache(chain, cacheConfig)
        } else {
            // No cache annotation - proceed with network request
            logWithDebugMode("network call -- ${request.url}")
            val response = chain.proceed(request)
            // Add network source header for non-cached requests
            response.newBuilder()
                .addHeader("X-Cache-Source", "network")
                .build()
        }
    }

    private fun handleAnnotationBasedCache(chain: Interceptor.Chain, cacheConfig: AnnotationCacheRegistry.CacheConfig): Response {
        val request = chain.request()
        val cacheKey = CacheKeyGenerator.generateKey(request)

        // Try to get from cache first
        val cachedEntry = diskCache.get(cacheKey)
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            logWithDebugMode("Cache hit for ${request.method}:${request.url}")
            return createResponseFromCache(cachedEntry, request)
        }

        // Cache miss - proceed with network request
        val response = chain.proceed(request)

        // Cache successful responses
        if (response.isSuccessful) {
            cacheResponse(response, cacheKey, cacheConfig)
            logWithDebugMode("Cached response for ${request.method}:${request.url}")
        }

        return response
    }

    private fun cacheResponse(response: Response, cacheKey: String, cacheConfig: AnnotationCacheRegistry.CacheConfig) {
        try {
            val responseBody = response.body ?: return
            // Create a copy of the response body to avoid consuming the original
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body
            val buffer = source.buffer.clone()
            val bodyBytes = buffer.readByteArray()

            val headers = response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
            val contentType = responseBody.contentType()?.toString() ?: "application/octet-stream"

            val cacheEntry = CacheEntry(
                responseBody = bodyBytes,
                headers = headers,
                statusCode = response.code,
                contentType = contentType,
                createdAt = System.currentTimeMillis(),
                expiresAt = cacheConfig.getExpirationTime()
            )

            diskCache.put(cacheKey, cacheEntry)
        } catch (e: Exception) {
            Log.e("CacheInterceptor", "Failed to cache response", e)
        }
    }

    private fun createResponseFromCache(cacheEntry: CacheEntry, originalRequest: okhttp3.Request): Response {
        val mediaType = cacheEntry.contentType.toMediaType()
        val responseBody = cacheEntry.responseBody.toResponseBody(mediaType)

        val responseBuilder = Response.Builder()
            .request(originalRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(cacheEntry.statusCode)
            .message("OK")
            .body(responseBody)

        // Add cached headers
        cacheEntry.headers.forEach { (name, value) ->
            responseBuilder.addHeader(name, value)
        }

        // Add cache source header
        responseBuilder.addHeader("X-Cache-Source", "cache")

        return responseBuilder.build()
    }

    private fun logWithDebugMode(msg: String) {
        Log.d("CallSource", msg)
    }
}
