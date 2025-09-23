package com.indiedev.networking.http

import org.json.simple.parser.JSONParser

open class CustomHttpException(private val response: retrofit2.Response<*>) : RuntimeException() {
    private val customMessage: String
    private val customErrorCode: String?
    private val errorJson: String? = response.errorBody()?.string()

    override val message: String?
        get() = "HTTP ${response.code()} $customMessage"

    init {
        val errorData = parseErrorData(errorJson)
        customMessage = errorData.first
        customErrorCode = errorData.second
    }

    open fun code(): Int {
        return response.code()
    }

    /** HTTP status message.  */
    open fun message(): String? {
        return customMessage
    }

    /** Custom error code from response body.  */
    open fun errorCode(): String? {
        return customErrorCode
    }

    private fun parseErrorData(rawJson: String?): Pair<String, String?> {
        return try {
            val obj = JSONParser().parse(rawJson) as? org.json.simple.JSONObject
                ?: return Pair(response.message(), null)

            var message = obj["message"] as? String
            val errorCode = obj["code"] as? String ?: obj["error_code"] as? String

            if (message.isNullOrBlank()) {
                val errors = obj["errors"] as? org.json.simple.JSONArray
                message = if (!errors.isNullOrEmpty()) {
                    errors[0] as? String
                } else null
            }

            Pair(message ?: response.message(), errorCode)
        } catch (ex: Exception) {
            Pair(response.message(), null)
        }
    }
}
