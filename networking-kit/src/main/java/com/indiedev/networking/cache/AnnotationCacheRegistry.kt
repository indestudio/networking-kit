package com.indiedev.networking.cache

import android.util.Log
import com.indiedev.networking.annotations.Cache
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Registry that extracts @Cache annotations from Retrofit requests
 * Similar to AnnotationMockRegistry but for caching
 */
class AnnotationCacheRegistry {
    private val tag = "AnnotationCacheRegistry"

    /**
     * Extract cache configuration from request annotation
     */
    fun extractCacheConfig(request: Request): CacheConfig? {
        return try {
            // Get the invocation from request tag (set by Retrofit)
            val invocation = request.tag(retrofit2.Invocation::class.java)
            invocation?.let {
                val method = it.method()
                val cacheAnnotation = method.getAnnotation(Cache::class.java)

                if (cacheAnnotation != null) {
                    val config = CacheConfig(
                        duration = cacheAnnotation.duration,
                        timeUnit = cacheAnnotation.timeUnit
                    )
                    Log.d(tag, "Found cache config for ${request.method}:${request.url} -> ${config.duration} ${config.timeUnit}")
                    config
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Could not extract cache config from request: ${e.message}")
            null
        }
    }

    /**
     * Cache configuration extracted from annotation
     */
    data class CacheConfig(
        val duration: Long,
        val timeUnit: TimeUnit
    ) {
        /**
         * Calculate expiration time in milliseconds
         */
        fun getExpirationTime(): Long {
            val durationMillis = timeUnit.toMillis(duration)
            return System.currentTimeMillis() + durationMillis
        }
    }
}
