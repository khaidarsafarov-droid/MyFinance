package com.truckerload.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val tc = LocalTruckColors.current
    val store = LocalRpmThresholdsStore.current
    val context = LocalContext.current
    val loadRepo = LocalLoadRepository.current
    val paycheckRepo = LocalPaycheckRepository.current
    val dieselRepo = LocalDieselRepository.current
    val scope = rememberCoroutineScope()
    var backupRestoreMessage by remember { mutableStateOf<String?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupService(context, loadRepo, paycheckRepo, dieselRepo).restoreFromUri(uri)
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

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
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
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_rpm_thresholds_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.settings_rpm_thresholds_desc),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = minInput,
                onValueChange = { minInput = it; error = null },
                label = { Text(stringResource(R.string.settings_red_threshold_label)) },
                supportingText = { Text(stringResource(R.string.settings_red_threshold_help)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tc.AccentPrimary,
                    unfocusedBorderColor = tc.Divider,
                    focusedLabelColor = tc.AccentPrimary,
                    unfocusedLabelColor = tc.TextSecondary,
                    focusedTextColor = tc.TextPrimary,
                    unfocusedTextColor = tc.TextPrimary,
                    focusedContainerColor = tc.CardBackground,
                    unfocusedContainerColor = tc.CardBackground,
                    focusedSupportingTextColor = tc.TextSecondary,
                    unfocusedSupportingTextColor = tc.TextSecondary,
                    cursorColor = tc.AccentPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it; error = null },
                label = { Text(stringResource(R.string.settings_green_threshold_label)) },
                supportingText = { Text(stringResource(R.string.settings_green_threshold_help)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tc.AccentPrimary,
                    unfocusedBorderColor = tc.Divider,
                    focusedLabelColor = tc.AccentPrimary,
                    unfocusedLabelColor = tc.TextSecondary,
                    focusedTextColor = tc.TextPrimary,
                    unfocusedTextColor = tc.TextPrimary,
                    focusedContainerColor = tc.CardBackground,
                    unfocusedContainerColor = tc.CardBackground,
                    focusedSupportingTextColor = tc.TextSecondary,
                    unfocusedSupportingTextColor = tc.TextSecondary,
                    cursorColor = tc.AccentPrimary
                )
            )
            error?.let { err ->
                Text(
                    text = err,
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
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
                                android.widget.Toast.makeText(context, context.getString(R.string.settings_saved), android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { error = it.message }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.common_save))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_backup_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.settings_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            BackupService(context, loadRepo, paycheckRepo, dieselRepo).createBackup()
                        }
                        result?.let {
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_saved, it.displayPath), android.widget.Toast.LENGTH_LONG).show()
                        } ?: android.widget.Toast.makeText(context, context.getString(R.string.settings_backup_error), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_backup_create))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_backup_restore))
                }
            }
        }
    }
}
