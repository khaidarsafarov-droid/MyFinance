package com.truckerload.di

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the [UserComponent] of the single device profile. Created when the profile opens,
 * dropped only by a full data erase ([endSession]).
 */
@Singleton
class UserComponentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileStore: UserProfileStore,
) {
    private val lock = Any()
    private var active: UserComponent? = null

    fun currentOrNull(): UserComponent? = synchronized(lock) { active }

    fun currentUserIdOrNull(): String? = synchronized(lock) { active?.userId }

    fun require(): UserComponent =
        currentOrNull() ?: error("No active UserComponent — call startSession(userId) first")

    /** Ensures the graph for [userId]. No-op when it is already open. */
    fun startSession(userId: String): UserComponent {
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        synchronized(lock) {
            active?.let { existing ->
                if (existing.userId == id) return existing
                Log.w(TAG, "Replacing UserComponent ${existing.userId} → $id")
                userProfileStore.unbind()
            }
            return UserComponent.create(context, id, userProfileStore).also { active = it }
        }
    }

    /** Full data erase: drop graph, close Room, unbind profile. */
    fun endSession() {
        synchronized(lock) {
            val previous = active ?: return
            active = null
            Log.i(TAG, "Ending UserComponent")
            AppDatabase.closeIfCurrentUser(previous.userId)
            userProfileStore.unbind()
        }
    }

    companion object {
        private const val TAG = "UserComponentManager"
    }
}
