package com.indiedev.networking.serialization

import com.indiedev.networking.adapters.FallbackEnum
import com.indiedev.networking.adapters.MoshiArrayListJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Converter
import retrofit2.converter.moshi.MoshiConverterFactory

class MoshiSerializationProvider(
    private val customMoshi: Moshi? = null
) : SerializationProvider {

    override fun createConverterFactory(): Converter.Factory {
        val moshi = customMoshi ?: createDefaultMoshi()
        return MoshiConverterFactory.create(moshi)
    }

    private fun createDefaultMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(MoshiArrayListJsonAdapter.FACTORY)
            .add(FallbackEnum.ADAPTER_FACTORY)
            .build()
    }
}
