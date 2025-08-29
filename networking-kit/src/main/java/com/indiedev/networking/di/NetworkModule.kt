package com.indiedev.networking.di

import android.content.Context
import com.appmattus.certificatetransparency.certificateTransparencyInterceptor
import com.appmattus.certificatetransparency.loglist.LogListDataSourceFactory
import com.indiedev.networking.api.BaseUrls
import com.indiedev.networking.api.CertTransparencyFlagProvider
import com.indiedev.networking.api.SessionManager
import com.indiedev.networking.authenticator.AccessTokenAuthenticator
import com.indiedev.networking.interceptor.ApiFailureInterceptor
import com.indiedev.networking.interceptor.CacheInterceptor
import com.indiedev.networking.interceptors.HeadersInterceptor
import com.indiedev.networking.interceptor.MockInterceptor
import com.indiedev.networking.interceptor.NoConnectionInterceptor
import com.indiedev.networking.qualifiers.IdentityGateway
import com.indiedev.networking.qualifiers.MainGateway
import com.indiedev.networking.qualifiers.SecureGateway
import com.indiedev.networking.token.AuthTokenProvider
import com.indiedev.networking.token.TokenRefreshService
import com.indiedev.networking.utils.AppVersionDetailsProviderImp
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.indiedev.networking.BuildConfig
import com.indiedev.networking.FlipperInterceptorFactory
import kotlinx.serialization.json.Json
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
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
        authenticator: AccessTokenAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .writeTimeout(EXTENDED_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(EXTENDED_READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(noConnectionInterceptor)
            .addInterceptor(backendInterceptor)
            .addInterceptor(ChuckerInterceptor.Builder(context).build())
            .addInterceptor(apiFailureInterceptor)
            .authenticator(authenticator)
            .cache(createCache(context))
            .addNetworkInterceptor(certInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
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
    fun provideGatewayRetrofit(
        okHttpClient: OkHttpClient,
        baseUrls: BaseUrls,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrls.getMainGatewayUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @SecureGateway
    @Provides
    fun provideBazaarPayRetrofit(
        okHttpClient: OkHttpClient,
        baseUrls: BaseUrls,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrls.getSecureGatewayUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Singleton
    @IdentityGateway
    @Provides
    fun provideBazaarIdentityRetrofit(
        okHttpClient: OkHttpClient,
        baseUrls: BaseUrls,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrls.getIdentityGatewayUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
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

    @Singleton
    @Provides
    internal fun provideTokenRefreshService(
        @ApplicationContext context: Context,
        baseUrls: BaseUrls,
        json: Json,
    ): TokenRefreshService {
        return Retrofit.Builder()
            .baseUrl(baseUrls.getIdentityGatewayUrl())
            .client(getRetrofitClient(context))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TokenRefreshService::class.java)
    }

    private fun getRetrofitClient(
        context: Context,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .also { client ->
                if (BuildConfig.DEBUG) {
                    val logging = HttpLoggingInterceptor()
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

                    val flipperInterceptor = FlipperInterceptorFactory.createInterceptor(context)

                    client.addInterceptor(logging).apply {
                        flipperInterceptor?.let {
                            addNetworkInterceptor(it)
                        }
                    }
                }
            }.build()
    }

    @Provides()
    @Named(CERTIFICATE_INTERCEPTOR)
    internal fun provideCertificateInterceptor(
        certTransparencyFlagProvider: CertTransparencyFlagProvider,
        baseUrls: BaseUrls,
    ): Interceptor {
        return if (certTransparencyFlagProvider.isFlagEnable().not()) {
            Interceptor { chain -> chain.proceed(chain.request()) }
        } else {
            certificateTransparencyInterceptor {
                +baseUrls.getMainGatewayUrl()
                +baseUrls.getSecureGatewayUrl()
                +baseUrls.getIdentityGatewayUrl()

                setLogListService(
                    LogListDataSourceFactory
                        .createLogListService("https://www.gstatic.com/ct/log_list/v3/"),
                )
            }
        }
    }
}
