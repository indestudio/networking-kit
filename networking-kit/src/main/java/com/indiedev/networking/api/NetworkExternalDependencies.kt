package com.indiedev.networking.api

interface NetworkExternalDependencies {

    fun getBaseUrls(): GatewaysBaseUrls

    fun getSessionManager(): SessionManager = object : SessionManager {
        override fun getAuthToken(): String = ""
        override fun getRefreshToken(): String = ""
        override fun getUsername(): String = ""
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
}
