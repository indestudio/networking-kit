package com.indiedev.networking.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshTokenResponse(
    @SerialName("token")
    val token: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresAt")
    val expiresAt: String = "",
)
