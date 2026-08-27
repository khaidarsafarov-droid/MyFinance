package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.domain.account.RegistrationProgress

object RegistrationBootstrap {
    fun afterCredentialsCreated(
        context: Context,
        userId: String,
        isVerified: Boolean,
    ) {
        val app = context.applicationContext
        val progress = RegistrationProgressStore(app)
        progress.bindUser(userId, setupAlreadyComplete = false)
        progress.save(
            RegistrationProgress(
                credentialsComplete = true,
                verificationComplete = isVerified,
            ),
        )
    }
}
