package com.indiedev.networking.contracts

interface SessionManager {

    fun getAuthToken(): String

    fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long =0)

    fun onTokenExpires()
    
    // New method for token refresh configuration
    fun  getTokenRefreshConfig(): TokenRefreshConfig<*, *>? = null
}
