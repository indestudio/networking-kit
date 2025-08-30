package com.indiedev.networking.api

interface GatewaysBaseUrls {

    fun getMainGatewayUrl(): String

    fun getSecureGatewayUrl(): String = ""

    fun getAuthGatewayUrl(): String = ""
}
