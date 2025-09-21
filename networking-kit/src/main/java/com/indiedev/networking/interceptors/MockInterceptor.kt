package com.indiedev.networking.interceptors

import android.content.Context
import android.util.Log
import com.indiedev.networking.common.RESPONSE_CODE_SUCCESS
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.GZIPOutputStream
import androidx.core.net.toUri

class MockInterceptor(@ApplicationContext val context: Context) : Interceptor {
    private val tag = MockInterceptor::class.simpleName

    override fun intercept(chain: Interceptor.Chain): Response {
        val uri = chain.request().url.toUri().toString()

        if (!uri.contains("mock")) {
            return chain.proceed(chain.request())
        }

        val fileName = getAPIMockResponseFileName(uri)
        fileName?.let {
            val resId = context.resources.getIdentifier(it, "raw", context.packageName)
            if (resId != 0) {
                val jsonString = readJsonFromRaw(context, resId)
                return modifyResponse(chain, jsonString)
            }
        }

        return chain.proceed(chain.request())
    }

    private fun getAPIMockResponseFileName(url: String): String? {
        val uri = url.toUri().path ?: url

        // Split the URI into parts based on the '/' delimiter
        // Split the URI into parts based on the '/' delimiter
        val parts = uri.split("/")

        // Filter out parts that contain any number
        var filteredParts = parts.filter { !it.any { char -> char.isDigit() } }

        if (filteredParts.contains("mock")) {
            filteredParts = filteredParts.dropLast(1)
        }

        // Ensure there are at least two parts after filtering
        return if (filteredParts.size >= 2) {
            filteredParts.takeLast(2).joinToString("_")
        } else {
            null
        }
    }

    private fun modifyResponse(chain: Interceptor.Chain, responseString: String): Response {
        val compressedResponse = compressString(responseString)

        return chain.proceed(chain.request())
            .newBuilder()
            .code(RESPONSE_CODE_SUCCESS)
            .protocol(Protocol.HTTP_2)
            .message(responseString)
            .body(
                compressedResponse.toResponseBody("application/json".toMediaTypeOrNull()),
            )
            .addHeader("content-type", "application/json")
            .addHeader("content-encoding", "gzip")
            .build()
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
