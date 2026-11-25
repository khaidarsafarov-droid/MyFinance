package com.truckerload.presentation.di

import androidx.lifecycle.ViewModel
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthSession
import com.truckerload.data.preferences.AuthSessionHealth
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Read-only view of the signed-in account for screens that used to read [AuthStore] and
 * [UserProfileStore] through composition locals.
 */
@HiltViewModel
@Suppress("ktlint:standard:class-signature", "ktlint:standard:function-signature")
class AccountSessionViewModel @Inject constructor(
    private val authStore: AuthStore,
    userProfileStore: UserProfileStore,
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = userProfileStore.profile
    val sessionHealth: StateFlow<AuthSessionHealth> = authStore.sessionHealth
    val userId: StateFlow<String?> = authStore.userId

    fun authProvider(): AuthProvider = authStore.authProvider()

    fun sessionOrNull(): AuthSession? = authStore.sessionOrNull()

    fun currentUserIdOrNull(): String? = authStore.currentUserIdOrNull()
}
