package com.indiedev.networking.contracts

interface EventLogger {
    fun logEvent(eventName: String, properties: HashMap<String, Any> = HashMap())
}
