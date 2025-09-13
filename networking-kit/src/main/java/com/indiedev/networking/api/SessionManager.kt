package com.indiedev.networking.api

interface SessionManager {

    fun getAuthToken(): String

    fun getRefreshToken(): String

    fun getUsername(): String
    
    fun getSessionData(): Map<String, String>

    fun onTokenRefreshed(token: String, expiresAt: String, refreshToken: String)

    fun onTokenExpires()
}
