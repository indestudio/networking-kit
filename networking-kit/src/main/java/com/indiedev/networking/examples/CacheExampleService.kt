package com.indiedev.networking.examples

import com.indiedev.networking.annotations.Cache
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Example service demonstrating @Cache annotation usage
 * for different HTTP methods
 */
interface CacheExampleService {

    /**
     * GET request with 5-minute cache
     */
    @GET("users")
    @Cache(duration = 5, timeUnit = TimeUnit.MINUTES)
    suspend fun getUsers(): List<User>

    /**
     * GET request with 10-minute cache
     */
    @GET("users/{id}")
    @Cache(duration = 10, timeUnit = TimeUnit.MINUTES)
    suspend fun getUser(@Path("id") id: String): User

    /**
     * POST request with 30-second cache (useful for preventing duplicate submissions)
     */
    @POST("users")
    @Cache(duration = 30, timeUnit = TimeUnit.SECONDS)
    suspend fun createUser(@Body user: CreateUserRequest): User

    /**
     * PUT request with 1-minute cache
     */
    @PUT("users/{id}")
    @Cache(duration = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun updateUser(@Path("id") id: String, @Body user: UpdateUserRequest): User

    /**
     * PATCH request with 2-minute cache
     */
    @PATCH("users/{id}")
    @Cache(duration = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun patchUser(@Path("id") id: String, @Body updates: Map<String, Any>): User

}

data class User(
    val id: String,
    val name: String,
    val email: String
)

data class CreateUserRequest(
    val name: String,
    val email: String
)

data class UpdateUserRequest(
    val name: String,
    val email: String
)