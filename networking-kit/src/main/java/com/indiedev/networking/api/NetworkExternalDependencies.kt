package com.indiedev.networking.api

import retrofit2.Retrofit

interface TokenRefreshApi<P, R> {
    suspend fun renewAccessToken(request: P): R
    fun isRefreshTokenExpiredError(exception: Throwable?): Boolean
}

interface NetworkExternalDependencies {

    fun getBaseUrls(): GatewaysBaseUrls

    fun  getSessionManager(): SessionManager<*, *> = object : SessionManager<Unit, Unit> {
        override fun getAuthToken(): String = ""
        override fun getRefreshToken(): String = ""
        override fun getUsername(): String = ""
        override fun getSessionData(): Map<String, String> = emptyMap()
        override fun getRefreshTokenRequest(): Unit = throw NotImplementedError("Must be implemented by container app")
        override fun onTokenRefreshed(response: Unit) {}
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

    fun getTokenRefreshApi(retrofit: Retrofit): TokenRefreshApi<*, *>? = null

}
