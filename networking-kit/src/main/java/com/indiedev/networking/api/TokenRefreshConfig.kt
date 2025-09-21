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
     * Returns the service class that networking-kit will use to create the Retrofit service
     * The returned class must extend TokenRefreshService
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
 * Marker interface that all token refresh services must implement
 * This is only used for type checking, not for Retrofit service creation
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