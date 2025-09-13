package com.indiedev.networking.api

import com.indiedev.networking.token.TokenRefreshProvider

interface ErrorCodeProvider {
    fun getRefreshTokenExpiredErrorCode(): Int = 1001
    fun getUserSessionNotFoundErrorCode(): Int = 1002
    fun getUserSessionNotFoundHttpStatusCode(): Int = 403
    fun getRefreshTokenExpiredHttpStatusCode(): Int = 401
}

interface NetworkExternalDependencies {

    fun getBaseUrls(): GatewaysBaseUrls

    fun getSessionManager(): SessionManager = object : SessionManager {
        override fun getAuthToken(): String = ""
        override fun getRefreshToken(): String = ""
        override fun getUsername(): String = ""
        override fun getSessionData(): Map<String, String> = emptyMap()
        override fun onTokenRefreshed(token: String, expiresAt: String, refreshToken: String) {}
        override fun onTokenExpires() {}
    }

    fun getNetworkEventLogger(): NetworkEventLogger = object : NetworkEventLogger {
        override fun logEvent(eventName: String, properties: HashMap<String, Any>) {}
    }

    fun getNetworkExceptionLogger(): NetworkApiExceptionLogger = object : NetworkApiExceptionLogger {
        override fun logException(throwable: Throwable) {}
    }

    fun getCertTransparencyFlagProvider(): CertTransparencyFlagProvider {
        return object : CertTransparencyFlagProvider {
            override fun isFlagEnable(): Boolean {
                return false
            }
        }
    }

    fun getTokenRefreshProvider(): TokenRefreshProvider? = null

    fun getErrorCodeProvider(): ErrorCodeProvider = object : ErrorCodeProvider {}
}
