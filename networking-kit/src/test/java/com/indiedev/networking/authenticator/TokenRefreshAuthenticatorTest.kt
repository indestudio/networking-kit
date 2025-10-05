package com.indiedev.networking.authenticator

import com.indiedev.networking.contracts.SessionTokenManager
import com.indiedev.networking.event.EventsHelper
import com.indiedev.networking.event.EventsNames
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class TokenRefreshAuthenticatorTest {

    private lateinit var sessionTokenManager: SessionTokenManager
    private lateinit var eventsHelper: EventsHelper
    private lateinit var retrofit: Retrofit
    private lateinit var authenticator: TokenRefreshAuthenticator

    @Before
    fun setup() {
        sessionTokenManager = mockk(relaxed = true)
        eventsHelper = mockk(relaxed = true)
        retrofit = mockk(relaxed = true)

        every { sessionTokenManager.getTokenRefreshConfig() } returns null

        authenticator = TokenRefreshAuthenticator(
            sessionTokenManager,
            eventsHelper,
            lazy { retrofit }
        )
    }

    @Test
    fun `should return null when response has no Authorization header`() {
        // Given
        val request = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        val response = createResponse(request, 401)

        // When
        val result = authenticator.authenticate(null, response)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when Authorization header doesn't start with Bearer`() {
        // Given
        val request = Request.Builder()
            .url("https://api.example.com/test")
            .header("Authorization", "Basic abc123")
            .build()
        val response = createResponse(request, 401)

        // When
        val result = authenticator.authenticate(null, response)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return request immediately when token was already refreshed by another thread`() {
        // Given
        val oldToken = "old_access_token"
        val newToken = "new_access_token"
        val request = createRequestWithToken(oldToken)
        val response = createResponse(request, 401)

        // Simulate token already refreshed
        every { sessionTokenManager.getAccessToken() } returnsMany listOf(oldToken, newToken)

        // When
        val result = authenticator.authenticate(null, response)

        // Then
        assertNotNull(result)
        assertEquals("Bearer $newToken", result?.header("Authorization"))
    }

    @Test
    fun `should return null and log event when token refresh config is null`() {
        // Given
        every { sessionTokenManager.getTokenRefreshConfig() } returns null
        every { sessionTokenManager.getAccessToken() } returns "access_token"

        val request = createRequestWithToken("access_token")
        val response = createResponse(request, 401)

        // When
        val result = authenticator.authenticate(null, response)

        // Then
        assertNull(result)
        verify { eventsHelper.logEvent(EventsNames.EVENT_REFRESHING_AUTH_TOKEN_FAILED) }
    }


    private fun createRequestWithToken(token: String): Request {
        return Request.Builder()
            .url("https://api.example.com/test")
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun createResponse(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Unauthorized")
            .build()
    }
}
