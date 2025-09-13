package com.indiedev.networking.authenticator

import com.indiedev.networking.api.ErrorCodeProvider
import com.indiedev.networking.api.SessionManager
import com.indiedev.networking.common.EMPTY_STRING
import com.indiedev.networking.common.PREFIX_AUTH_TOKEN
import com.indiedev.networking.event.EventsHelper
import com.indiedev.networking.event.EventsNames
import com.indiedev.networking.token.TokenRefreshProvider
import com.indiedev.networking.token.TokenRefreshResult
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

internal class AccessTokenAuthenticator (
    private val tokenRefreshProvider: TokenRefreshProvider,
    private val sessionManager: SessionManager,
    private val eventsHelper: EventsHelper,
    private val errorCodeProvider: ErrorCodeProvider,
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
                val tokenResponse: Result<TokenRefreshResult> = safeApiCall {
                    tokenRefreshProvider.refreshToken(
                        sessionManager.getAuthToken(),
                        sessionManager.getRefreshToken(),
                        sessionManager.getSessionData()
                    )
                }
            ) {
                is Result.Success -> {
                    return handleSuccess(tokenResponse, response)
                }

                else -> {
                    val error = tokenResponse as Result.Error

                    if (shouldAbortDueToHttpException(error.exception)) {
                        if (isRefreshTokenExpiredError(error.exception)) {
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

    @Suppress("MagicNumber")
    private fun isRefreshTokenExpiredError(exception: Throwable?): Boolean {
        val httpException = exception as HttpException

        val response = httpException.response() ?: return false
        val responseCode = response.code()
        val errorCode = getErrorCode(response.errorBody())

        val isUserSessionNotFound = responseCode == errorCodeProvider.getUserSessionNotFoundHttpStatusCode() && errorCode == errorCodeProvider.getUserSessionNotFoundErrorCode()
        val isRefreshTokenExpired = responseCode == errorCodeProvider.getRefreshTokenExpiredHttpStatusCode() && errorCode == errorCodeProvider.getRefreshTokenExpiredErrorCode()

        val shouldAbort = isUserSessionNotFound || isRefreshTokenExpired

        return shouldAbort.also {
            if (it) {
                eventsHelper.logEvent(
                    EventsNames.EVENT_REFRESH_TOKEN_NOT_VALID,
                    eventsHelper.getEventProperties(responseCode, errorCode),
                )
            }
        }
    }

    private fun logErrorEvent(throwable: Throwable?) {
        val msg = "${throwable?.javaClass?.simpleName}  ${throwable?.message}"

        eventsHelper.logEvent(
            EventsNames.EVENT_REFRESH_TOKEN_API_IO_FAILURE,
            eventsHelper.getEventProperties(0, 0, msg),
        )
    }

    private fun getErrorCode(errorBody: ResponseBody?): Int {
        return try {
            val rawBody = errorBody?.string() ?: return 0
            JSONObject(rawBody).getInt("code")
        } catch (exception: Exception) {
            0
        }
    }

    private fun handleSuccess(
        tokenResponse: Result<TokenRefreshResult>,
        response: Response,
    ): Request {
        tokenResponse.data?.let {
            sessionManager.onTokenRefreshed(
                it.accessToken,
                it.expiresAt,
                it.refreshToken,
            )
        }

        return newRequestWithAccessToken(
            response.request,
            tokenResponse.data?.accessToken ?: EMPTY_STRING,
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
