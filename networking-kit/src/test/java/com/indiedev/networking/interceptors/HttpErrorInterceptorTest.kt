package com.indiedev.networking.interceptors

import com.indiedev.networking.contracts.ExceptionLogger
import com.indiedev.networking.event.EventsProperties
import com.indiedev.networking.http.ClientHttpException
import com.indiedev.networking.http.ServerHttpException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HttpErrorInterceptorTest {

    private lateinit var exceptionLogger: ExceptionLogger
    private lateinit var interceptor: HttpErrorInterceptor
    private lateinit var chain: Interceptor.Chain
    private lateinit var request: Request

    @Before
    fun setup() {
        exceptionLogger = mockk(relaxed = true)
        interceptor = HttpErrorInterceptor(exceptionLogger)
        chain = mockk(relaxed = true)
        request = Request.Builder().url("https://api.example.com/test").build()

        every { chain.request() } returns request
    }

    @Test
    fun `should log and throw IOException when network call fails`() {
        // Given
        val ioException = IOException("Network error")
        every { chain.proceed(request) } throws ioException

        // When & Then
        try {
            interceptor.intercept(chain)
        } catch (e: IOException) {
            // Verify exception was logged with API URL
            val customKeysSlot = slot<Map<String, Any>>()
            verify {
                exceptionLogger.logException(
                    ioException,
                    capture(customKeysSlot)
                )
            }
            assertTrue(customKeysSlot.captured[EventsProperties.API_URL] == "https://api.example.com/test")
        }
    }

    @Test
    fun `should handle 400 client error and log exception`() {
        // Given
        val errorResponse = createErrorResponse(400, """{"message": "Bad Request", "code": "BAD_REQUEST"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(400, result.code)
        val customKeysSlot = slot<Map<String, Any>>()
        verify {
            exceptionLogger.logException(
                any<ClientHttpException>(),
                capture(customKeysSlot)
            )
        }
        assertTrue(customKeysSlot.captured[EventsProperties.API_URL] == "https://api.example.com/test")
        assertTrue(customKeysSlot.captured[EventsProperties.HTTP_CODE] == 400)
    }

    @Test
    fun `should handle 404 client error and log exception`() {
        // Given
        val errorResponse = createErrorResponse(404, """{"message": "Not Found", "code": "NOT_FOUND"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(404, result.code)
        verify {
            exceptionLogger.logException(
                any<ClientHttpException>(),
                any()
            )
        }
    }

    @Test
    fun `should handle 401 unauthorized error`() {
        // Given
        val errorResponse = createErrorResponse(401, """{"message": "Unauthorized", "code": "UNAUTHORIZED"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(401, result.code)
        verify {
            exceptionLogger.logException(
                any<ClientHttpException>(),
                any()
            )
        }
    }

    @Test
    fun `should handle 500 server error and log exception`() {
        // Given
        val errorResponse = createErrorResponse(500, """{"message": "Internal Server Error", "code": "SERVER_ERROR"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(500, result.code)
        val customKeysSlot = slot<Map<String, Any>>()
        verify {
            exceptionLogger.logException(
                any<ServerHttpException>(),
                capture(customKeysSlot)
            )
        }
        assertTrue(customKeysSlot.captured[EventsProperties.API_URL] == "https://api.example.com/test")
        assertTrue(customKeysSlot.captured[EventsProperties.HTTP_CODE] == 500)
    }

    @Test
    fun `should handle 503 service unavailable error`() {
        // Given
        val errorResponse = createErrorResponse(503, """{"message": "Service Unavailable", "code": "UNAVAILABLE"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(503, result.code)
        verify {
            exceptionLogger.logException(
                any<ServerHttpException>(),
                any()
            )
        }
    }

    @Test
    fun `should not log exception for successful 200 response`() {
        // Given
        val successResponse = createSuccessResponse(200, """{"status": "success"}""")
        every { chain.proceed(request) } returns successResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(200, result.code)
        verify(exactly = 0) {
            exceptionLogger.logException(any<Throwable>(), any())
        }
    }

    @Test
    fun `should not log exception for successful 201 response`() {
        // Given
        val successResponse = createSuccessResponse(201, """{"id": 123}""")
        every { chain.proceed(request) } returns successResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(201, result.code)
        verify(exactly = 0) {
            exceptionLogger.logException(any<Throwable>(), any())
        }
    }

    @Test
    fun `should handle 422 unprocessable entity error`() {
        // Given
        val errorResponse = createErrorResponse(422, """{"message": "Validation Error", "code": "VALIDATION_ERROR"}""")
        every { chain.proceed(request) } returns errorResponse

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertEquals(422, result.code)
        verify {
            exceptionLogger.logException(
                any<ClientHttpException>(),
                any()
            )
        }
    }

    private fun createErrorResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun createSuccessResponse(code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
