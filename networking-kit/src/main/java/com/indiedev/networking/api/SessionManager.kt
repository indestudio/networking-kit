package com.indiedev.networking.api

interface SessionManager<P, R> {

    fun getAuthToken(): String

    fun getRefreshToken(): String

    fun getUsername(): String
    
    fun getSessionData(): Map<String, String>

    fun createRefreshRequest(): P

    fun onTokenRefreshed(response: R)

    fun onTokenExpires()
}
