package com.indiedev.networking.contracts

interface GatewaysBaseUrls {

    fun getMainGatewayUrl(): String

    fun getSecureGatewayUrl(): String = ""

    fun getAuthGatewayUrl(): String = ""
}
