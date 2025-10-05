package com.indiedev.networking.mock

import android.content.Context
import android.util.Log
import com.indiedev.networking.annotations.MockResponse
import okhttp3.Request
import retrofit2.http.*
import java.lang.reflect.Method
import java.util.regex.Pattern

/**
 * Registry that automatically discovers and processes @MockResponse annotations * from the call stack when intercepting requests
 */
class AnnotationMockRegistry(private val context: Context) {
    private val tag = "AnnotationMockRegistry"

    /**
     * Extract mock info directly from Retrofit request without persistent caching
     */
    private fun extractMockFromRequest(request: Request): MockInfo? {
        try {
            // Get the invocation from request tag (set by Retrofit)
            val invocation = request.tag(retrofit2.Invocation::class.java)
            invocation?.let {
                val method = it.method()
                val mockAnnotation = method.getAnnotation(MockResponse::class.java)

                if (mockAnnotation != null) {
                    val httpInfo = extractHttpInfo(method)
                    if (httpInfo != null && matchesRequest(request, httpInfo)) {
                        val mockInfo = createMockInfo(mockAnnotation, httpInfo)
                        Log.d(tag, "Found mock for ${request.method}:${request.url} -> ${mockInfo.resourceName}")
                        return mockInfo
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Could not extract mock from request: ${e.message}")
        }
        return null
    }

    /**
     * Check if the HTTP info matches the current request
     */
    private fun matchesRequest(request: Request, httpInfo: HttpInfo): Boolean {
        if (!request.method.equals(httpInfo.method, ignoreCase = true)) {
            return false
        }

        val url = request.url.toString()
        return matchesPattern(request.method, url, "${httpInfo.method}:${httpInfo.path}")
    }

    /**
     * Find mock info for a given request (processes annotation on-demand)
     */
    fun findMockForRequest(request: Request): MockInfo? {
        return extractMockFromRequest(request)
    }

    private fun extractHttpInfo(method: Method): HttpInfo? {
        method.annotations.forEach { annotation ->
            when (annotation) {
                is GET -> return HttpInfo("GET", annotation.value)
                is POST -> return HttpInfo("POST", annotation.value)
                is PUT -> return HttpInfo("PUT", annotation.value)
                is DELETE -> return HttpInfo("DELETE", annotation.value)
                is PATCH -> return HttpInfo("PATCH", annotation.value)
                is HEAD -> return HttpInfo("HEAD", annotation.value)
                is OPTIONS -> return HttpInfo("OPTIONS", annotation.value)
                is HTTP -> return HttpInfo(annotation.method, annotation.path)
            }
        }
        return null
    }

    private fun createMockInfo(annotation: MockResponse, httpInfo: HttpInfo): MockInfo {
        val resourceName = if (annotation.resourceName.isNotEmpty()) {
            annotation.resourceName
        } else {
            generateResourceName(httpInfo.path)
        }

        val headers = parseHeaders(annotation.headers)

        return MockInfo(
            resourceName = resourceName,
            statusCode = annotation.statusCode,
            contentType = annotation.contentType,
            headers = headers,
            delay = annotation.delay
        )
    }

    private fun generateResourceName(path: String): String {
        // Convert path like "/users/{id}/posts" to "users_posts"
        return path
            .removePrefix("/")
            .split("/")
            .filter { it.isNotEmpty() && !it.startsWith("{") } // Remove path parameters
            .joinToString("_")
            .lowercase()
    }

    private fun parseHeaders(headerString: String): Map<String, String> {
        if (headerString.isBlank()) return emptyMap()

        return headerString.split(",")
            .mapNotNull { header ->
                val parts = header.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }
            .toMap()
    }

    private fun matchesPattern(method: String, url: String, pattern: String): Boolean {
        val parts = pattern.split(":", limit = 2)
        if (parts.size != 2) return false

        val patternMethod = parts[0]
        val patternPath = parts[1]

        if (!method.equals(patternMethod, ignoreCase = true)) {
            return false
        }

        // Convert Retrofit path pattern to regex
        // e.g., "/users/{id}/posts" -> "/users/\\d+/posts"
        val regexPattern = patternPath
            .replace("{id}", "\\d+")
            .replace("{userId}", "\\d+")
            .replace("{postId}", "\\d+")
            .replace(Regex("\\{\\w+\\}"), "[^/]+") // Generic path parameter matching

        return try {
            val regex = Pattern.compile(".*$regexPattern.*")
            regex.matcher(url).find()
        } catch (e: Exception) {
            Log.w(tag, "Invalid regex pattern: $regexPattern", e)
            url.contains(patternPath.replace(Regex("\\{\\w+\\}"), ""))
        }
    }

    private data class HttpInfo(
        val method: String,
        val path: String
    )
}
