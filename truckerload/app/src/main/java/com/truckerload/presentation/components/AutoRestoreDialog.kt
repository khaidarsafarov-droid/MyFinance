package com.truckerload.presentation.components

import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.utils.BackupService
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BackupRestore"

@Composable
fun AutoRestoreDialog(loadRepository: LoadRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsDataStore = remember { SettingsDataStore(context) }
    var visible by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }

    LaunchedEffect(loadRepository) {
        if (!settingsDataStore.isFirstRunOnce()) return@LaunchedEffect

        val isEmpty = loadRepository.getAllLoadsOnce().isEmpty()
        val backups = withContext(Dispatchers.IO) {
            BackupService.getAutoBackups(context)
        }
        visible = isEmpty && backups.isNotEmpty()
        if (visible) {
            Log.d(TAG, "First run: found ${backups.size} auto-backup(s), showing restore dialog")
        }
    }

    fun finishFirstRun() {
        scope.launch {
            settingsDataStore.markFirstRunComplete()
            Log.d(TAG, "First run completed, dialog dismissed")
        }
    }

    if (!visible || restoring) return

    AlertDialog(
        onDismissRequest = {
            visible = false
            finishFirstRun()
        },
        title = { Text(stringResource(R.string.auto_restore_title)) },
        text = { Text(stringResource(R.string.auto_restore_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    restoring = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val latest = BackupService.getAutoBackups(context).firstOrNull()
                                ?: return@withContext Result.failure<Int>(
                                    IllegalStateException(context.getString(R.string.auto_restore_no_file))
                                )
                            Log.d(TAG, "Restoring from ${latest.name}")
                            BackupService.restoreFromFile(context, latest)
                        }
                        restoring = false
                        visible = false
                        settingsDataStore.markFirstRunComplete()
                        result.fold(
                            onSuccess = { loadCount ->
                                WidgetDataUpdater.updateWidgetData(context)
                                WidgetUpdateWorker.refreshNow(context)
                                Log.d(TAG, "Restore success: $loadCount loads")
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.auto_restore_success, loadCount),
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Restore failed: ${e.message}", e)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.auto_restore_failed, e.message.orEmpty()),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.auto_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    visible = false
                    finishFirstRun()
                }
            ) {
                Text(stringResource(R.string.auto_restore_dismiss))
            }
        }
    )
}
