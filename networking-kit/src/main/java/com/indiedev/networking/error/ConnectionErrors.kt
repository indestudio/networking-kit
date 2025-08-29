package com.indiedev.networking.error

import java.io.IOException

internal class NoConnectivityException : IOException() {
    override val message: String
        get() = "No network available, please check your WiFi or Data connection"
}

internal class NoInternetException : IOException() {
    override val message: String
        get() = "No internet available, please check your connected WIFi or Data"
}
