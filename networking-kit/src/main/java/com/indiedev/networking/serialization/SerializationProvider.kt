package com.indiedev.networking.serialization

import retrofit2.Converter

interface SerializationProvider {
    fun createConverterFactory(): Converter.Factory
}