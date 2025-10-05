package com.indiedev.networking.event

import com.indiedev.networking.contracts.EventLogger
import com.indiedev.networking.event.EventsProperties.API_URL
import com.indiedev.networking.event.EventsProperties.BACKEND_CODE
import com.indiedev.networking.event.EventsProperties.ERROR_MESSAGE
import com.indiedev.networking.event.EventsProperties.EXCEPTION_NAME
import com.indiedev.networking.event.EventsProperties.HTTP_CODE
import com.indiedev.networking.http.CustomHttpException

internal interface EventsHelper {

    fun logEvent(eventName: String, properties: HashMap<String, Any> = HashMap())

    fun logApiExceptionEvent(eventName: String, e: Exception, apiUrl: String)

    fun logApiHttpExceptionEvent(eventName: String, e: CustomHttpException, apiUrl: String)

    fun getEventProperties(
        httpCode: Int,
        errorCode: Int,
        message: String = "",
    ): HashMap<String, Any>
}

internal class EventHelperImp(
    private val eventLogger: EventLogger,
) : EventsHelper {

    override fun logEvent(eventName: String, properties: HashMap<String, Any>) {
        eventLogger.logEvent(eventName, properties)
    }

    override fun logApiExceptionEvent(eventName: String, e: Exception, apiUrl: String) {
        val properties = HashMap<String, Any>()

        properties[EXCEPTION_NAME] = e::class.java.simpleName
        properties[ERROR_MESSAGE] = e.message.toString()
        properties[API_URL] = apiUrl

        eventLogger.logEvent(eventName, properties)
    }

    override fun logApiHttpExceptionEvent(eventName: String, e: CustomHttpException, apiUrl: String) { // ktlint-disable max-line-length
        val properties = HashMap<String, Any>()

        properties[EXCEPTION_NAME] = e::class.java.simpleName
        properties[HTTP_CODE] = e.code()
        properties[ERROR_MESSAGE] = e.message().toString()
        properties[API_URL] = apiUrl

        eventLogger.logEvent(eventName, properties)
    }

    override fun getEventProperties(
        httpCode: Int,
        errorCode: Int,
        message: String,
    ): HashMap<String, Any> {
        val properties = HashMap<String, Any>()
        properties[HTTP_CODE] = httpCode
        properties[BACKEND_CODE] = errorCode
        properties[ERROR_MESSAGE] = message
        return properties
    }
}

object EventsNames {
    const val EVENT_REFRESH_TOKEN_NOT_VALID = "refresh_token_not_valid"
    const val HTTP_ERROR = "http_error"
    const val EVENT_REFRESHING_AUTH_TOKEN_FAILED = "refreshing_auth_token_failed"
    const val EVENT_REFRESH_TOKEN_API_IO_FAILURE = "refresh_token_api_io_failure"
}

object EventsProperties {
    const val HTTP_CODE = "httpCode"
    const val BACKEND_CODE =  "backendCode"
    const val ERROR_MESSAGE = "ErrorMessage"
    const val API_URL = "ApiUrl"
    const val EXCEPTION_NAME = "ExceptionName"
}
