package com.indiedev.networking.event

import com.indiedev.networking.contracts.EventLogger
import com.indiedev.networking.event.EventsProperties.API_URL
import com.indiedev.networking.event.EventsProperties.BACKEND_CODE
import com.indiedev.networking.event.EventsProperties.ERROR_MESSAGE
import com.indiedev.networking.event.EventsProperties.EXCEPTION_NAME
import com.indiedev.networking.event.EventsProperties.HTTP_CODE
import com.indiedev.networking.http.CustomHttpException
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class EventHelperImpTest {

    private lateinit var eventLogger: EventLogger
    private lateinit var eventsHelper: EventsHelper

    @Before
    fun setup() {
        eventLogger = mockk(relaxed = true)
        eventsHelper = EventHelperImp(eventLogger)
    }

    @Test
    fun `logEvent should delegate to EventLogger`() {
        // Given
        val eventName = "test_event"
        val properties = hashMapOf<String, Any>("key1" to "value1", "key2" to 123)

        // When
        eventsHelper.logEvent(eventName, properties)

        // Then
        verify { eventLogger.logEvent(eventName, properties) }
    }

    @Test
    fun `logEvent with default empty properties should work`() {
        // Given
        val eventName = "simple_event"

        // When
        eventsHelper.logEvent(eventName)

        // Then
        val propertiesSlot = slot<HashMap<String, Any>>()
        verify { eventLogger.logEvent(eventName, capture(propertiesSlot)) }
        assertEquals(0, propertiesSlot.captured.size)
    }

    @Test
    fun `logApiExceptionEvent should log exception with correct properties`() {
        // Given
        val eventName = "api_exception"
        val exception = RuntimeException("Test error message")
        val apiUrl = "https://api.example.com/test"

        // When
        eventsHelper.logApiExceptionEvent(eventName, exception, apiUrl)

        // Then
        val propertiesSlot = slot<HashMap<String, Any>>()
        verify { eventLogger.logEvent(eventName, capture(propertiesSlot)) }

        val capturedProps = propertiesSlot.captured
        assertEquals("RuntimeException", capturedProps[EXCEPTION_NAME])
        assertEquals("Test error message", capturedProps[ERROR_MESSAGE])
        assertEquals(apiUrl, capturedProps[API_URL])
    }

    @Test
    fun `logApiExceptionEvent should handle null exception message`() {
        // Given
        val eventName = "api_exception"
        val exception = RuntimeException()
        val apiUrl = "https://api.example.com/test"

        // When
        eventsHelper.logApiExceptionEvent(eventName, exception, apiUrl)

        // Then
        val propertiesSlot = slot<HashMap<String, Any>>()
        verify { eventLogger.logEvent(eventName, capture(propertiesSlot)) }

        val capturedProps = propertiesSlot.captured
        assertEquals("RuntimeException", capturedProps[EXCEPTION_NAME])
        assertEquals("null", capturedProps[ERROR_MESSAGE])
        assertEquals(apiUrl, capturedProps[API_URL])
    }

    @Test
    fun `logApiHttpExceptionEvent should log HTTP exception with correct properties`() {
        // Given
        val eventName = "http_error"
        val errorResponse = Response.error<Any>(
            404,
            """{"message":"Not Found","code":"NOT_FOUND"}""".toResponseBody("application/json".toMediaType())
        )
        val httpException = CustomHttpException(errorResponse)
        val apiUrl = "https://api.example.com/users/123"

        // When
        eventsHelper.logApiHttpExceptionEvent(eventName, httpException, apiUrl)

        // Then
        val propertiesSlot = slot<HashMap<String, Any>>()
        verify { eventLogger.logEvent(eventName, capture(propertiesSlot)) }

        val capturedProps = propertiesSlot.captured
        assertEquals("CustomHttpException", capturedProps[EXCEPTION_NAME])
        assertEquals(404, capturedProps[HTTP_CODE])
        assertEquals("Not Found", capturedProps[ERROR_MESSAGE])
        assertEquals(apiUrl, capturedProps[API_URL])
    }

    @Test
    fun `getEventProperties should create properties map with all fields`() {
        // Given
        val httpCode = 500
        val errorCode = 1001
        val message = "Internal server error"

        // When
        val result = eventsHelper.getEventProperties(httpCode, errorCode, message)

        // Then
        assertEquals(3, result.size)
        assertEquals(httpCode, result[HTTP_CODE])
        assertEquals(errorCode, result[BACKEND_CODE])
        assertEquals(message, result[ERROR_MESSAGE])
    }

    @Test
    fun `getEventProperties with default empty message should work`() {
        // Given
        val httpCode = 400
        val errorCode = 2001

        // When
        val result = eventsHelper.getEventProperties(httpCode, errorCode)

        // Then
        assertEquals(3, result.size)
        assertEquals(httpCode, result[HTTP_CODE])
        assertEquals(errorCode, result[BACKEND_CODE])
        assertEquals("", result[ERROR_MESSAGE])
    }

    @Test
    fun `getEventProperties should handle zero error code`() {
        // Given
        val httpCode = 200
        val errorCode = 0
        val message = "Success"

        // When
        val result = eventsHelper.getEventProperties(httpCode, errorCode, message)

        // Then
        assertEquals(0, result[BACKEND_CODE])
        assertEquals(200, result[HTTP_CODE])
        assertEquals("Success", result[ERROR_MESSAGE])
    }

    @Test
    fun `logApiExceptionEvent with IOException should log network error`() {
        // Given
        val eventName = "network_error"
        val exception = java.io.IOException("Connection timeout")
        val apiUrl = "https://api.example.com/data"

        // When
        eventsHelper.logApiExceptionEvent(eventName, exception, apiUrl)

        // Then
        val propertiesSlot = slot<HashMap<String, Any>>()
        verify { eventLogger.logEvent(eventName, capture(propertiesSlot)) }

        val capturedProps = propertiesSlot.captured
        assertEquals("IOException", capturedProps[EXCEPTION_NAME])
        assertEquals("Connection timeout", capturedProps[ERROR_MESSAGE])
        assertEquals(apiUrl, capturedProps[API_URL])
    }
}
