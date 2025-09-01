package com.indiedev.networking.http

import org.json.simple.parser.JSONParser

open class CustomHttpException(private val response: retrofit2.Response<*>) : RuntimeException() {
    private val customMessage: String
    private val errorJson: String? = response.errorBody()?.string()

    override val message: String?
        get() = "HTTP ${response.code()} $customMessage"

    init {
        customMessage = getErrorMessage(errorJson)
    }

    open fun code(): Int {
        return response.code()
    }

    /** HTTP status message.  */
    open fun message(): String? {
        return customMessage
    }

    private fun getErrorMessage(rawJson: String?): String {
        return try {
            val obj = JSONParser().parse(rawJson) as org.json.simple.JSONObject

            var message = obj["message"] as String?

            if (message.isNullOrBlank()) {
                message = (obj["errors"] as org.json.simple.JSONArray)[0] as String
            }

            return message
        } catch (ex: Exception) {
            response.message()
        }
    }
}
