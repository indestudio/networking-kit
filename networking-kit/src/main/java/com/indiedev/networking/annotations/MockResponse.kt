package com.indiedev.networking.annotations

/**
 * Annotation to define mock responses for API endpoints
 * 
 * Usage:
 * ```kotlin
 * @GET("users")
 * @MockResponse("users_list")
 * suspend fun getUsers(): List<User>
 * 
 * @POST("login")
 * @MockResponse(resourceName = "login_success", statusCode = 200, delay = 1000)
 * suspend fun login(@Body request: LoginRequest): LoginResponse
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MockResponse(
    /**
     * Name of the raw resource file (without .json extension)
     * If empty, will be auto-generated from the endpoint path
     */
    val resourceName: String = "",
    
    /**
     * HTTP status code for the mock response
     */
    val statusCode: Int = 200,
    
    /**
     * Content type of the response
     */
    val contentType: String = "application/json",
    
    /**
     * Response delay in milliseconds (useful for testing loading states)
     */
    val delay: Long = 0,
    
    /**
     * Additional response headers in "key:value,key2:value2" format
     */
    val headers: String = ""
)