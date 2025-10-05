package com.indiedev.networking.interceptors

import com.indiedev.networking.contracts.ExceptionLogger
import com.indiedev.networking.event.EventsProperties
import com.indiedev.networking.http.ClientHttpException
import com.indiedev.networking.http.CustomHttpException
import com.indiedev.networking.http.HttpErrorCodes.CLIENT_END_RANGE
import com.indiedev.networking.http.HttpErrorCodes.CLIENT_START_RANGE
import com.indiedev.networking.http.HttpErrorCodes.SERVER_END_RANGE
import com.indiedev.networking.http.HttpErrorCodes.SERVER_START_RANGE
import com.indiedev.networking.http.ServerHttpException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import retrofit2.Response.error
import java.io.IOException

internal class HttpErrorInterceptor(
    private val exceptionLogger: ExceptionLogger
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val httpUrl = chain.request().url.toString()

        val response = try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            exceptionLogger.logException(e, mapOf(EventsProperties.API_URL to httpUrl))
            throw e
        }

        // Log all client and severs exception
        if (response.code in CLIENT_START_RANGE..CLIENT_END_RANGE) {
            val httpException =
                ClientHttpException(error<Any>(response.code, response.peekBody(Long.MAX_VALUE)))
            logException(httpException, httpUrl, response)

            return createErrorResponse(response, httpException)
        } else if (response.code in SERVER_START_RANGE..SERVER_END_RANGE) {
            val httpException =
                ServerHttpException(error<Any>(response.code, response.peekBody(Long.MAX_VALUE)))

            logException(httpException, httpUrl, response)

            return createErrorResponse(response, httpException)
        }

        return response
    }

    private fun logException(
        httpException: CustomHttpException,
        httpUrl: String,
        response: Response
    ) {
        exceptionLogger.logException(
            httpException,
            mapOf(
                EventsProperties.API_URL to httpUrl,
                EventsProperties.HTTP_CODE to response.code
            )
        )
    }

    private fun createErrorResponse(
        response: Response,
        httpException: CustomHttpException
    ) = Response.Builder().body(response.body).code(response.code)
        .message(httpException.message().toString())
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url(response.request.url).build()).build()
}
