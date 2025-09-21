package com.indiedev.networking.api

/**
 * Generic implementation of SessionManager that bridges the new simplified approach
 * with the existing SessionManager interface used by AccessTokenAuthenticator.
 * 
 * This class is no longer needed since SessionManager interface has been simplified
 * and apps implement it directly.
 */
@Deprecated("No longer needed - apps implement SessionManager directly")
internal class GenericSessionManager<REQUEST, RESPONSE>(
    private val config: TokenRefreshConfig<REQUEST, RESPONSE>,
    private val dependencies: NetworkExternalDependencies
) : SessionManager {

    override fun getAuthToken(): String = dependencies.getSessionManager().getAuthToken()


    override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {
        dependencies.getSessionManager().onTokenRefreshed(accessToken, refreshToken, expiresIn)
    }

    override fun onTokenExpires() {
        dependencies.getSessionManager().onTokenExpires()
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <REQUEST, RESPONSE> getTokenRefreshConfig(): TokenRefreshConfig<REQUEST, RESPONSE>? {
        return config as? TokenRefreshConfig<REQUEST, RESPONSE>
    }
}