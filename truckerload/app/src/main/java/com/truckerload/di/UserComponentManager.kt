package com.truckerload.di

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.ktor.HttpClientProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the active [UserComponent]. Create on login, destroy on logout / account switch.
 *
 * Thread-safe: MainActivity, workers, and ViewModel injection may race briefly during switch.
 */
@Singleton
class UserComponentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileStore: UserProfileStore,
    private val httpClientProvider: HttpClientProvider,
) {
    private val active = AtomicReference<UserComponent?>(null)
    private val sessionLock = Any()

    @Synchronized
    fun currentOrNull(): UserComponent? = active.get()

    @Synchronized
    fun currentUserIdOrNull(): String? = active.get()?.userId

    @Synchronized
    fun require(): UserComponent =
        active.get()
            ?: error("No active UserComponent — call startSession(userId) after login")

    /**
     * Ensures a graph for [userId]. No-op when already bound to the same user.
     * Switches atomically so [require] never sees a torn-down graph mid-switch.
     */
    fun startSession(userId: String): UserComponent = synchronized(sessionLock) {
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        active.get()?.let { existing ->
            if (existing.userId == id) return existing
            Log.i(TAG, "Switching UserComponent ${existing.userId} → $id")
            userProfileStore.unbind()
        } ?: Log.i(TAG, "Starting UserComponent for $id")
        // FIX: create + swap before clearing active — no null window for concurrent readers
        val created = UserComponent.create(context, id, userProfileStore, httpClientProvider)
        active.set(created)
        return created
    }

    /** Logout / guest: drop graph, close Room, unbind profile. */
    fun endSession() = synchronized(sessionLock) {
        val previous = active.getAndSet(null) ?: return
        Log.i(TAG, "Ending UserComponent")
        AppDatabase.closeIfCurrentUser(previous.userId)
        userProfileStore.unbind()
    }

    companion object {
        private const val TAG = "UserComponentManager"
    }
}
