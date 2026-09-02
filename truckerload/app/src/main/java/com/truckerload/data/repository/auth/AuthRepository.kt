package com.truckerload.data.repository.auth

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val userId: String,
    val email: String,
    val displayName: String = "",
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>

    suspend fun signOut()
}
