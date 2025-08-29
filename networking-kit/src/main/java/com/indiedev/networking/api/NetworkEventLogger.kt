package com.indiedev.networking.api

interface NetworkEventLogger {
    fun logEvent(eventName: String, properties: HashMap<String, Any> = HashMap())
}
