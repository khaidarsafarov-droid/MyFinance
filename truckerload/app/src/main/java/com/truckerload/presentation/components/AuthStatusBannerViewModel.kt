package com.truckerload.presentation.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.StartupRepairStore
import com.truckerload.di.UserComponentManager
import com.truckerload.widget.WidgetUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Startup-repair retry behind [AuthStatusBanner]. Uses the session's own
 * [com.truckerload.data.repository.LoadRepository]; the banner only shows state.
 */
@HiltViewModel
class AuthStatusBannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val users: UserComponentManager,
) : ViewModel() {

    private val repairStore = StartupRepairStore(context)

    /** User ids whose retry finished in this screen's lifetime, mapped to "still needs retry". */
    private val _retryResult = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val retryResult: StateFlow<Map<String, Boolean>> = _retryResult.asStateFlow()

    fun needsBackfillRetry(userId: String?): Boolean =
        userId?.let { _retryResult.value[it] } ?: repairStore.needsBackfillRetry(userId)

    fun retryBackfill(userId: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val session = users.currentOrNull()?.takeIf { it.userId == userId }
                    ?: return@withContext false
                val repo = session.loadRepository
                val puDel = runCatching { repo.backfillPuDelMillisFromStops() }
                val weeks = runCatching { repo.refreshReportingWeeks() }
                if (puDel.isSuccess && weeks.isSuccess) {
                    repairStore.markBackfillDone(userId)
                    WidgetUpdateWorker.refreshNow(context)
                    true
                } else {
                    repairStore.markBackfillNeedsRetry(userId)
                    false
                }
            }
            _retryResult.value = _retryResult.value + (userId to !ok)
        }
    }
}
