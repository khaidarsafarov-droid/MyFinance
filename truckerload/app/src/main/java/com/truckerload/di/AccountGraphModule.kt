package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.ConsentStore
import com.truckerload.data.preferences.RegistrationProgressStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.privacy.AesGcmSensitiveFieldCipher
import com.truckerload.data.privacy.SensitiveFieldCipher
import com.truckerload.data.repository.account.AccountDeletionService
import com.truckerload.data.repository.account.AccountIdentityRepository
import com.truckerload.data.repository.account.DriverProfessionalRepository
import com.truckerload.data.repository.account.RegistrationService

object AccountGraphModule {
    @UserScope
    data class Bundle(
        val identity: AccountIdentityRepository,
        val professional: DriverProfessionalRepository,
        val registration: RegistrationService,
        val deletion: AccountDeletionService,
        val progressStore: RegistrationProgressStore,
        val consentStore: ConsentStore,
        val cipher: SensitiveFieldCipher,
    )

    fun create(
        context: Context,
        userId: String,
        db: AppDatabase,
        userProfileStore: UserProfileStore,
    ): Bundle {
        val app = context.applicationContext
        val cipher = AesGcmSensitiveFieldCipher.forUser(app, userId)
        val identity = AccountIdentityRepository(db.userAccountDao())
        val professional = DriverProfessionalRepository(db.driverProfessionalDao(), cipher)
        val progressStore = RegistrationProgressStore(app).also {
            val setupDone = runCatching { userProfileStore.setupComplete.value }.getOrDefault(false)
            it.bindUser(userId, setupDone)
        }
        val consentStore = ConsentStore(app)
        val registration = RegistrationService(
            userId = userId,
            identity = identity,
            professional = professional,
            progressStore = progressStore,
            userProfileStore = userProfileStore,
        )
        val deletion = AccountDeletionService(
            context = app,
            userId = userId,
            database = db,
            identity = identity,
            professional = professional,
            cipher = cipher,
            consentStore = consentStore,
            progressStore = progressStore,
            userProfileStore = userProfileStore,
            authStore = AuthStore(app),
            credentialsStore = AuthCredentialsStore(app),
        )
        return Bundle(
            identity = identity,
            professional = professional,
            registration = registration,
            deletion = deletion,
            progressStore = progressStore,
            consentStore = consentStore,
            cipher = cipher,
        )
    }
}
