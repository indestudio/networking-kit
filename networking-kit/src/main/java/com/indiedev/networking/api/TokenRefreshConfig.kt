package com.indiedev.networking.api

import retrofit2.HttpException

/**
 * Configuration interface for token refresh functionality.
 * Apps implement this to provide their specific token refresh logic.
 *
 * @param REQUEST The request type for token refresh API
 * @param RESPONSE The response type for token refresh API
 */
interface TokenRefreshConfig<REQUEST, RESPONSE> {
    
    /**
     * Returns the service interface class that networking-kit will use to create the Retrofit service.
     * The returned interface class must extend TokenRefreshService and contain exactly one method
     * with proper Retrofit annotations. See [TokenRefreshService] documentation for implementation example.
     */
    fun getServiceClass(): Class<out TokenRefreshService>
    
    /**
     * Creates the refresh token request using current user credentials
     */
    fun createRefreshRequest(): REQUEST
    
    /**
     * Extracts access token and refresh token from the response
     */
    fun extractTokens(response: RESPONSE): AuthTokens
    
    /**
     * Determines if the exception indicates an expired refresh token
     * that should trigger user logout
     */
    fun isRefreshTokenExpired(exception: HttpException): Boolean
    
    /**
     * Optional: Number of retry attempts for token refresh (default: 3)
     */
    fun getRetryCount(): Int = 3
    
}

/**
 * Marker interface that all token refresh services must implement.
 * 
 * Apps should extend this interface and provide one method with Retrofit annotations
 * for their token refresh endpoint.
 * 
 * Example implementation:
 * ```
 * interface AppTokenRefreshService : TokenRefreshService {
 *     @Headers("client-key: your-client-key")
 *     @POST("v1/identity/token/renew")
 *     suspend fun renewToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
 * }
 * ```
 * 
 * Requirements:
 * - Must extend TokenRefreshService
 * - Must have exactly one method with @POST (or other HTTP method) annotation
 * - Must use @Body annotation for the request parameter
 * - Method should be suspend function for coroutine support
 * - Can include @Headers for additional headers like client keys
 */
interface TokenRefreshService

/**
 * Simple data class for extracted tokens from response
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)