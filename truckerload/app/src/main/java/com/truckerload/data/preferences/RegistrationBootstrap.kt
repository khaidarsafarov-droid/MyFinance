package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.domain.account.AccountConsents
import com.truckerload.domain.account.RegistrationProgress

object RegistrationBootstrap {
    fun afterCredentialsCreated(
        context: Context,
        userId: String,
        isVerified: Boolean,
        consents: AccountConsents,
    ) {
        val app = context.applicationContext
        ConsentStore(app).save(
            userId = userId,
            tosAccepted = consents.tosAccepted,
            analyticsAccepted = consents.analyticsAccepted,
            ageConfirmed = consents.ageConfirmed,
        )
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
