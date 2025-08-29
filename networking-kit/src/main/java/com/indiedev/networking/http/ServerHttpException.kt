package com.indiedev.networking.http

class ServerHttpException(response: retrofit2.Response<*>) : CustomHttpException(
    response,
)
