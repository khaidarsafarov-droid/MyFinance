package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthSessionHealth
import com.truckerload.data.preferences.SecurePreferences
import com.truckerload.data.preferences.StartupRepairStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.widget.WidgetUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Soft status strip for degraded auth / secure-storage / startup repair.
 * Never blocks local work.
 */
@Composable
fun AuthStatusBanner(modifier: Modifier = Modifier) {
    val authStore = LocalAuthStore.current
    val health by authStore.sessionHealth.collectAsStateWithLifecycle()
    val userId by authStore.userId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val repairStore = remember(context) { StartupRepairStore(context) }
    var needsBackfillRetry by remember(userId) {
        mutableStateOf(repairStore.needsBackfillRetry(userId))
    }
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()

    val messageRes = when {
        SecurePreferences.plaintextFallbackUsed -> R.string.auth_secure_storage_fallback_banner
        needsBackfillRetry -> R.string.auth_startup_backfill_retry_banner
        health == AuthSessionHealth.SESSION_UNCONFIRMED -> R.string.auth_session_unconfirmed_banner
        health == AuthSessionHealth.OFFLINE_LOCAL -> R.string.auth_session_offline_local_banner
        else -> null
    } ?: return

    val clickableRetry = needsBackfillRetry &&
        !SecurePreferences.plaintextFallbackUsed &&
        !userId.isNullOrBlank()

    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.labelMedium,
        color = tc.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(tc.AccentWarning.copy(alpha = 0.18f))
            .then(
                if (clickableRetry) {
                    Modifier.clickable {
                        val id = userId ?: return@clickable
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                val db = AppDatabase.getInstanceForActiveUser(context) ?: return@withContext false
                                val repo = LoadRepository(db)
                                val puDel = runCatching { repo.backfillPuDelMillisFromStops() }
                                val weeks = runCatching { repo.refreshReportingWeeks() }
                                if (puDel.isSuccess && weeks.isSuccess) {
                                    repairStore.markBackfillDone(id)
                                    WidgetUpdateWorker.refreshNow(context)
                                    true
                                } else {
                                    repairStore.markBackfillNeedsRetry(id)
                                    false
                                }
                            }
                            needsBackfillRetry = !ok
                        }
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
