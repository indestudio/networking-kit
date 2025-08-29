package com.indiedev.networking.api

interface SessionManager {

    fun getAuthToken(): String

    fun getRefreshToken(): String

    fun getUsername(): String

    fun onTokenRefreshed(token: String, expiresAt: String, refreshToken: String)

    fun onTokenExpires()

    @Deprecated(
        "No need of this, network is handling the app " +
            "version name and version code internally, will removed this method in future",
    )
    fun getAppVersion(): String = ""
}
