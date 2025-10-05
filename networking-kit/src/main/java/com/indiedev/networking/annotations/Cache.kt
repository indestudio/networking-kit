package com.indiedev.networking.annotations

import java.util.concurrent.TimeUnit

/**
 * Annotation to define caching behavior for API endpoints
 * Supports all HTTP methods (GET, POST, PUT, PATCH, DELETE)
 *
 * Usage:
 * ```kotlin
 * @GET("users")
 * @Cache(duration = 5, timeUnit = TimeUnit.MINUTES)
 * suspend fun getUsers(): List<User>
 *
 * @POST("users")
 * @Cache(duration = 30, timeUnit = TimeUnit.SECONDS, key = "create_user")
 * suspend fun createUser(@Body user: User): User
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(
    /**
     * Cache duration value
     */
    val duration: Long,

    /**
     * Time unit for the duration
     */
    val timeUnit: TimeUnit = TimeUnit.MINUTES

)
