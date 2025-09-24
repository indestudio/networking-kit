package com.indiedev.networking.token

internal interface AccessTokenProvider {
    fun getAccessToken(): String
}
