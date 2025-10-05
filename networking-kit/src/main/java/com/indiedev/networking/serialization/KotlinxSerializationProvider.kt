package com.indiedev.networking.serialization

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Converter

class KotlinxSerializationProvider(
    private val customJson: Json? = null
) : SerializationProvider {

    override fun createConverterFactory(): Converter.Factory {
        val json = customJson ?: createDefaultJson()
        return json.asConverterFactory("application/json".toMediaType())
    }

    private fun createDefaultJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
