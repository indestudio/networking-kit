package com.indiedev.networking.di

import android.content.Context
import com.appmattus.certificatetransparency.certificateTransparencyInterceptor
import com.appmattus.certificatetransparency.loglist.LogListDataSourceFactory
import com.indiedev.networking.api.GatewaysBaseUrls
import com.indiedev.networking.api.CertTransparencyFlagProvider
import com.indiedev.networking.api.SessionManager
import com.indiedev.networking.authenticator.AccessTokenAuthenticator
import com.indiedev.networking.interceptor.ApiFailureInterceptor
import com.indiedev.networking.interceptor.CacheInterceptor
import com.indiedev.networking.interceptors.HeadersInterceptor
import com.indiedev.networking.interceptors.MockInterceptor
import com.indiedev.networking.interceptors.NoConnectionInterceptor
import com.indiedev.networking.qualifiers.AuthGateway
import com.indiedev.networking.qualifiers.MainGateway
import com.indiedev.networking.qualifiers.SecureGateway
import com.indiedev.networking.token.AuthTokenProvider
import com.indiedev.networking.utils.AppVersionDetailsProviderImp
import com.indiedev.networking.event.EventsHelper
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.indiedev.networking.BuildConfig
import com.indiedev.networking.FlipperInterceptorFactory
import com.indiedev.networking.adapters.FallbackEnum
import com.indiedev.networking.adapters.MoshiArrayListJsonAdapter
import com.indiedev.networking.qualifiers.AuthHttpClient
import kotlinx.serialization.json.Json
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val EXTENDED_READ_TIMEOUT = 70L
    private const val EXTENDED_WRITE_TIMEOUT = 70L
    private const val CACHE_SIZE = 10 * 1024 * 1024L
    private const val CERTIFICATE_INTERCEPTOR = "certificateInterceptor"

    @Provides
    internal fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    internal fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(FallbackEnum.ADAPTER_FACTORY)
            .add(KotlinJsonAdapterFactory())
            .add(MoshiArrayListJsonAdapter.FACTORY)
            .build()
    }

    @Singleton
    @Provides
    @Suppress("LongParameterList")
    internal fun provideOkHttpClient(
        @ApplicationContext context: Context,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        noConnectionInterceptor: NoConnectionInterceptor,
        backendInterceptor: HeadersInterceptor,
        apiFailureInterceptor: ApiFailureInterceptor,
        @Named(CERTIFICATE_INTERCEPTOR) certInterceptor: Interceptor,
        authenticator: Authenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .writeTimeout(EXTENDED_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(EXTENDED_READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(noConnectionInterceptor)
            .addInterceptor(backendInterceptor)
            .addInterceptor(apiFailureInterceptor)
            .authenticator(authenticator)
            .cache(createCache(context))
            .addNetworkInterceptor(certInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor.Builder(context).build())

                    val flipperInterceptor = FlipperInterceptorFactory.createInterceptor(context)
                    flipperInterceptor?.let {
                        addNetworkInterceptor(it)
                        addNetworkInterceptor(MockInterceptor(context))
                    }

                }

                addNetworkInterceptor(CacheInterceptor())
            }

        return builder.build()
    }

    private fun createCache(context: Context): Cache {
        return Cache(context.cacheDir, CACHE_SIZE)
    }

    @Singleton
    @Provides
    internal fun provideAuthenticator(
        sessionManager: SessionManager,
        eventsHelper: EventsHelper,
        @AuthGateway retrofit: Retrofit?,
    ): Authenticator {
        return if (retrofit != null && sessionManager.getTokenRefreshConfig<Any, Any>() != null) {
            AccessTokenAuthenticator(
                sessionManager,
                eventsHelper,
                retrofit
            )
        } else {
            Authenticator.NONE
        }
    }

    @Provides
    internal fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        if (BuildConfig.DEBUG) {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        return httpLoggingInterceptor
    }

    @Singleton
    @MainGateway
    @Provides
    fun provideMainGatewayRetrofit(
        okHttpClient: OkHttpClient,
        gatewaysBaseUrls: GatewaysBaseUrls,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(gatewaysBaseUrls.getMainGatewayUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Singleton
    @SecureGateway
    @Provides
    fun provideSecureGatewayRetrofit(
        okHttpClient: OkHttpClient,
        gatewaysBaseUrls: GatewaysBaseUrls,
        moshi: Moshi
    ): Retrofit? {
        val secureUrl = gatewaysBaseUrls.getSecureGatewayUrl()
        if (secureUrl.isBlank()) {
            return null
        }
        return Retrofit.Builder()
            .baseUrl(secureUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Singleton
    @AuthGateway
    @Provides
    fun provideAuthGatewayRetrofit(
        gatewaysBaseUrls: GatewaysBaseUrls,
        @AuthHttpClient okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit? {
        val authUrl = gatewaysBaseUrls.getAuthGatewayUrl()
        if (authUrl.isBlank()) {
            return null
        }
        return Retrofit.Builder()
            .baseUrl(authUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    internal fun provideBackendInterceptor(
        sessionManager: SessionManager,
        appVersionDetailsProvider: AppVersionDetailsProviderImp,
    ): HeadersInterceptor {
        return HeadersInterceptor(
            object : AuthTokenProvider {
                override fun getAuthToken(): String {
                    return sessionManager.getAuthToken()
                }
            },
            appVersionDetailsProvider,
        )
    }

    // TokenRefreshApi is now created dynamically by AccessTokenAuthenticator
    // No longer needed as a singleton dependency
    @AuthHttpClient
    @Singleton
    @Provides
    internal fun provideIdentityHttpClient(
        @ApplicationContext context: Context,
        httpLoggingInterceptor: HttpLoggingInterceptor,
        noConnectionInterceptor: NoConnectionInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .writeTimeout(EXTENDED_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(EXTENDED_READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(noConnectionInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor.Builder(context).build())

                    val flipperInterceptor = FlipperInterceptorFactory.createInterceptor(context)
                    flipperInterceptor?.let {
                        addNetworkInterceptor(it)
                        addNetworkInterceptor(MockInterceptor(context))
                    }

                }
            }.build()
    }

    @Provides()
    @Named(CERTIFICATE_INTERCEPTOR)
    internal fun provideCertificateInterceptor(
        certTransparencyFlagProvider: CertTransparencyFlagProvider,
        gatewaysBaseUrls: GatewaysBaseUrls,
    ): Interceptor {
        return if (certTransparencyFlagProvider.isFlagEnable().not()) {
            Interceptor { chain -> chain.proceed(chain.request()) }
        } else {
            certificateTransparencyInterceptor {
                +gatewaysBaseUrls.getMainGatewayUrl()
                val secureUrl = gatewaysBaseUrls.getSecureGatewayUrl()
                if (secureUrl.isNotBlank()) {
                    +secureUrl
                }
                val authUrl = gatewaysBaseUrls.getAuthGatewayUrl()
                if (authUrl.isNotBlank()) {
                    +authUrl
                }

                setLogListService(
                    LogListDataSourceFactory
                        .createLogListService("https://www.gstatic.com/ct/log_list/v3/"),
                )
            }
        }
    }
}
