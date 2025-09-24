package com.indiedev.networking.contracts

interface SessionTokenManager {

    fun getAccessToken(): String

    fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long =0)

    fun onTokenExpires()
    
    fun getTokenRefreshConfig(): TokenRefreshConfig<*, *>? = null
}
