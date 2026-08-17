package com.truckerload.presentation.screens.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.backup.GoogleDriveBackupService
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.TruckColorPalette
import com.truckerload.utils.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GoogleDriveSyncSection(tc: TruckColorPalette) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val prefs = remember { GoogleDriveBackupService.prefs(context) }
    val connectivity by ConnectivityObserver.observe(context)
        .collectAsStateWithLifecycle(initialValue = ConnectivityStatus.Online)
    var linkedEmail by remember {
        mutableStateOf(
            GoogleDriveBackupService.linkedAccountEmail(context) ?: run {
                GoogleDriveBackupService.syncLinkedAccountFromGoogle(context)
                GoogleDriveBackupService.linkedAccountEmail(context)
            },
        )
    }
    var autoSync by remember { mutableStateOf(prefs.autoSyncEnabled) }
    var lastSyncAt by remember { mutableStateOf(prefs.lastSyncAt) }
    var busy by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreConflict by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val ok = GoogleDriveBackupService.onSignInResult(context, result.data)
        if (ok) {
            linkedEmail = prefs.accountEmail
            Toast.makeText(context, context.getString(R.string.drive_sync_connected), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.drive_sync_connect_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun toastResult(result: Result<String>) {
        result.fold(
            onSuccess = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
            onFailure = { err ->
                if (activity != null && GoogleDriveBackupService.launchConsentIfNeeded(activity, err)) {
                    return
                }
                Toast.makeText(
                    context,
                    err.message ?: context.getString(R.string.drive_sync_api_error, ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    BentoGlassSection(
        title = stringResource(R.string.drive_sync_title),
        subtitle = stringResource(R.string.drive_sync_desc),
    ) {
        Text(
            text = if (linkedEmail.isNullOrBlank()) {
                stringResource(R.string.drive_sync_status_off)
            } else {
                stringResource(R.string.drive_sync_status_on, linkedEmail.orEmpty())
            },
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextPrimary,
        )
        Text(
            text = stringResource(
                R.string.drive_sync_last,
                if (lastSyncAt > 0L) dateFormat.format(Date(lastSyncAt))
                else stringResource(R.string.drive_sync_last_never),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        if (connectivity == ConnectivityStatus.Offline) {
            Text(
                text = stringResource(R.string.connectivity_offline_banner),
                style = MaterialTheme.typography.labelSmall,
                color = tc.AccentExpense,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            Text(
                text = stringResource(R.string.drive_sync_busy),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (linkedEmail.isNullOrBlank()) {
            Button(
                onClick = {
                    val host = activity ?: context.findActivity()
                    if (host == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.drive_sync_need_activity),
                            Toast.LENGTH_LONG,
                        ).show()
                        return@Button
                    }
                    runCatching {
                        driveSignInLauncher.launch(GoogleDriveBackupService.signInIntent(host))
                    }.onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.drive_sync_connect_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = stringResource(R.string.drive_sync_connect))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.drive_sync_connect))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.drive_sync_auto),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.drive_sync_auto_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.TextSecondary,
                    )
                }
                Switch(
                    checked = autoSync,
                    onCheckedChange = {
                        autoSync = it
                        prefs.autoSyncEnabled = it
                    },
                    colors = AppSwitchDefaults.colors(),
                    enabled = !busy,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        val result = withContext(Dispatchers.IO) {
                            GoogleDriveBackupService.backupNow(context)
                        }
                        lastSyncAt = prefs.lastSyncAt
                        busy = false
                        toastResult(result)
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.drive_sync_backup_now))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        val (hasRemote, localDirty) = withContext(Dispatchers.IO) {
                            val remote = GoogleDriveBackupService.probeRemote(context)
                            val dirty = GoogleDriveBackupService.hasLocalChangesAfterLastSync(context)
                            remote to dirty
                        }
                        restoreConflict = hasRemote &&
                            GoogleDriveBackupService.shouldWarnBeforeRestore(context, localDirty)
                        busy = false
                        showRestoreConfirm = true
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.drive_sync_restore_now))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) {
                            GoogleDriveBackupService.disconnect(context)
                        }
                        linkedEmail = null
                        lastSyncAt = 0L
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.drive_sync_disconnect))
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(R.string.drive_sync_restore_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (restoreConflict) {
                            R.string.drive_sync_restore_conflict_body
                        } else {
                            R.string.drive_sync_restore_confirm_body
                        },
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        scope.launch {
                            busy = true
                            val result = withContext(Dispatchers.IO) {
                                GoogleDriveBackupService.restoreNow(context)
                            }
                            lastSyncAt = prefs.lastSyncAt
                            busy = false
                            toastResult(result)
                        }
                    }
                ) {
                    Text(stringResource(R.string.drive_sync_restore_now))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
