package com.indiedev.networking.authenticator

import com.indiedev.networking.contracts.SessionTokenManager
import com.indiedev.networking.contracts.TokenRefreshConfig
import com.indiedev.networking.common.EMPTY_STRING
import com.indiedev.networking.common.PREFIX_AUTH_TOKEN
import com.indiedev.networking.event.EventsHelper
import com.indiedev.networking.event.EventsNames
import com.indiedev.networking.utils.Result
import com.indiedev.networking.utils.data
import com.indiedev.networking.utils.safeApiCall
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger

import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.*

internal class TokenRefreshAuthenticator(
    private val sessionTokenManager: SessionTokenManager,
    private val eventsHelper: EventsHelper,
    private val retrofitLazy: Lazy<retrofit2.Retrofit>,
) : Authenticator {

    private val mutex = Mutex()

    @Volatile private var shouldAbort = false
    private val waitingCount = AtomicInteger(0)
    
    private val tokenRefreshService: Any? by lazy {
        val config = sessionTokenManager.getTokenRefreshConfig()
        config?.let {
            retrofitLazy.value.create(it.getServiceClass())
        }
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!isRequestWithAccessToken(response)) return null

        val authToken = sessionTokenManager.getAccessToken()

        waitingCount.incrementAndGet()

        return runBlocking {
            mutex.withLock {
                try {
                    val newAuthToken = sessionTokenManager.getAccessToken()

                    if (authToken != newAuthToken) {
                        return@runBlocking newRequestWithAccessToken(response.request, newAuthToken)
                    }

                    if (shouldAbort) {
                        if (waitingCount.get() == 1) {
                            shouldAbort = false
                        }
                        return@runBlocking null
                    }

                    refreshTokenOrAbort(response)
                } finally {
                    waitingCount.decrementAndGet()
                }
            }
        }
    }

    private suspend fun refreshTokenOrAbort(response: Response): Request? {
        val config = sessionTokenManager.getTokenRefreshConfig()
        if (config == null) {
            eventsHelper.logEvent(EventsNames.EVENT_REFRESHING_AUTH_TOKEN_FAILED)
            shouldAbort = waitingCount.get() > 1
            return null
        }

        val service = tokenRefreshService
        if (service == null) {
            eventsHelper.logEvent(EventsNames.EVENT_REFRESHING_AUTH_TOKEN_FAILED)
            shouldAbort = waitingCount.get() > 1
            return null
        }

        repeat(config.getRetryCount()) {
            when (
                val tokenResponse: Result<*> = safeApiCall {
                    val request = config.createRefreshRequest()
                    
                    // Filter out standard Object methods to find the token refresh method
                    val refreshMethods = service.javaClass.declaredMethods.filter { method ->
                        method.name !in setOf("equals", "hashCode", "toString")
                    }

                    // Validate exactly one refresh method exists
                    if (refreshMethods.size != 1) {
                        throw IllegalArgumentException(
                            "Token refresh service must have exactly one refresh method, but found ${refreshMethods.size}"
                        )
                    }

                    val renewMethod = refreshMethods.first()

                    // Validate method has correct parameter count
                    if (renewMethod.parameterCount != 2) {
                        throw IllegalArgumentException(
                            "Token refresh method must have exactly 2 parameters, but found ${renewMethod.parameterCount}"
                        )
                    }

                    // Validate method has proper return type
                    val invalidReturnTypes = setOf(Void.TYPE, Unit::class.java, Nothing::class.java)
                    if (renewMethod.returnType in invalidReturnTypes) {
                        throw IllegalArgumentException(
                            "Token refresh method must return a value, not void, Unit, or Nothing"
                        )
                    }

                    // Invoke the refresh method
                    renewMethod.invokeSuspend(service, request)
                        ?: throw IllegalStateException("Token refresh method returned null")
                }
            ) {
                is Result.Success -> {
                    return handleSuccess(tokenResponse, response, config as TokenRefreshConfig<Any, Any>)
                }

                else -> {
                    val error = tokenResponse as Result.Error

                    if (shouldAbortDueToHttpException(error.exception)) {
                        if (config.isRefreshTokenExpired(error.exception as HttpException)) {
                            eventsHelper.logEvent(EventsNames.EVENT_REFRESH_TOKEN_NOT_VALID)
                            sessionTokenManager.onTokenExpires()
                        }
                        shouldAbort = waitingCount.get() > 1

                        return null
                    } else {
                        logErrorEvent(error.exception)
                    }
                }
            }
        }

        eventsHelper.logEvent(EventsNames.EVENT_REFRESHING_AUTH_TOKEN_FAILED)
        shouldAbort = waitingCount.get() > 1

        return null
    }

    private fun shouldAbortDueToHttpException(exception: Throwable?): Boolean {
        val httpException = exception as? HttpException ?: return false
        val response = httpException.response() ?: return false
        val responseCode = response.code()

        eventsHelper.logEvent(
            EventsNames.HTTP_ERROR,
            eventsHelper.getEventProperties(responseCode, 0, exception.message ?: EMPTY_STRING),
        )

        return true
    }


    private fun logErrorEvent(throwable: Throwable?) {
        val msg = "${throwable?.javaClass?.simpleName}  ${throwable?.message}"

        eventsHelper.logEvent(
            EventsNames.EVENT_REFRESH_TOKEN_API_IO_FAILURE,
            eventsHelper.getEventProperties(0, 0, msg),
        )
    }

    private fun handleSuccess(
        tokenResponse: Result<*>,
        response: Response,
        config: TokenRefreshConfig<Any, Any>
    ): Request {
        tokenResponse.data?.let { responseData ->
            val tokens = config.extractTokens(responseData)
            sessionTokenManager.onTokenRefreshed(tokens.accessToken, tokens.refreshToken, tokens.expiresIn)
        }

        return newRequestWithAccessToken(
            response.request,
            sessionTokenManager.getAccessToken(),
        )
    }

    private fun isRequestWithAccessToken(response: Response): Boolean {
        val header = response.request.header(AUTHORIZATION_HEADER)
        return header != null && header.startsWith(PREFIX_AUTH_TOKEN)
    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header(AUTHORIZATION_HEADER, PREFIX_AUTH_TOKEN + accessToken)
            .build()
    }

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}


suspend fun Method.invokeSuspend(obj: Any, vararg args: Any?): Any? =
    suspendCoroutineUninterceptedOrReturn { cont ->
        invoke(obj, *args, cont)
    }