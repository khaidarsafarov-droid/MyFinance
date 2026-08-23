package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.backup.BackupRestoreErrors
import com.truckerload.data.backup.BackupSchema
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsDataSection(
    settingsViewModel: SettingsViewModel,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val exportState by settingsViewModel.exportState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showExportActions by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupService.restoreFromUri(context, uri)
            }
            val message = result.fold(
                onSuccess = { it },
                onFailure = { BackupRestoreErrors.userMessage(context, it) },
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is SettingsViewModel.ExportState.Success -> {
                exportedFile = state.file
                showExportActions = true
                Toast.makeText(
                    context,
                    context.getString(R.string.export_loads_success, state.file.name),
                    Toast.LENGTH_LONG,
                ).show()
                settingsViewModel.resetExportState()
            }
            is SettingsViewModel.ExportState.Error -> {
                val message = when (state.message) {
                    SettingsViewModel.ERROR_NO_LOADS -> context.getString(R.string.no_loads_to_export)
                    else -> context.getString(R.string.export_loads_error, state.message)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                settingsViewModel.resetExportState()
            }
            else -> Unit
        }
    }

    BentoGlassSection(
        title = stringResource(R.string.settings_data_title),
        subtitle = stringResource(R.string.settings_backup_desc),
    ) {
        if (exportState is SettingsViewModel.ExportState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.exporting_loads),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        Button(
            onClick = { settingsViewModel.exportCsv() },
            enabled = exportState !is SettingsViewModel.ExportState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = context.getString(R.string.settings_cd_export_csv) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_csv))
            }
        }
        Button(
            onClick = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        BackupService.createManualBackup(context)
                    }
                    result?.let {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.settings_backup_saved,
                                it.loadCount,
                                it.save.displayPath,
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                        BackupService.shareBackupFile(context, it.save.uri)
                    } ?: Toast.makeText(
                        context,
                        context.getString(R.string.settings_backup_error),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = stringResource(R.string.settings_backup_create))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_backup_create))
            }
        }
        OutlinedButton(
            onClick = { showRestoreConfirm = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.settings_backup_restore))
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        restoreLauncher.launch(BackupSchema.RESTORE_OPEN_MIME_TYPES)
                    },
                ) {
                    Text(stringResource(R.string.backup_restore_confirm_action))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    val exportTarget = exportedFile
    if (showExportActions && exportTarget != null) {
        AlertDialog(
            onDismissRequest = {
                showExportActions = false
                exportedFile = null
            },
            title = { Text(stringResource(R.string.export_actions_title), color = tc.TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.export_loads_success, exportTarget.name),
                    color = tc.TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.openExportsFolder(exportTarget)
                        showExportActions = false
                        exportedFile = null
                    },
                ) {
                    Text(stringResource(R.string.open_folder))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showExportActions = false
                        exportedFile = null
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}
