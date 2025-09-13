package com.indiedev.networking.authenticator

import com.indiedev.networking.api.SessionManager
import com.indiedev.networking.api.TokenRefreshApi
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
import okhttp3.ResponseBody
import okhttp3.Route
import org.json.JSONObject
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger

internal class AccessTokenAuthenticator<P, R>(
    private val tokenRefreshApi: TokenRefreshApi<P, R>,
    private val sessionManager: SessionManager<P, R>,
    private val eventsHelper: EventsHelper,
) : Authenticator {

    private val mutex = Mutex()

    @Volatile private var shouldAbort = false
    private val waitingCount = AtomicInteger(0)

    override fun authenticate(route: Route?, response: Response): Request? {
        if (!isRequestWithAccessToken(response)) return null

        val authToken = sessionManager.getAuthToken()

        waitingCount.incrementAndGet()

        return runBlocking {
            mutex.withLock {
                try {
                    val newAuthToken = sessionManager.getAuthToken()

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
        // Need to refresh an access token

        repeat(3) {
            when (
                val tokenResponse: Result<R> = safeApiCall {
                    val request = sessionManager.createRefreshRequest()
                    tokenRefreshApi.renewAccessToken(request)
                }
            ) {
                is Result.Success -> {
                    return handleSuccess(tokenResponse, response)
                }

                else -> {
                    val error = tokenResponse as Result.Error

                    if (shouldAbortDueToHttpException(error.exception)) {
                        if (tokenRefreshApi.isRefreshTokenExpiredError(error.exception)) {
                            eventsHelper.logEvent(EventsNames.EVENT_REFRESH_TOKEN_NOT_VALID)
                            sessionManager.onTokenExpires()
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
        tokenResponse: Result<R>,
        response: Response,
    ): Request {
        tokenResponse.data?.let {
            sessionManager.onTokenRefreshed(it)
        }

        return newRequestWithAccessToken(
            response.request,
            sessionManager.getAuthToken(),
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

