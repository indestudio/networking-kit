package com.indiedev.networking.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class MainGateway

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SecureGateway

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IdentityGateway
