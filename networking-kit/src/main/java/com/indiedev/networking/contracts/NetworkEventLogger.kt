package com.indiedev.networking.contracts

interface NetworkEventLogger {
    fun logEvent(eventName: String, properties: HashMap<String, Any> = HashMap())
}
