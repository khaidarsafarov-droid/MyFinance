package com.truckerload.data.remote

import android.util.Log
import com.truckerload.utils.LogRedactor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

/**
 * Rewrites network failures so the bot token in `.../bot<token>/...` never
 * leaves OkHttp inside an exception message. Debug builds may also attach
 * [HttpLoggingInterceptor]; release builds do not.
 */
class TokenRedactingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            return chain.proceed(chain.request())
        } catch (error: IOException) {
            throw redacted(error)
        }
    }

    companion object {
        fun redacted(error: IOException): IOException {
            val message = LogRedactor.redact(collectMessages(error).ifBlank { error.javaClass.simpleName })
            val safe = IOException(message)
            safe.stackTrace = error.stackTrace
            return safe
        }

        private fun collectMessages(error: Throwable): String {
            val parts = ArrayList<String>(4)
            var current: Throwable? = error
            var guard = 0
            while (current != null && guard++ < 8) {
                current.message?.let { parts.add(it) }
                current = current.cause
            }
            return parts.joinToString(" | ")
        }
    }
}

/**
 * Installs [TokenRedactingInterceptor] on every client. HTTP wire logging is
 * added only when [enableHttpLogging] is true (debug builds).
 */
fun OkHttpClient.Builder.installSecretRedaction(
    enableHttpLogging: Boolean,
    httpLogger: ((String) -> Unit)? = null,
): OkHttpClient.Builder {
    addInterceptor(TokenRedactingInterceptor())
    if (enableHttpLogging) {
        val logging = HttpLoggingInterceptor { line ->
            val redacted = LogRedactor.redact(line)
            if (httpLogger != null) httpLogger(redacted) else Log.d(HTTP_LOG_TAG, redacted)
        }
        logging.level = HttpLoggingInterceptor.Level.BASIC
        addInterceptor(logging)
    }
    return this
}

private const val HTTP_LOG_TAG = "Http"
