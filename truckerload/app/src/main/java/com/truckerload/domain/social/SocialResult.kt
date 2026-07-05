package com.truckerload.domain.social

sealed class SocialResult<out T> {
    data class Success<T>(val data: T) : SocialResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : SocialResult<Nothing>()
}

inline fun <T> SocialResult<T>.getOrNull(): T? = (this as? SocialResult.Success)?.data
