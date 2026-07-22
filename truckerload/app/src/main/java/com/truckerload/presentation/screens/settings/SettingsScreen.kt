package com.truckerload.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.VolumeUp
import android.app.Activity
import android.widget.Toast
import com.truckerload.data.backup.GoogleDriveBackupService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Receipt
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AppThemeMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.screens.settings.ThemeSettingsSection
import com.truckerload.presentation.screens.settings.LanguageSettingsSection
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.DarkGlassScreenTitle
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.theme.TruckColorPalette
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.utils.BatteryOptimizationHelper
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTaxTracker: () -> Unit = {},
    onAddPaycheck: () -> Unit = {},
    onAddDiesel: () -> Unit = {},
    showBack: Boolean = false
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val themeMode by settingsDataStore.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
    val appLanguage by settingsDataStore.language.collectAsState(initial = com.truckerload.data.preferences.AppLanguage.RU)
    val tc = LocalTruckColors.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val store = LocalRpmThresholdsStore.current
    val context = LocalContext.current
    val loadRepo = LocalLoadRepository.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(loadRepo, context)
    )
    val exportState by settingsViewModel.exportState.collectAsState()
    val restoreState by settingsViewModel.restoreState.collectAsState()
    val sendTelegramState by settingsViewModel.sendTelegramState.collectAsState()
    val savedTelegramChatId by settingsViewModel.telegramChatId.collectAsState()
    val scope = rememberCoroutineScope()
    var backupRestoreMessage by remember { mutableStateOf<String?>(null) }
    var exportedFile by remember { mutableStateOf<java.io.File?>(null) }
    var showExportActions by remember { mutableStateOf(false) }
    var showTelegramIdDialog by remember { mutableStateOf(false) }
    var telegramIdInput by remember(savedTelegramChatId) {
        mutableStateOf(savedTelegramChatId?.toString().orEmpty())
    }
    var pendingTelegramFile by remember { mutableStateOf<java.io.File?>(null) }

    val loadRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        settingsViewModel.restoreLoadsFromUri(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupService.restoreFromUri(context, uri)
            }
            backupRestoreMessage = result.fold(
                onSuccess = { it },
                onFailure = { context.getString(R.string.settings_restore_error, it.message.orEmpty()) }
            )
            android.widget.Toast.makeText(context, backupRestoreMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val thresholds by store.thresholds.collectAsState()
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

    LaunchedEffect(sendTelegramState) {
        when (val state = sendTelegramState) {
            is SettingsViewModel.SendTelegramState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.send_success),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                settingsViewModel.resetSendTelegramState()
            }
            is SettingsViewModel.SendTelegramState.NeedChatId -> {
                showTelegramIdDialog = true
                settingsViewModel.resetSendTelegramState()
            }
            is SettingsViewModel.SendTelegramState.Error -> {
                val message = when (state.message) {
                    SettingsViewModel.ERROR_NO_TOKEN -> context.getString(R.string.settings_telegram_token_missing)
                    else -> context.getString(R.string.send_error, state.message)
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                settingsViewModel.resetSendTelegramState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { DarkGlassScreenTitle(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
        ) {
            ThemeSettingsSection(selected = themeMode)
            LanguageSettingsSection(selected = appLanguage)
            ParserSettings()

            var soundEnabled by remember { mutableStateOf(settingsViewModel.isSoundEnabled()) }
            var vibrationEnabled by remember { mutableStateOf(settingsViewModel.isVibrationEnabled()) }
            BentoGlassSection(
                title = stringResource(R.string.settings_sound_title),
                subtitle = stringResource(R.string.settings_sound_desc),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.settings_sound_title),
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(stringResource(R.string.settings_sound_title), color = tc.TextPrimary)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            settingsViewModel.setSoundEnabled(it)
                        },
                        colors = AppSwitchDefaults.colors(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = stringResource(R.string.settings_vibration_title),
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(stringResource(R.string.settings_vibration_title), color = tc.TextPrimary)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            settingsViewModel.setVibrationEnabled(it)
                        },
                        colors = AppSwitchDefaults.colors(),
                    )
                }
            }

            BentoGlassSection(title = stringResource(R.string.settings_rpm_thresholds_title)) {
                        Text(
                            text = stringResource(R.string.settings_rpm_thresholds_desc),
                            style = AppTypography.Caption,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                RpmColorLegend(
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                val fieldColors = AppTextFieldDefaults.outlined()
                OutlinedTextField(
                    value = minInput,
                    onValueChange = { minInput = it; error = null },
                    label = { Text(stringResource(R.string.settings_red_threshold_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it; error = null },
                    label = { Text(stringResource(R.string.settings_green_threshold_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors
                )
                error?.let { err ->
                    Text(
                        text = err,
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(stringResource(R.string.settings_rpm_save))
                }
            }

            BentoGlassSection(title = stringResource(R.string.settings_section_tools)) {
                OutlinedButton(
                    onClick = onTaxTracker,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Receipt, contentDescription = stringResource(R.string.tax_title))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.tax_title))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddPaycheck,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.add_paycheck_title))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddDiesel,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.add_diesel_title))
                }
            }

            BentoGlassSection(
                title = stringResource(R.string.settings_telegram_title),
                subtitle = stringResource(R.string.settings_telegram_desc)
            ) {
                TelegramBotSettingsContent(context = context, tc = tc)
            }

            BentoGlassSection(
                title = stringResource(R.string.settings_battery_title),
                subtitle = if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                    stringResource(R.string.settings_battery_ok)
                } else {
                    stringResource(R.string.settings_battery_desc)
                }
            ) {
                BatteryOptimizationContent(context = context, tc = tc)
            }

            BentoGlassSection(
                title = stringResource(R.string.export_loads),
                subtitle = stringResource(R.string.export_loads_description)
            ) {
                val fieldColors = AppTextFieldDefaults.outlined()
                OutlinedTextField(
                    value = telegramIdInput,
                    onValueChange = { telegramIdInput = it },
                    label = { Text(stringResource(R.string.enter_telegram_id)) },
                    placeholder = { Text(stringResource(R.string.telegram_id_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedButton(
                    onClick = {
                        settingsViewModel.saveTelegramChatId(telegramIdInput)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.telegram_id_saved),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.save_telegram_id))
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (exportState is SettingsViewModel.ExportState.Loading ||
                    restoreState is SettingsViewModel.RestoreState.Loading ||
                    sendTelegramState is SettingsViewModel.SendTelegramState.Loading
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    val progressText = when {
                        exportState is SettingsViewModel.ExportState.Loading ->
                            stringResource(R.string.exporting_loads)
                        restoreState is SettingsViewModel.RestoreState.Loading ->
                            stringResource(R.string.restoring_loads)
                        else -> stringResource(R.string.sending_to_telegram)
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        Text("📦")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_loads))
                    }
                }
                Button(
                    onClick = { settingsViewModel.exportCsv() },
                    enabled = exportState !is SettingsViewModel.ExportState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("📊")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_csv))
                    }
                }
                OutlinedButton(
                    onClick = { loadRestoreLauncher.launch(arrayOf("text/plain")) },
                    enabled = restoreState !is SettingsViewModel.RestoreState.Loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("📥")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.restore_loads))
                    }
                }
                Text(
                    text = stringResource(R.string.restore_loads_description),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            BentoGlassSection(
                title = stringResource(R.string.settings_backup_title),
                subtitle = stringResource(R.string.settings_backup_desc)
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
                                if (it.visibleText.isNotBlank()) {
                                    BackupService.shareNoteText(context, it.visibleText)
                                }
                            } ?: android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_error), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Backup, contentDescription = stringResource(R.string.settings_backup_create))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_backup_create))
                    }
                }
                OutlinedButton(
                    onClick = { restoreLauncher.launch("text/*") },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.settings_backup_restore))
                }
                Text(
                    text = stringResource(R.string.settings_backup_restore_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            GoogleDriveSyncSection(tc = tc)

            if (!BuildConfig.LOCAL_ONLY_MODE) {
                BentoGlassSection(
                    title = stringResource(R.string.settings_logout_title),
                    subtitle = stringResource(R.string.settings_logout_desc)
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
    if (showExportActions && exportedFile != null) {
        val file = exportedFile!!
        AlertDialog(
            onDismissRequest = {
                showExportActions = false
                exportedFile = null
            },
            title = { Text(stringResource(R.string.export_actions_title), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.export_loads_success, file.name), color = tc.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingTelegramFile = file
                        settingsViewModel.sendExportToTelegram(file)
                        showExportActions = false
                    }
                ) {
                    Text(stringResource(R.string.send_to_telegram))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        settingsViewModel.openExportsFolder(file)
                        showExportActions = false
                    }
                ) {
                    Text(stringResource(R.string.open_folder))
                }
            }
        )
    }

    if (showTelegramIdDialog) {
        AlertDialog(
            onDismissRequest = { showTelegramIdDialog = false },
            title = { Text(stringResource(R.string.enter_telegram_id), color = tc.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = telegramIdInput,
                    onValueChange = { telegramIdInput = it },
                    placeholder = { Text(stringResource(R.string.telegram_id_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.saveTelegramChatId(telegramIdInput)
                        showTelegramIdDialog = false
                        pendingTelegramFile?.let { settingsViewModel.sendExportToTelegram(it) }
                        pendingTelegramFile = null
                    }
                ) {
                    Text(stringResource(R.string.save_telegram_id))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTelegramIdDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showLogoutConfirm && !BuildConfig.LOCAL_ONLY_MODE) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.settings_logout_confirm_message), color = tc.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Stop Telegram first so it cannot write into a closed Room pool.
                            com.truckerload.sync.TelegramBotForegroundService.stopForLogout(context)
                            kotlinx.coroutines.delay(300)
                            com.truckerload.data.local.AppDatabase.closeCurrent()
                            userProfileStore.unbind()
                            authStore.logout()
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

@Composable
private fun TelegramBotSettingsContent(
    context: android.content.Context,
    tc: TruckColorPalette
) {
    val tokenStore = remember { TelegramTokenStore(context) }
    val storedToken = remember { tokenStore.getToken() }
    var tokenInput by remember { mutableStateOf(storedToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = tokenInput,
        onValueChange = { tokenInput = it; statusText = null },
        label = { Text(stringResource(R.string.settings_telegram_token_label)) },
        placeholder = { Text(stringResource(R.string.settings_telegram_token_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                Icon(
                    imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (tokenVisible) R.string.settings_telegram_hide_token else R.string.settings_telegram_show_token,
                    ),
                )
            }
        },
        colors = AppTextFieldDefaults.outlined(),
    )
    statusText?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = if (it.startsWith("✅")) tc.AccentProfit else tc.AccentExpense,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                tokenStore.setToken(tokenInput.trim())
                TelegramBotForegroundService.start(context)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_telegram_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.weight(1f).height(48.dp),
            enabled = tokenInput.isNotBlank()
        ) {
            Text(stringResource(R.string.common_save))
        }
        OutlinedButton(
            onClick = {
                isChecking = true
                statusText = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        TelegramBotHealth.check(tokenInput.trim())
                    }
                    statusText = when {
                        result.ok -> context.getString(
                            R.string.settings_telegram_ok,
                            result.username.orEmpty()
                        )
                        result.isUnauthorized -> context.getString(R.string.settings_telegram_invalid)
                        else -> "❌ ${result.error.orEmpty()}"
                    }
                    isChecking = false
                }
            },
            modifier = Modifier.weight(1f).height(48.dp),
            enabled = tokenInput.isNotBlank() && !isChecking
        ) {
            Text(if (isChecking) stringResource(R.string.settings_telegram_checking) else stringResource(R.string.settings_telegram_test))
        }
    }
}

@Composable
private fun BatteryOptimizationContent(
    context: android.content.Context,
    tc: TruckColorPalette
) {
    var ignoring by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    if (!ignoring) {
        Button(
            onClick = {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                ignoring = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.settings_battery_button))
        }
    } else {
        Text(
            text = stringResource(R.string.settings_battery_ok),
            style = MaterialTheme.typography.bodySmall,
            color = tc.AccentProfit
        )
    }
}

@Composable
private fun GoogleDriveSyncSection(tc: TruckColorPalette) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { GoogleDriveBackupService.prefs(context) }
    var linkedEmail by remember {
        mutableStateOf(
            prefs.accountEmail ?: run {
                GoogleDriveBackupService.syncLinkedAccountFromGoogle(context)
                prefs.accountEmail
            }
        )
    }
    var autoSync by remember { mutableStateOf(prefs.autoSyncEnabled) }
    var lastSyncAt by remember { mutableStateOf(prefs.lastSyncAt) }
    var busy by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
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
                stringResource(R.string.drive_sync_status_on, linkedEmail!!)
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
                    driveSignInLauncher.launch(GoogleDriveBackupService.signInIntent(context))
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
                onClick = { showRestoreConfirm = true },
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
            text = { Text(stringResource(R.string.drive_sync_restore_confirm_body)) },
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
