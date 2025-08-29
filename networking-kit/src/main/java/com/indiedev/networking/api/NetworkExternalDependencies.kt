package com.indiedev.networking.api

interface NetworkExternalDependencies {

    fun getBaseUrls(): BaseUrls

    fun getSessionManager(): SessionManager

    fun getNetworkEventLogger(): NetworkEventLogger

    fun getNetworkExceptionLogger(): NetworkApiExceptionLogger

    fun getCertTransparencyFlagProvider(): CertTransparencyFlagProvider {
        return object : CertTransparencyFlagProvider {
            override fun isFlagEnable(): Boolean {
                return false
            }
        }
    }
}
