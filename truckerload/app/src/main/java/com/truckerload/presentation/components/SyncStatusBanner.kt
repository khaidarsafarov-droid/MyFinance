package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.sync.cloud.SyncState
import com.truckerload.data.sync.cloud.SyncStatusTracker
import com.truckerload.presentation.theme.LocalTruckColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Surfaces cloud sync failures from [SyncStatusTracker] (updated by the injectable
 * [com.truckerload.data.sync.cloud.CloudSyncEngine]). Tap retries session sync when retryable.
 */
@Composable
fun SyncStatusBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        SyncStatusEntryPoint::class.java,
    )
    val tracker = entryPoint.syncStatusTracker()
    val engine = entryPoint.cloudSyncEngine()
    val state by tracker.state.collectAsStateWithLifecycle()
    val error = state as? SyncState.Error ?: return
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()

    Text(
        text = if (error.retryable) {
            stringResource(R.string.sync_error_banner_retryable, error.message)
        } else {
            stringResource(R.string.sync_error_banner, error.message)
        },
        style = MaterialTheme.typography.labelMedium,
        color = tc.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(tc.AccentWarning.copy(alpha = 0.18f))
            .then(
                if (error.retryable) {
                    Modifier.clickable {
                        scope.launch {
                            runCatching { engine.onSessionReady() }
                        }
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncStatusEntryPoint {
    fun syncStatusTracker(): SyncStatusTracker
    fun cloudSyncEngine(): com.truckerload.data.sync.cloud.CloudSyncEngine
}
