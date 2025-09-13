package com.indiedev.networking.token

data class TokenRefreshResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String
)