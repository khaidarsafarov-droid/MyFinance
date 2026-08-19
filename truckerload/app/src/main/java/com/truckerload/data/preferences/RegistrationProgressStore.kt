package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.domain.account.RegistrationProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegistrationProgressStore(context: Context) {
    private val appContext = context.applicationContext
    private var boundUserId: String? = null
    private var prefs: SharedPreferences =
        appContext.getSharedPreferences("truckerload_reg_progress_unbound", Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(RegistrationProgress())
    val progress: StateFlow<RegistrationProgress> = _progress.asStateFlow()

    fun bindUser(userId: String, setupAlreadyComplete: Boolean) {
        val id = userId.trim()
        require(id.isNotBlank())
        boundUserId = id
        prefs = appContext.getSharedPreferences(prefsName(id), Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_BASIC) && setupAlreadyComplete) {
            prefs.edit {
                putBoolean(KEY_CREDENTIALS, true)
                putBoolean(KEY_VERIFY, true)
                putBoolean(KEY_BASIC, true)
                putBoolean(KEY_PRO_DONE, true)
                putBoolean(KEY_PRO_SKIP, false)
                putBoolean(KEY_COM_DONE, true)
                putBoolean(KEY_COM_SKIP, false)
            }
        }
        _progress.value = read()
    }

    fun current(): RegistrationProgress = _progress.value

    fun save(progress: RegistrationProgress) {
        prefs.edit {
            putBoolean(KEY_CREDENTIALS, progress.credentialsComplete)
            putBoolean(KEY_VERIFY, progress.verificationComplete)
            putBoolean(KEY_BASIC, progress.basicComplete)
            putBoolean(KEY_PRO_DONE, progress.professionalComplete)
            putBoolean(KEY_PRO_SKIP, progress.professionalSkipped)
            putBoolean(KEY_COM_DONE, progress.communityComplete)
            putBoolean(KEY_COM_SKIP, progress.communitySkipped)
        }
        _progress.value = progress
    }

    fun clear() {
        prefs.edit { clear() }
        _progress.value = RegistrationProgress()
    }

    private fun read(): RegistrationProgress = RegistrationProgress(
        credentialsComplete = prefs.getBoolean(KEY_CREDENTIALS, false),
        verificationComplete = prefs.getBoolean(KEY_VERIFY, false),
        basicComplete = prefs.getBoolean(KEY_BASIC, false),
        professionalComplete = prefs.getBoolean(KEY_PRO_DONE, false),
        professionalSkipped = prefs.getBoolean(KEY_PRO_SKIP, false),
        communityComplete = prefs.getBoolean(KEY_COM_DONE, false),
        communitySkipped = prefs.getBoolean(KEY_COM_SKIP, false),
    )

    companion object {
        private const val KEY_CREDENTIALS = "credentials"
        private const val KEY_VERIFY = "verify"
        private const val KEY_BASIC = "basic"
        private const val KEY_PRO_DONE = "professional_done"
        private const val KEY_PRO_SKIP = "professional_skip"
        private const val KEY_COM_DONE = "community_done"
        private const val KEY_COM_SKIP = "community_skip"

        fun prefsName(userId: String): String =
            "truckerload_reg_progress_${AccountIds.sanitizeFilePart(userId)}"
    }
}
