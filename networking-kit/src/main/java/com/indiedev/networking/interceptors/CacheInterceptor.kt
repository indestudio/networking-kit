package com.indiedev.networking.interceptor

import android.util.Log
import com.indiedev.networking.common.CACHE_CONTROL
import com.indiedev.networking.common.CACHE_DURATION
import com.indiedev.networking.common.CACHE_UNIT
import com.indiedev.networking.common.PRAGMA
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val headers = chain.request().headers
        val duration = headers[CACHE_DURATION]
        val unit = headers[CACHE_UNIT]

        val response = chain.proceed(chain.request())

        val isSuccessful = response.isSuccessful

        return if (duration != null && unit != null && isSuccessful) {
            logWithDebugMode("network call -- Duration: $duration Unit: $unit,  ${chain.request().url}")

            val cacheControl = CacheControl.Builder()
                .maxAge(duration.toInt(), TimeUnit.valueOf(unit))
                .build()

            response
                .newBuilder()
                .header(CACHE_CONTROL, cacheControl.toString())
                .removeHeader(PRAGMA)
                .build()
        } else {
            logWithDebugMode("network call -- ${chain.request().url}")

            response
        }
    }

    private fun logWithDebugMode(msg: String) {
        Log.d("CallSource", msg)
    }
}
