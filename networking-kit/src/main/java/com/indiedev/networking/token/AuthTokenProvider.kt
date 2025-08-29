package com.indiedev.networking.token

internal interface AuthTokenProvider {
    fun getAuthToken(): String
}
