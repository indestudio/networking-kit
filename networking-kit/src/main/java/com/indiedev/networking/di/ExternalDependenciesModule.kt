package com.indiedev.networking.di

import com.indiedev.networking.api.BaseUrls
import com.indiedev.networking.api.CertTransparencyFlagProvider
import com.indiedev.networking.api.NetworkApiExceptionLogger
import com.indiedev.networking.api.NetworkEventLogger
import com.indiedev.networking.api.NetworkExternalDependencies
import com.indiedev.networking.api.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExternalDependenciesModule {

    @Singleton
    @Provides
    internal fun provideBaseUrls(dependencies: NetworkExternalDependencies): BaseUrls =
        dependencies.getBaseUrls()

    @Provides
    internal fun provideSessionManager(dependencies: NetworkExternalDependencies): SessionManager =
        dependencies.getSessionManager()

    @Singleton
    @Provides
    internal fun provideNetworkEventLogger(dependencies: NetworkExternalDependencies): NetworkEventLogger =
        dependencies.getNetworkEventLogger()

    @Singleton
    @Provides
    internal fun provideNetworkExceptionLogger(dependencies: NetworkExternalDependencies): NetworkApiExceptionLogger =
        dependencies.getNetworkExceptionLogger()

    @Singleton
    @Provides
    internal fun supplyCertTransparencyFlagProvider(
        dependencies: NetworkExternalDependencies,
    ): CertTransparencyFlagProvider =
        dependencies.getCertTransparencyFlagProvider()
}
