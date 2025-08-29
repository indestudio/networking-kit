package com.indiedev.networking.api

interface BaseUrls {

    fun getMainGatewayUrl(): String

    fun getSecureGatewayUrl(): String

    fun getIdentityGatewayUrl(): String
}
