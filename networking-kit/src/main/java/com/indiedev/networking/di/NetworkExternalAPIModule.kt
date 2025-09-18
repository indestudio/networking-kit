package com.indiedev.networking.di

import com.indiedev.networking.api.NetworkExternalAPI
import com.indiedev.networking.qualifiers.IdentityGateway
import com.indiedev.networking.qualifiers.MainGateway
import com.indiedev.networking.qualifiers.SecureGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkExternalAPIModule {

    @Singleton
    @Provides
    fun provideExternalAPI(
        @MainGateway retrofitMain: Retrofit,
        @SecureGateway retrofitSecure: Retrofit?,
        @IdentityGateway retrofitIdentity: Retrofit?,
    ): NetworkExternalAPI {
        return object : NetworkExternalAPI {
            override fun <T> createServiceOnMainGateway(type: Class<T>): T {
                return retrofitMain.create(type)
            }

            override fun <T> createServiceOnSecureGateway(type: Class<T>): T {
                return retrofitSecure?.create(type)
                    ?: throw IllegalStateException("Secure gateway URL is not configured")
            }

            override fun <T> createServiceOnIdentityGateway(type: Class<T>): T {
                return retrofitIdentity?.create(type)
                    ?: throw IllegalStateException("Identity gateway URL is not configured")
            }
        }
    }
}
