package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Login identity (Google / email / sign-up), **namespaced per account**.
 * Call [bindUser] after determining the account id; data persists across logouts for that user.
 */
data class UserProfile(
    val email: String,
    val givenName: String,
    val familyName: String,
    val photoUrl: String?,
    val phoneNumber: String? = null,
    /** Google OpenID `sub` — stable identity key for this Google account. */
    val googleId: String? = null,
    /** Unique Truck Log handle for finding friends (@nickname). */
    val nickname: String? = null,
    /** True after the driver sets a display name in the app (not Google's name). */
    val customDisplayName: Boolean = false,
    /** True after the driver uploads or removes a profile photo in the app. */
    val customPhoto: Boolean = false,
) {
    val displayName: String
        get() = listOf(givenName, familyName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { email }
}

class UserProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private var boundUserId: String? = null
    private var prefs: SharedPreferences =
        appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    val boundUserIdOrNull: String? get() = boundUserId

    fun bindUser(userId: String) {
        val id = userId.trim()
        require(id.isNotBlank())
        if (boundUserId == id) return
        boundUserId = id
        prefs = appContext.getSharedPreferences(prefsName(id), Context.MODE_PRIVATE)
        migrateLegacyProfileIfNeeded(id)
        _profile.value = loadProfile()
        _setupComplete.value = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    /** Detach without wiping the account's stored profile (multi-user logout). */
    fun unbind() {
        boundUserId = null
        _profile.value = null
        _setupComplete.value = false
    }

    fun saveProfile(profile: UserProfile) {
        val id = boundUserId
        require(!id.isNullOrBlank()) { "bindUser() before saveProfile()" }
        prefs.edit {
            putString(KEY_EMAIL, profile.email)
            putString(KEY_GIVEN_NAME, profile.givenName)
            putString(KEY_FAMILY_NAME, profile.familyName)
            putString(KEY_PHOTO_URL, profile.photoUrl)
            putString(KEY_PHONE, profile.phoneNumber)
            if (profile.googleId.isNullOrBlank()) remove(KEY_GOOGLE_ID)
            else putString(KEY_GOOGLE_ID, profile.googleId)
            if (profile.nickname.isNullOrBlank()) remove(KEY_NICKNAME)
            else putString(KEY_NICKNAME, profile.nickname)
            putBoolean(KEY_CUSTOM_DISPLAY_NAME, profile.customDisplayName)
            putBoolean(KEY_CUSTOM_PHOTO, profile.customPhoto)
        }
        _profile.value = profile
    }

    fun setSetupComplete(complete: Boolean) {
        val id = boundUserId ?: return
        prefs = appContext.getSharedPreferences(prefsName(id), Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_SETUP_COMPLETE, complete) }
        _setupComplete.value = complete
    }

    /** Wipe identity for the bound account (rare; prefer [unbind] on logout). */
    fun clearProfile() {
        val id = boundUserId
        if (id != null) {
            prefs.edit { clear() }
        }
        _profile.value = null
        _setupComplete.value = false
    }

    private fun loadProfile(): UserProfile? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        if (email.isBlank()) return null
        return UserProfile(
            email = email,
            givenName = prefs.getString(KEY_GIVEN_NAME, "").orEmpty(),
            familyName = prefs.getString(KEY_FAMILY_NAME, "").orEmpty(),
            photoUrl = prefs.getString(KEY_PHOTO_URL, null)?.takeIf { it.isNotBlank() },
            phoneNumber = prefs.getString(KEY_PHONE, null)?.takeIf { it.isNotBlank() },
            googleId = prefs.getString(KEY_GOOGLE_ID, null)?.takeIf { it.isNotBlank() },
            nickname = prefs.getString(KEY_NICKNAME, null)?.takeIf { it.isNotBlank() },
            customDisplayName = prefs.getBoolean(KEY_CUSTOM_DISPLAY_NAME, false),
            customPhoto = prefs.getBoolean(KEY_CUSTOM_PHOTO, false),
        )
    }

    private fun migrateLegacyProfileIfNeeded(userId: String) {
        val meta = appContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (meta.getBoolean(KEY_LEGACY_PROFILE_MIGRATED, false)) return
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val email = legacy.getString(KEY_EMAIL, null)
        if (email.isNullOrBlank()) {
            meta.edit { putBoolean(KEY_LEGACY_PROFILE_MIGRATED, true) }
            return
        }
        if (prefs.getString(KEY_EMAIL, null).isNullOrBlank()) {
            prefs.edit {
                putString(KEY_EMAIL, email)
                putString(KEY_GIVEN_NAME, legacy.getString(KEY_GIVEN_NAME, "").orEmpty())
                putString(KEY_FAMILY_NAME, legacy.getString(KEY_FAMILY_NAME, "").orEmpty())
                putString(KEY_PHOTO_URL, legacy.getString(KEY_PHOTO_URL, null))
                putString(KEY_PHONE, legacy.getString(KEY_PHONE, null))
                putBoolean(KEY_SETUP_COMPLETE, legacy.getBoolean(KEY_SETUP_COMPLETE, false))
            }
        }
        meta.edit {
            putBoolean(KEY_LEGACY_PROFILE_MIGRATED, true)
            putString(KEY_LEGACY_PROFILE_OWNER, userId)
        }
    }

    companion object {
        private const val LEGACY_PREFS_NAME = "truckerload_user_profile"
        private const val META_PREFS = "truckerload_account_meta"
        private const val KEY_LEGACY_PROFILE_MIGRATED = "legacy_profile_migrated"
        private const val KEY_LEGACY_PROFILE_OWNER = "legacy_profile_owner"
        private const val KEY_EMAIL = "email"
        private const val KEY_GIVEN_NAME = "given_name"
        private const val KEY_FAMILY_NAME = "family_name"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_PHONE = "phone_number"
        private const val KEY_GOOGLE_ID = "google_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_CUSTOM_DISPLAY_NAME = "custom_display_name"
        private const val KEY_CUSTOM_PHOTO = "custom_photo"
        private const val KEY_SETUP_COMPLETE = "profile_setup_complete"

        fun prefsName(userId: String): String =
            "truckerload_user_profile_${AccountIds.sanitizeFilePart(userId)}"
    }
}
