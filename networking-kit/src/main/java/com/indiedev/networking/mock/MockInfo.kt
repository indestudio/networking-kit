package com.indiedev.networking.mock

/**
 * Data class containing mock response information
 */
data class MockInfo(
    val resourceName: String,
    val statusCode: Int = 200,
    val contentType: String = "application/json",
    val headers: Map<String, String> = emptyMap(),
    val delay: Long = 0L
)
