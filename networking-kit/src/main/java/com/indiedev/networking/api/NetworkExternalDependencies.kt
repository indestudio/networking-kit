package com.indiedev.networking.api

interface NetworkExternalDependencies {

    fun getBaseUrls(): GatewaysBaseUrls

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

    // SessionManager contains all token-related logic including configuration
    fun getSessionManager(): SessionManager = object : SessionManager {
        override fun getAuthToken(): String {
           return ""
        }

        override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {
        }

        override fun onTokenExpires() {
        }

        override fun <REQUEST, RESPONSE> getTokenRefreshConfig(): TokenRefreshConfig<REQUEST, RESPONSE>? {
            return null
        }

    }
}
