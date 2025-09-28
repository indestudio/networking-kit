package com.indiedev.networking

import android.content.Context
import com.appmattus.certificatetransparency.certificateTransparencyInterceptor
import com.appmattus.certificatetransparency.loglist.LogListDataSourceFactory
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.indiedev.networking.adapters.FallbackEnum
import com.indiedev.networking.adapters.MoshiArrayListJsonAdapter
import com.indiedev.networking.contracts.*
import com.indiedev.networking.authenticator.TokenRefreshAuthenticator
import com.indiedev.networking.event.EventsHelper
import com.indiedev.networking.interceptors.HttpErrorInterceptor
import com.indiedev.networking.interceptor.CacheInterceptor
import com.indiedev.networking.interceptors.DefaultHeadersInterceptor
import com.indiedev.networking.interceptors.MockResponseInterceptor
import com.indiedev.networking.interceptors.NoConnectionInterceptor
import com.indiedev.networking.token.AccessTokenProvider
import com.indiedev.networking.utils.AppVersionProviderImp
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit


class NetworkingKit private constructor(
    private val mainGatewayRetrofit: Retrofit?,
    private val secureGatewayRetrofit: Retrofit?,
    private val authGatewayRetrofit: Retrofit?
) {


    fun <T> createMainService(serviceClass: Class<T>): T {
        return mainGatewayRetrofit?.create(serviceClass) ?: throw IllegalStateException("Main Gateway is not initialized.")
    }


    fun <T> createSecureService(serviceClass: Class<T>): T {
        return secureGatewayRetrofit?.create(serviceClass) ?: throw IllegalStateException("Secure Gateway is not initialized.")
    }


    fun <T> createAuthService(serviceClass: Class<T>): T {
        return authGatewayRetrofit?.create(serviceClass) ?: throw IllegalStateException("Auth Gateway is not initialized.")
    }


    class Builder(private val context: Context) {
        private var gatewayUrls: GatewayBaseUrls? = null
        private var sessionTokenManager: SessionTokenManager? = null
        private var eventLogger: EventLogger? = null
        private var exceptionLogger: ExceptionLogger? = null
        private var certTransparencyProvider: CertTransparencyConfig? = null

        private val readTimeoutSeconds = 70L
        private val writeTimeoutSeconds = 70L
        private val connectTimeoutSeconds = 30L
        private val cacheSizeBytes = 10L * 1024 * 1024 // 10MB


        fun gatewayUrls(urls: GatewayBaseUrls) = apply {
            this.gatewayUrls = urls
        }


        fun sessionManager(manager: SessionTokenManager) = apply {
            this.sessionTokenManager = manager
        }


        fun eventLogger(logger: EventLogger) = apply {
            this.eventLogger = logger
        }


        fun exceptionLogger(logger: ExceptionLogger) = apply {
            this.exceptionLogger = logger
        }


        fun certTransparencyProvider(provider: CertTransparencyConfig) = apply {
            this.certTransparencyProvider = provider
        }

        /**
         * Build the NetworkingKit instance
         * @throws IllegalStateException if required dependencies are not provided
         */
        fun build(): NetworkingKit {
            val urls = gatewayUrls
                ?: throw IllegalStateException("Gateway URLs are required. Call gatewayUrls() method.")
            
            // Optional dependencies with defaults
            val session = sessionTokenManager ?: createDefaultSessionManager()
            val events = eventLogger ?: createDefaultEventLogger()
            val exceptions = exceptionLogger ?: createDefaultExceptionLogger()
            val certProvider = certTransparencyProvider ?: createDefaultCertTransparencyProvider()

            // Create EventsHelper wrapper
            val eventsHelper = createEventsHelper(events, exceptions)

            // Create shared components
            val authTokenProvider = createAuthTokenProvider(session)
            val versionDetailsProvider = AppVersionProviderImp(context)

            // Create OkHttpClients for different gateways
            val mainOkHttpClient = createMainOkHttpClient(
                authTokenProvider,
                versionDetailsProvider,
                certProvider,
                exceptions,
                urls
            )
            val secureOkHttpClient = createSecureOkHttpClient(
                authTokenProvider,
                versionDetailsProvider,
                certProvider,
                exceptions,
                urls
            )
            val authOkHttpClient = createAuthOkHttpClient(
                authTokenProvider,
                versionDetailsProvider,
                certProvider,
                exceptions,
                urls
            )

            // Create authenticator with auth retrofit
            val authRetrofit = createRetrofit(urls.getAuthGatewayUrl(), authOkHttpClient)
            val authenticator = createAuthenticator(session, eventsHelper, authRetrofit)

            // Add authenticator to main and secure clients
            val mainClientWithAuth =
                mainOkHttpClient.newBuilder().authenticator(authenticator).build()
            val secureClientWithAuth =
                secureOkHttpClient.newBuilder().authenticator(authenticator).build()

            // Create final Retrofit instances
            val mainRetrofit = createRetrofit(urls.getMainGatewayUrl(), mainClientWithAuth)
            val secureRetrofit = createRetrofit(urls.getSecureGatewayUrl(), secureClientWithAuth)

            return NetworkingKit(mainRetrofit, secureRetrofit, authRetrofit)
        }

        /**
         * Create EventsHelper wrapper that bridges NetworkEventLogger and NetworkApiExceptionLogger
         */
        private fun createEventsHelper(
            eventLogger: EventLogger,
            exceptionLogger: ExceptionLogger
        ): EventsHelper {
            return object : EventsHelper {
                override fun logEvent(eventName: String, properties: HashMap<String, Any>) {
                    eventLogger.logEvent(eventName, properties)
                }

                override fun logApiExceptionEvent(eventName: String, e: Exception, apiUrl: String) {
                    // Log exception using exception logger
                    exceptionLogger.logException(e)

                    // Also log as event
                    val props = HashMap<String, Any>()
                    props["exception_name"] = e::class.java.simpleName
                    props["error_message"] = e.message ?: ""
                    props["api_url"] = apiUrl
                    eventLogger.logEvent(eventName, props)
                }

                override fun logApiHttpExceptionEvent(
                    eventName: String,
                    e: com.indiedev.networking.http.CustomHttpException,
                    apiUrl: String
                ) {
                    // Log exception using exception logger
                    exceptionLogger.logException(e)

                    // Also log as event
                    val props = HashMap<String, Any>()
                    props["exception_name"] = e::class.java.simpleName
                    props["http_code"] = e.code()
                    props["error_message"] = e.message() ?: ""
                    props["api_url"] = apiUrl
                    eventLogger.logEvent(eventName, props)
                }

                override fun getEventProperties(
                    httpCode: Int,
                    errorCode: Int,
                    message: String
                ): HashMap<String, Any> {
                    val properties = HashMap<String, Any>()
                    properties["http_code"] = httpCode
                    properties["error_code"] = errorCode
                    properties["error_message"] = message
                    return properties
                }
            }
        }

        /**
         * Create AuthTokenProvider wrapper
         */
        private fun createAuthTokenProvider(sessionTokenManager: SessionTokenManager): AccessTokenProvider {
            return object : AccessTokenProvider {
                override fun getAccessToken(): String {
                    return sessionTokenManager.getAccessToken()
                }
            }
        }

        /**
         * Create OkHttpClient for Main Gateway
         */
        private fun createMainOkHttpClient(
            accessTokenProvider: AccessTokenProvider,
            versionDetailsProvider: AppVersionProviderImp,
            certTransparencyProvider: CertTransparencyConfig,
            exceptionLogger: ExceptionLogger,
            gatewayUrls: GatewayBaseUrls
        ): OkHttpClient {
            return createBaseOkHttpClient(
                accessTokenProvider,
                versionDetailsProvider,
                certTransparencyProvider,
                exceptionLogger,
                gatewayUrls
            )
        }

        /**
         * Create OkHttpClient for Secure Gateway
         */
        private fun createSecureOkHttpClient(
            accessTokenProvider: AccessTokenProvider,
            versionDetailsProvider: AppVersionProviderImp,
            certTransparencyProvider: CertTransparencyConfig,
            exceptionLogger: ExceptionLogger,
            gatewayUrls: GatewayBaseUrls
        ): OkHttpClient {
            return createBaseOkHttpClient(
                accessTokenProvider,
                versionDetailsProvider,
                certTransparencyProvider,
                exceptionLogger,
                gatewayUrls
            )
        }

        /**
         * Create OkHttpClient for Auth Gateway (no authenticator to avoid circular dependency)
         */
        private fun createAuthOkHttpClient(
            accessTokenProvider: AccessTokenProvider,
            versionDetailsProvider: AppVersionProviderImp,
            certTransparencyProvider: CertTransparencyConfig,
            exceptionLogger: ExceptionLogger,
            gatewayUrls: GatewayBaseUrls
        ): OkHttpClient {
            return createBaseOkHttpClient(
                accessTokenProvider,
                versionDetailsProvider,
                certTransparencyProvider,
                exceptionLogger,
                gatewayUrls
            )
        }

        /**
         * Create base OkHttpClient with all interceptors
         */
        private fun createBaseOkHttpClient(
            accessTokenProvider: AccessTokenProvider,
            versionDetailsProvider: AppVersionProviderImp,
            certTransparencyProvider: CertTransparencyConfig,
            exceptionLogger: ExceptionLogger,
            gatewayUrls: GatewayBaseUrls
        ): OkHttpClient {
            val cacheDir = File(context.cacheDir, "networking_cache")
            val cache = Cache(cacheDir, cacheSizeBytes)

            val builder = OkHttpClient.Builder()
                // Timeout configurations  
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)

                // Cache setup
                .cache(cache)


            // Add regular interceptors in correct order
            builder.addInterceptor(NoConnectionInterceptor(context))
                .addInterceptor(DefaultHeadersInterceptor(accessTokenProvider, versionDetailsProvider))
                .addInterceptor(HttpErrorInterceptor(exceptionLogger))

            // Add certificate transparency network interceptor if enabled
            if (certTransparencyProvider.isFlagEnable()) {
                builder.addNetworkInterceptor(
                    certificateTransparencyInterceptor {
                        // Add gateway URLs for host pinning
                        +gatewayUrls.getMainGatewayUrl()
                        val secureUrl = gatewayUrls.getSecureGatewayUrl()
                        if (secureUrl.isNotBlank()) {
                            +secureUrl
                        }
                        val authUrl = gatewayUrls.getAuthGatewayUrl()
                        if (authUrl.isNotBlank()) {
                            +authUrl
                        }

                        // Set log list service
                        setLogListService(
                            LogListDataSourceFactory
                                .createLogListService("https://www.gstatic.com/ct/log_list/v3/")
                        )
                    }
                )
            }



            // Add debug interceptors if enabled
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(MockResponseInterceptor(context))

                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                builder.addNetworkInterceptor(loggingInterceptor)

                builder.addNetworkInterceptor(ChuckerInterceptor.Builder(context).build())

                // Add Flipper and Mock interceptors (debug build only)
                val flipperInterceptor = FlipperInterceptorFactory.createInterceptor(context)
                flipperInterceptor?.let {
                    builder.addNetworkInterceptor(it)
                }
            }

            // Add cache network interceptor last
            builder.addInterceptor(CacheInterceptor(context))

            return builder.build()
        }

        /**
         * Create Retrofit instance for specific gateway
         */
        private fun createRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit? {

            if(baseUrl.isBlank()) return null


//            val moshi = Moshi.Builder()
//                .add(KotlinJsonAdapterFactory())
//                .add(MoshiArrayListJsonAdapter.FACTORY)
//                .add(FallbackEnum.ADAPTER_FACTORY)
//                .build()
//
//            return Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .client(okHttpClient)
//                .addConverterFactory(MoshiConverterFactory.create(moshi))
//                .build()

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        }

        /**
         * Create authenticator for token refresh
         */
        private fun createAuthenticator(
            sessionTokenManager: SessionTokenManager,
            eventsHelper: EventsHelper,
            authRetrofit: Retrofit?
        ): Authenticator {
            return if (authRetrofit != null && sessionTokenManager.getTokenRefreshConfig() != null) {
                TokenRefreshAuthenticator(sessionTokenManager, eventsHelper, lazy { authRetrofit })
            } else {
                Authenticator.NONE
            }
        }
        
        /**
         * Create default SessionManager (no-op implementation)
         */
        private fun createDefaultSessionManager(): SessionTokenManager {
            return object : SessionTokenManager {
                override fun getAccessToken(): String = ""
                override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {}
                override fun onTokenExpires() {}
                override fun getTokenRefreshConfig(): TokenRefreshConfig<*, *>? = null
            }
        }
        
        /**
         * Create default NetworkEventLogger (no-op implementation)
         */
        private fun createDefaultEventLogger(): EventLogger {
            return object : EventLogger {
                override fun logEvent(eventName: String, properties: HashMap<String, Any>) {}
            }
        }
        
        /**
         * Create default NetworkApiExceptionLogger (no-op implementation)
         */
        private fun createDefaultExceptionLogger(): ExceptionLogger {
            return object : ExceptionLogger {
                override fun logException(throwable: Throwable) {}
                override fun logException(throwable: Throwable, customKeys: Map<String, Any>) {}
            }
        }
        
        /**
         * Create default CertTransparencyFlagProvider (disabled by default)
         */
        private fun createDefaultCertTransparencyProvider(): CertTransparencyConfig {
            return object : CertTransparencyConfig {
                override fun isFlagEnable(): Boolean = false
            }
        }
    }

    companion object {
        /**
         * Create a new builder instance
         */
        fun builder(context: Context): Builder = Builder(context)
    }
}

