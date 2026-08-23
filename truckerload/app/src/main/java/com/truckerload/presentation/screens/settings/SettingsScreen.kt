package com.truckerload.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TableChart
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AppThemeMode
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.backup.BackupRestoreErrors
import com.truckerload.data.backup.BackupSchema
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.screens.settings.ThemeSettingsSection
import com.truckerload.presentation.screens.settings.LanguageSettingsSection
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.SoftTabletTwoPane
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.di.userComponentManager
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    showBack: Boolean = false,
    onOpenPrivacy: () -> Unit = {},
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
    val oledDark by settingsDataStore.oledDark.collectAsStateWithLifecycle(initialValue = false)
    val dynamicColor by settingsDataStore.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val reduceMotion by settingsDataStore.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val quietHoursEnabled by settingsDataStore.quietHoursEnabled.collectAsStateWithLifecycle(initialValue = false)
    val quietHoursStart by settingsDataStore.quietHoursStart.collectAsStateWithLifecycle(initialValue = 22)
    val quietHoursEnd by settingsDataStore.quietHoursEnd.collectAsStateWithLifecycle(initialValue = 7)
    val notifyMissingWeek by settingsDataStore.notifyMissingWeek.collectAsStateWithLifecycle(initialValue = true)
    val notifyMaintenance by settingsDataStore.notifyMaintenance.collectAsStateWithLifecycle(initialValue = true)
    val appLanguage by settingsDataStore.language.collectAsStateWithLifecycle(initialValue = com.truckerload.data.preferences.AppLanguage.RU)
    val tc = LocalTruckColors.current
    val authStore = LocalAuthStore.current
    val store = LocalRpmThresholdsStore.current
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val exportState by settingsViewModel.exportState.collectAsStateWithLifecycle()
    val restoreState by settingsViewModel.restoreState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var backupRestoreMessage by remember { mutableStateOf<String?>(null) }
    var exportedFile by remember { mutableStateOf<java.io.File?>(null) }
    var showExportActions by remember { mutableStateOf(false) }

    val loadRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        settingsViewModel.restoreLoadsFromUri(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupService.restoreFromUri(context, uri)
            }
            backupRestoreMessage = result.fold(
                onSuccess = { it },
                onFailure = { BackupRestoreErrors.userMessage(context, it) }
            )
            android.widget.Toast.makeText(context, backupRestoreMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val thresholds by store.thresholds.collectAsStateWithLifecycle()
    var minInput by remember(thresholds) { mutableStateOf(thresholds.minProfit.toString()) }
    var targetInput by remember(thresholds) { mutableStateOf(thresholds.targetProfit.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is SettingsViewModel.ExportState.Success -> {
                exportedFile = state.file
                showExportActions = true
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.export_loads_success, state.file.name),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                settingsViewModel.resetExportState()
            }
            is SettingsViewModel.ExportState.Error -> {
                val message = when (state.message) {
                    SettingsViewModel.ERROR_NO_LOADS -> context.getString(R.string.no_loads_to_export)
                    else -> context.getString(R.string.export_loads_error, state.message)
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                settingsViewModel.resetExportState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is SettingsViewModel.RestoreState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.restore_success, state.imported, state.skipped),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                settingsViewModel.resetRestoreState()
            }
            is SettingsViewModel.RestoreState.Error -> {
                val message = when (state.message) {
                    SettingsViewModel.ERROR_NO_PARSED_LOADS -> context.getString(R.string.no_loads_to_export)
                    else -> context.getString(R.string.restore_error, state.message)
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                settingsViewModel.resetRestoreState()
            }
            else -> Unit
        }
    }

    val tabletChrome = useNavigationRail()
    SoftAppPageScaffold(
        title = stringResource(R.string.settings_title),
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
        ) {
            SoftTabletTwoPane(
                start = {
                    Column {
                        ThemeSettingsSection(
                            selected = themeMode,
                            oledDark = oledDark,
                            dynamicColor = dynamicColor,
                        )
                        AccessibilitySettingsSection(reduceMotion = reduceMotion)
                        LanguageSettingsSection(selected = appLanguage)
                        FeedbackSettingsSection(settingsViewModel = settingsViewModel)
                    }
                },
                end = {
                    Column {
                        BiometricSettingsSection()
                        PrivacySettingsSection(onOpenPrivacy = onOpenPrivacy)
                        NotificationSettingsSection(
                            quietHoursEnabled = quietHoursEnabled,
                            quietHoursStart = quietHoursStart,
                            quietHoursEnd = quietHoursEnd,
                            notifyMissingWeek = notifyMissingWeek,
                            notifyMaintenance = notifyMaintenance,
                        )
                        TelegramSettingsSection()
                    }
                },
            )

            BentoGlassSection(title = stringResource(R.string.settings_rpm_thresholds_title)) {
                RpmColorLegend(compact = true)
                val fieldColors = AppTextFieldDefaults.outlined()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it; error = null },
                        label = { Text(stringResource(R.string.settings_red_threshold_short)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it; error = null },
                        label = { Text(stringResource(R.string.settings_green_threshold_short)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = fieldColors,
                    )
                }
                error?.let { err ->
                    Text(
                        text = err,
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        val min = minInput.replace(",", ".").toDoubleOrNull()
                        val target = targetInput.replace(",", ".").toDoubleOrNull()
                        when {
                            min == null -> error = context.getString(R.string.settings_red_threshold_error)
                            target == null -> error = context.getString(R.string.settings_green_threshold_error)
                            else -> store.save(min, target)
                                .onSuccess {
                                    error = null
                                    android.widget.Toast.makeText(context, context.getString(R.string.settings_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { error = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_rpm_save))
                }
            }

            BentoGlassSection(
                title = stringResource(R.string.export_loads),
            ) {
                if (exportState is SettingsViewModel.ExportState.Loading ||
                    restoreState is SettingsViewModel.RestoreState.Loading
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    val progressText = when {
                        exportState is SettingsViewModel.ExportState.Loading ->
                            stringResource(R.string.exporting_loads)
                        else -> stringResource(R.string.restoring_loads)
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary
                    )
                }
                Button(
                    onClick = { settingsViewModel.exportLoads() },
                    enabled = exportState !is SettingsViewModel.ExportState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_loads))
                    }
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_csv))
                    }
                }
                OutlinedButton(
                    onClick = { loadRestoreLauncher.launch(arrayOf("text/plain")) },
                    enabled = restoreState !is SettingsViewModel.RestoreState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = context.getString(R.string.settings_cd_import) },
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.restore_loads))
                    }
                }
            }

            BentoGlassSection(
                title = stringResource(R.string.settings_backup_title),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                BackupService.createManualBackup(context)
                            }
                            result?.let {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.settings_backup_saved,
                                        it.loadCount,
                                        it.save.displayPath
                                    ),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                BackupService.shareBackupFile(context, it.save.uri)
                            } ?: android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.Backup, contentDescription = stringResource(R.string.settings_backup_create))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_backup_create))
                    }
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(BackupSchema.RESTORE_OPEN_MIME_TYPES) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.settings_backup_restore))
                }
            }

            GoogleDriveSyncSection(tc = tc)

            DeleteAccountSection()

            BentoGlassSection(
                title = stringResource(R.string.settings_logout_title),
            ) {
                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = stringResource(R.string.settings_logout_button))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_logout_button))
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    val exportTarget = exportedFile
    if (showExportActions && exportTarget != null) {
        AlertDialog(
            onDismissRequest = {
                showExportActions = false
                exportedFile = null
            },
            title = { Text(stringResource(R.string.export_actions_title), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.export_loads_success, exportTarget.name), color = tc.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.openExportsFolder(exportTarget)
                        showExportActions = false
                        exportedFile = null
                    }
                ) {
                    Text(stringResource(R.string.open_folder))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showExportActions = false
                        exportedFile = null
                    }
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.settings_logout_confirm_message), color = tc.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            com.truckerload.sync.SessionTeardown.signOut(
                                context = context,
                                authStore = authStore,
                                endSession = { context.userComponentManager().endSession() },
                            )
                            showLogoutConfirm = false
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_logout_success), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_logout_button))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
