package com.indiedev.networking.interceptors

import com.indiedev.networking.token.AccessTokenProvider
import com.indiedev.networking.utils.AppVersionProvider
import okhttp3.Interceptor
import okhttp3.Response

internal class DefaultHeadersInterceptor(
    private val tokenProvider: AccessTokenProvider,
    private val versionDetailsProvider: AppVersionProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .header(AUTHORIZATION, "$BEARER ${tokenProvider.getAccessToken()}")
                .header(APP_VERSION_NAME, versionDetailsProvider.getAppVersionName())
                .header(APP_VERSION_CODE, versionDetailsProvider.getAppVersionCode())
                .build()
        )
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val APP_VERSION_NAME = "AppVersionName"
        const val APP_VERSION_CODE = "AppVersionCode"
        const val BEARER = "Bearer"
    }
}
