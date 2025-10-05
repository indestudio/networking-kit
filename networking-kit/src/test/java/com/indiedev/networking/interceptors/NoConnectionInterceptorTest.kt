package com.indiedev.networking.interceptors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.indiedev.networking.error.NoConnectivityException
import com.indiedev.networking.error.NoInternetException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class NoConnectionInterceptorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCapabilities: NetworkCapabilities
    private lateinit var interceptor: NoConnectionInterceptor
    private lateinit var chain: Interceptor.Chain
    private lateinit var request: Request

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)
        chain = mockk(relaxed = true)
        request = Request.Builder().url("https://api.example.com/test").build()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { chain.request() } returns request

        interceptor = NoConnectionInterceptor(context)
    }

    @Test
    fun `should throw NoConnectivityException when no network connection`() {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When & Then
        assertThrows(NoConnectivityException::class.java) {
            interceptor.intercept(chain)
        }
    }

    @Test
    fun `should throw NoConnectivityException when network capabilities is null`() {
        // Given
        val network = mockk<android.net.Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        // When & Then
        assertThrows(NoConnectivityException::class.java) {
            interceptor.intercept(chain)
        }
    }

    @Test
    fun `should throw NoInternetException when connected but no internet`() {
        // Given
        val network = mockk<android.net.Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        // When & Then
        assertThrows(NoInternetException::class.java) {
            interceptor.intercept(chain)
        }
    }

    @Test
    fun `should proceed with request when WiFi is connected with internet`() {
        // Given
        val network = mockk<android.net.Network>()
        val response = mockk<Response>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { chain.proceed(request) } returns response

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertNotNull(result)
        verify { chain.proceed(request) }
    }

    @Test
    fun `should proceed with request when cellular is connected with internet`() {
        // Given
        val network = mockk<android.net.Network>()
        val response = mockk<Response>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { chain.proceed(request) } returns response

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertNotNull(result)
        verify { chain.proceed(request) }
    }

    @Test
    fun `should proceed with request when VPN is connected with internet`() {
        // Given
        val network = mockk<android.net.Network>()
        val response = mockk<Response>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns true
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { chain.proceed(request) } returns response

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertNotNull(result)
        verify { chain.proceed(request) }
    }

    @Test
    fun `should proceed with request when WiFi Aware is connected with internet`() {
        // Given
        val network = mockk<android.net.Network>()
        val response = mockk<Response>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { chain.proceed(request) } returns response

        // When
        val result = interceptor.intercept(chain)

        // Then
        assertNotNull(result)
        verify { chain.proceed(request) }
    }
}
