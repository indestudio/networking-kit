package com.indiedev.networking.http

class ClientHttpException(response: retrofit2.Response<*>) : CustomHttpException(
    response,
)
