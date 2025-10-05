package com.indiedev.networking.serialization

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.converter.moshi.MoshiConverterFactory

class SerializationProvidersTest {

    @Test
    fun `KotlinxSerializationProvider should create converter factory`() {
        // Given
        val provider = KotlinxSerializationProvider()

        // When
        val converterFactory = provider.createConverterFactory()

        // Then
        assertNotNull(converterFactory)
    }

    @Test
    fun `MoshiSerializationProvider should create MoshiConverterFactory`() {
        // Given
        val provider = MoshiSerializationProvider()

        // When
        val converterFactory = provider.createConverterFactory()

        // Then
        assertNotNull(converterFactory)
        assertTrue(converterFactory is MoshiConverterFactory)
    }

    @Test
    fun `SerializationStrategy should have KOTLINX_SERIALIZATION option`() {
        // When
        val strategy = SerializationStrategy.KOTLINX_SERIALIZATION

        // Then
        assertNotNull(strategy)
    }

    @Test
    fun `SerializationStrategy should have MOSHI option`() {
        // When
        val strategy = SerializationStrategy.MOSHI

        // Then
        assertNotNull(strategy)
    }
}
