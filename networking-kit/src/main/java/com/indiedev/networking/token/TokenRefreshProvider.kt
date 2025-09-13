package com.indiedev.networking.token

interface TokenRefreshProvider {
    suspend fun refreshToken(
        currentAccessToken: String,
        refreshToken: String,
        sessionData: Map<String, String>
    ): TokenRefreshResult
}