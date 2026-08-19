package com.truckerload.domain.auth

/**
 * Identity providers the shared client can name.
 *
 * [APPLE] is reserved for Sign in with Apple at App Store publication. Do not
 * add AuthenticationServices, Apple JWT validation, or login UI until the
 * Apple Developer Program account exists.
 */
enum class AuthProvider {
    EMAIL,
    GOOGLE,
    APPLE,
}
