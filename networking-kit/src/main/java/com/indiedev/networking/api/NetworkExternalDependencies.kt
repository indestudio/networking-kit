package com.indiedev.networking.api

interface TokenRefreshApi<P, R> {
    suspend fun renewAccessToken(request: P): R
    fun isRefreshTokenExpiredError(exception: Throwable?): Boolean
}

interface NetworkExternalDependencies {

    fun getBaseUrls(): GatewaysBaseUrls

    fun  getSessionManager(): SessionManager<*, *> = object : SessionManager<Any, Any> {
        override fun getAuthToken(): String = ""
        override fun getRefreshToken(): String = ""
        override fun getUsername(): String = ""
        override fun getSessionData(): Map<String, String> = emptyMap()
        override fun createRefreshRequest(): Any = throw NotImplementedError("Must be implemented by container app")
        override fun onTokenRefreshed(response: Any) {}
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

    fun getTokenRefreshApiClass(): Class<out TokenRefreshApi<*, *>>

}
