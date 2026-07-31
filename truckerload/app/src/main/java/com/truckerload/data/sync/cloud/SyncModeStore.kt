package com.truckerload.data.sync.cloud

import android.content.Context
import androidx.core.content.edit
import com.truckerload.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists [SyncMode] in SharedPreferences. Default [SyncMode.HYBRID] during transition.
 * `LOCAL_ONLY_MODE=true` or blank `SYNC_BACKEND_URL` forces effective [SyncMode.DEVICE_ONLY]
 * for cloud calls without mutating the user preference.
 */
@Singleton
class SyncModeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readPersisted())
    val mode: StateFlow<SyncMode> = _mode.asStateFlow()

    fun current(): SyncMode = _mode.value

    /** User-selected mode (may still be overridden by build gates). */
    fun setMode(mode: SyncMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _mode.value = mode
    }

    /**
     * Mode after applying compile-time gates. Use this before any Ktor call.
     */
    fun effectiveMode(): SyncMode {
        if (BuildConfig.LOCAL_ONLY_MODE) return SyncMode.DEVICE_ONLY
        if (BuildConfig.SYNC_BACKEND_URL.isBlank()) return SyncMode.DEVICE_ONLY
        return current()
    }

    fun allowsCloudCalls(): Boolean = effectiveMode().allowsCloudCalls

    private fun readPersisted(): SyncMode =
        SyncMode.parse(prefs.getString(KEY_MODE, SyncMode.HYBRID.name))

    companion object {
        private const val PREFS = "truckerload_sync_mode"
        private const val KEY_MODE = "sync_mode"
    }
}
