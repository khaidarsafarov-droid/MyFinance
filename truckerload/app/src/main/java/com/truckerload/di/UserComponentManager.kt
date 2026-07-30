package com.truckerload.di

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
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
) {
    private val active = AtomicReference<UserComponent?>(null)

    fun currentOrNull(): UserComponent? = active.get()

    fun currentUserIdOrNull(): String? = active.get()?.userId

    fun require(): UserComponent =
        active.get()
            ?: error("No active UserComponent — call startSession(userId) after login")

    /**
     * Ensures a graph for [userId]. No-op when already bound to the same user.
     * Switches close the previous Room connection first.
     */
    @Synchronized
    fun startSession(userId: String): UserComponent {
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        val existing = active.get()
        if (existing != null && existing.userId == id) return existing
        if (existing != null) {
            Log.i(TAG, "Switching UserComponent ${existing.userId} → $id")
            destroySessionLocked()
        } else {
            Log.i(TAG, "Starting UserComponent for $id")
        }
        val created = UserComponent.create(context, id, userProfileStore)
        active.set(created)
        return created
    }

    /** Logout / guest: drop graph, close Room, unbind profile. */
    @Synchronized
    fun endSession() {
        if (active.get() == null) return
        Log.i(TAG, "Ending UserComponent")
        destroySessionLocked()
    }

    private fun destroySessionLocked() {
        active.set(null)
        AppDatabase.closeCurrent()
        userProfileStore.unbind()
    }

    companion object {
        private const val TAG = "UserComponentManager"
    }
}
