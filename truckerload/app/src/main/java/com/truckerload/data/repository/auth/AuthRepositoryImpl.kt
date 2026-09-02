package com.truckerload.data.repository.auth

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import com.truckerload.di.UserComponentManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val authStore: AuthStore,
    private val userComponentManager: UserComponentManager,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> =
        combine(authStore.isLoggedIn, authStore.userId, authStore.email) { loggedIn, userId, email ->
            if (loggedIn && !userId.isNullOrBlank()) {
                AuthState.SignedIn(AuthUser(userId = userId, email = email))
            } else {
                AuthState.SignedOut
            }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.Main.immediate) {
            com.truckerload.sync.SessionTeardown.signOut(
                context = appContext,
                authStore = authStore,
                endSession = { userComponentManager.endSession() },
            )
        }
    }
}
