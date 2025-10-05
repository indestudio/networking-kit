package com.indiedev.networking.interceptors

import android.content.Context
import android.util.Log
import com.indiedev.networking.BuildConfig
import com.indiedev.networking.mock.AnnotationMockRegistry
import com.indiedev.networking.mock.MockInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.GZIPOutputStream

class MockResponseInterceptor(val context: Context) : Interceptor {
    private val tag = MockResponseInterceptor::class.simpleName
    private val annotationRegistry = AnnotationMockRegistry(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Only process mocks in debug mode
        if (!BuildConfig.DEBUG) {
            return chain.proceed(request)
        }
        
        // Check annotation-based mocks
        val mockInfo = annotationRegistry.findMockForRequest(request)
        
        return if (mockInfo != null) {
            createMockResponse(request, mockInfo)
        } else {
            chain.proceed(request)
        }
    }

    /**
     * Create a mock response based on MockInfo
     */
    private fun createMockResponse(request: Request, mockInfo: MockInfo): Response {
        return runBlocking {
            // Apply delay if configured
            if (mockInfo.delay > 0) {
                delay(mockInfo.delay)
            }
            
            // Get resource ID for the mock file
            val resourceId = context.resources.getIdentifier(
                mockInfo.resourceName, 
                "raw", 
                context.packageName
            )
            
            if (resourceId == 0) {
                Log.w(tag, "Mock resource not found: ${mockInfo.resourceName}")
                throw IOException("Mock resource not found: ${mockInfo.resourceName}")
            }
            
            // Read and compress the mock data
            val jsonString = readJsonFromRaw(context, resourceId)
//            val compressedResponse = compressString(jsonString)
            
            // Build the mock response
            val responseBuilder = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(mockInfo.statusCode)
                .message("OK")
                .body(jsonString.toResponseBody(mockInfo.contentType.toMediaTypeOrNull()))
                .addHeader("content-type", mockInfo.contentType)
//                .addHeader("content-encoding", "gzip")
            
            // Add custom headers if any
            mockInfo.headers.forEach { (name, value) ->
                responseBuilder.addHeader(name, value)
            }
            
            responseBuilder.build()
        }
    }

    private fun compressString(input: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            GZIPOutputStream(byteArrayOutputStream).use { gzip ->
                gzip.write(input.toByteArray(Charsets.UTF_8))
            }
        } catch (e: IOException) {
            Log.e(tag, e.message ?: "Exception in compressString()")
        }
        return byteArrayOutputStream.toByteArray()
    }

    private fun readJsonFromRaw(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = StringBuilder()
        var line: String?

        try {
            while (reader.readLine().also { line = it } != null) {
                content.append(line)
            }
        } finally {
            reader.close()
        }

        return content.toString()
    }
}
