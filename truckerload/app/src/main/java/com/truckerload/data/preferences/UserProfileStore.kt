package com.truckerload.data.preferences

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Хранилище профиля пользователя (из Google или email).
 * Сохраняет имя, фамилию, email. Дата рождения из Google недоступна — её нужно вводить отдельно.
 */
data class UserProfile(
    val email: String,
    val givenName: String,
    val familyName: String,
    val photoUrl: String?
) {
    val displayName: String
        get() = listOf(givenName, familyName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { email }
}

class UserProfileStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    fun saveProfile(profile: UserProfile) {
        prefs.edit {
            putString(KEY_EMAIL, profile.email)
            putString(KEY_GIVEN_NAME, profile.givenName)
            putString(KEY_FAMILY_NAME, profile.familyName)
            putString(KEY_PHOTO_URL, profile.photoUrl)
        }
        _profile.value = profile
    }

    fun clearProfile() {
        prefs.edit {
            remove(KEY_EMAIL)
            remove(KEY_GIVEN_NAME)
            remove(KEY_FAMILY_NAME)
            remove(KEY_PHOTO_URL)
        }
        _profile.value = null
    }

    private fun loadProfile(): UserProfile? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        if (email.isBlank()) return null
        return UserProfile(
            email = email,
            givenName = prefs.getString(KEY_GIVEN_NAME, "").orEmpty(),
            familyName = prefs.getString(KEY_FAMILY_NAME, "").orEmpty(),
            photoUrl = prefs.getString(KEY_PHOTO_URL, null)?.takeIf { it.isNotBlank() }
        )
    }

    companion object {
        private const val PREFS_NAME = "truckerload_user_profile"
        private const val KEY_EMAIL = "email"
        private const val KEY_GIVEN_NAME = "given_name"
        private const val KEY_FAMILY_NAME = "family_name"
        private const val KEY_PHOTO_URL = "photo_url"
    }
}
