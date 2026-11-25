package com.truckerload.presentation.auth

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** What the unlock gate needs from a fingerprint or face prompt. Tests supply a fake. */
interface BiometricAuthenticator {
    fun isAvailable(): Boolean

    fun authenticate(callback: Callback)

    interface Callback {
        fun onSucceeded()

        fun onError(code: Int, message: CharSequence)

        fun onFailed()
    }
}

interface BiometricUnlockMessages {
    fun failed(): String

    fun cancelled(): String
}

/**
 * Decides what each prompt outcome means. It unlocks only on an explicit success;
 * cancel, error, failed match, missing prompt and unavailable hardware all stay locked.
 */
class BiometricUnlockController(
    private val authenticator: BiometricAuthenticator?,
    private val messages: BiometricUnlockMessages,
    private val onUnlocked: () -> Unit,
    private val onError: (String?) -> Unit,
) {
    fun launch() {
        if (authenticator == null || !authenticator.isAvailable()) {
            onError(messages.failed())
            return
        }
        authenticator.authenticate(
            object : BiometricAuthenticator.Callback {
                override fun onSucceeded() = onUnlocked()

                override fun onError(code: Int, message: CharSequence) {
                    val cancelled = code == BiometricPrompt.ERROR_USER_CANCELED ||
                        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    onError(if (cancelled) messages.cancelled() else message.toString())
                }

                override fun onFailed() = onError(messages.failed())
            },
        )
    }
}

internal class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity,
    private val title: String,
    private val subtitle: String,
    private val negativeText: String,
) : BiometricAuthenticator {
    private val context: Context get() = activity

    override fun isAvailable(): Boolean = canUseBiometricUnlock(context)

    override fun authenticate(callback: BiometricAuthenticator.Callback) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    callback.onSucceeded()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    callback.onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() = callback.onFailed()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .build()
        prompt.authenticate(info)
    }
}
