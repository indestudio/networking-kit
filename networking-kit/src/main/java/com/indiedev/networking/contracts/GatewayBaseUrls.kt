package com.indiedev.networking.contracts

interface GatewayBaseUrls {

    fun getMainGatewayUrl(): String

    fun getSecureGatewayUrl(): String = ""

    fun getAuthGatewayUrl(): String = ""
}
