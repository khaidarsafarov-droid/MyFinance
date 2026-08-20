package com.truckerload.data.repository.account

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.ConsentStore
import com.truckerload.data.preferences.RegistrationProgressStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.privacy.SensitiveFieldCipher
import com.truckerload.di.userComponentManager
import com.truckerload.sync.SessionTeardown

/**
 * Cascading account delete. Local journal is personal (not a fleet ledger), so loads
 * and related rows in the account-scoped Room file are wiped with identity.
 */
class AccountDeletionService(
    private val context: Context,
    private val userId: String,
    private val database: AppDatabase,
    private val identity: AccountIdentityRepository,
    private val professional: DriverProfessionalRepository,
    private val community: CommunityProfileRepository,
    private val cipher: SensitiveFieldCipher,
    private val consentStore: ConsentStore,
    private val progressStore: RegistrationProgressStore,
    private val userProfileStore: UserProfileStore,
    private val authStore: AuthStore,
    private val credentialsStore: AuthCredentialsStore,
) {
    /**
     * In-memory / testable wipe of identity + journal tables without closing the process session.
     */
    suspend fun cascadeDeleteLocalTables() {
        identity.delete(userId)
        professional.delete(userId)
        community.delete(userId)
        database.clearAllTables()
        cipher.wipe()
        consentStore.clear(userId)
        progressStore.clear()
        userProfileStore.clearProfile()
    }

    suspend fun deleteAccountAndSignOut() {
        val email = authStore.email.value
        cascadeDeleteLocalTables()
        if (email.isNotBlank()) {
            runCatching { credentialsStore.clearCredentials(email) }
        }
        AppDatabase.closeCurrent()
        context.applicationContext.deleteDatabase(AppDatabase.databaseNameFor(userId))
        runCatching {
            SessionTeardown.signOut(
                context = context,
                authStore = authStore,
                endSession = { context.userComponentManager().endSession() },
            )
        }.onFailure {
            authStore.logout()
        }
    }
}
