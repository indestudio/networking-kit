package com.indiedev.networking.utils

import retrofit2.HttpException
import java.io.IOException

fun Throwable.isServerError(): Boolean {
    val regexPattern = "HTTP\\s5[0-9]{2}\\s.*"
    val regex = Regex(regexPattern)
    return message?.contains(regex) ?: false
}

fun Throwable.isClientError(): Boolean {
    val regexPattern = "HTTP\\s4[0-9]{2}\\s.*"
    val regex = Regex(regexPattern)
    return message?.contains(regex) ?: false
}

fun Throwable.isIOError(): Boolean {
    return this is IOException
}

@Throws(RuntimeException::class)
fun Throwable.getHttpErrorCode(): Int {
    return if (this is HttpException) {
        this.code()
    } else {
        throw RuntimeException("Not Http exception")
    }
}

@Throws(RuntimeException::class)
fun Throwable.getHttpErrorMessage(): String {
    return if (this is HttpException) {
        this.message()
    } else {
        throw RuntimeException("Not Http exception")
    }
}
