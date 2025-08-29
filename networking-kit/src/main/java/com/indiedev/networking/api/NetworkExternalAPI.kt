package com.indiedev.networking.api

interface NetworkExternalAPI {

    fun <T> createServiceOnMainGateway(type: Class<T>): T

    fun <T> createServiceOnSecureGateway(type: Class<T>): T

    fun <T> createServiceOnIdentityGateway(type: Class<T>): T
}
