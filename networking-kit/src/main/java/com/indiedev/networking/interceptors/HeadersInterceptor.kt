package com.indiedev.networking.interceptors

import com.indiedev.networking.token.AuthTokenProvider
import com.indiedev.networking.utils.AppVersionDetailsProvider
import okhttp3.Interceptor
import okhttp3.Response

internal class HeadersInterceptor constructor(
    private val tokenProvider: AuthTokenProvider,
    private val versionDetailsProvider: AppVersionDetailsProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .header(AUTHORIZATION, "$BEARER ${tokenProvider.getAuthToken()}")
                .header(APP_VERSION, versionDetailsProvider.getAppVersionCode())
                .header(APP_VERSION_NAME, versionDetailsProvider.getAppVersionName() )
                .header(APP_VERSION_CODE, versionDetailsProvider.getAppVersionCode())
                .build(),
        )
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val APP_VERSION = "AppVersion"
        const val APP_VERSION_NAME = "AppVersionName"
        const val APP_VERSION_CODE = "AppVersionCode"
        const val BEARER = "Bearer"
    }
}
