package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.truckerload.data.backup.BackupRestoreErrors
import com.truckerload.data.backup.BackupRestoreException
import com.truckerload.data.backup.DriveConnectOutcome
import com.truckerload.data.backup.DriveConnectPending
import com.truckerload.data.backup.DriveConnectInterpreter
import com.truckerload.data.backup.GoogleDriveBackupService
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.presentation.auth.GoogleSignInSupport
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.connectivity.ConnectivityObserver
import com.truckerload.presentation.connectivity.ConnectivityStatus
import com.truckerload.presentation.di.LocalAuthStore
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
    val authProvider = LocalAuthStore.current.authProvider()
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
    var lastSyncError by remember { mutableStateOf(prefs.lastSyncError) }
    var busy by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreConflict by remember { mutableStateOf(false) }
    var pendingConnect by remember { mutableStateOf(DriveConnectPending.None) }
    var tokenConsentTried by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val driveSignInLauncherRef = remember {
        arrayOfNulls<androidx.activity.result.ActivityResultLauncher<android.content.Intent>>(1)
    }

    fun refreshSyncStatus() {
        lastSyncAt = prefs.lastSyncAt
        lastSyncError = prefs.lastSyncError
        linkedEmail = GoogleDriveBackupService.linkedAccountEmail(context) ?: prefs.accountEmail
    }

    fun toastMessage(text: String, long: Boolean = true) {
        Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun launchGoogleIntent(intent: android.content.Intent) {
        mainHandler.post {
            val launcher = driveSignInLauncherRef[0]
            if (launcher == null) {
                pendingConnect = DriveConnectPending.None
                toastMessage(context.getString(R.string.drive_sync_need_activity))
                return@post
            }
            runCatching { launcher.launch(intent) }.onFailure {
                pendingConnect = DriveConnectPending.None
                toastMessage(context.getString(R.string.drive_sync_connect_failed))
            }
        }
    }

    fun toastResult(result: Result<String>) {
        result.fold(
            onSuccess = {
                tokenConsentTried = false
                toastMessage(it)
            },
            onFailure = { err ->
                val host = activity ?: context.findActivity()
                val consent = GoogleDriveBackupService.consentIntent(err)
                if (host != null && consent != null && !tokenConsentTried) {
                    tokenConsentTried = true
                    pendingConnect = DriveConnectPending.TokenConsent
                    launchGoogleIntent(consent)
                    return
                }
                toastMessage(
                    if (err is BackupRestoreException) {
                        BackupRestoreErrors.userMessage(context, err)
                    } else {
                        err.message ?: context.getString(R.string.drive_sync_api_error, "")
                    },
                )
            }
        )
    }

    suspend fun pushToDrive() {
        busy = true
        val result = withContext(Dispatchers.IO) {
            GoogleDriveBackupService.backupNow(context)
        }
        refreshSyncStatus()
        busy = false
        toastResult(result)
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val parsed = if (pendingConnect == DriveConnectPending.TokenConsent) {
            GoogleDriveBackupService.ParsedSignIn(null, false, null)
        } else {
            GoogleDriveBackupService.parseSignInIntent(result.data)
        }
        val outcome = DriveConnectInterpreter.next(
            resultCode = result.resultCode,
            accountEmail = parsed.email,
            grantedDriveScope = parsed.grantedDriveScope,
            error = parsed.error,
            pending = pendingConnect,
        )
        when (outcome) {
            DriveConnectOutcome.Granted -> {
                val email = parsed.email.orEmpty()
                GoogleDriveBackupService.linkAccountEmail(context, email)
                pendingConnect = DriveConnectPending.None
                linkedEmail = prefs.accountEmail
                toastMessage(context.getString(R.string.drive_sync_connected), long = false)
                scope.launch { pushToDrive() }
            }
            is DriveConnectOutcome.RequestDriveConsent -> {
                val host = activity ?: context.findActivity()
                if (host == null) {
                    pendingConnect = DriveConnectPending.None
                    toastMessage(context.getString(R.string.drive_sync_need_activity))
                } else {
                    pendingConnect = DriveConnectPending.DriveConsent
                    launchGoogleIntent(
                        GoogleSignInClients.driveConsentIntent(host, outcome.email),
                    )
                }
            }
            DriveConnectOutcome.RetryBackup -> {
                pendingConnect = DriveConnectPending.None
                scope.launch { pushToDrive() }
            }
            DriveConnectOutcome.Cancelled -> {
                pendingConnect = DriveConnectPending.None
                toastMessage(context.getString(R.string.drive_sync_connect_cancelled))
            }
            is DriveConnectOutcome.Failed -> {
                pendingConnect = DriveConnectPending.None
                toastMessage(
                    outcome.error?.let { GoogleSignInSupport.formatError(context, it) }
                        ?: context.getString(R.string.drive_sync_connect_failed),
                )
            }
        }
    }
    driveSignInLauncherRef[0] = driveSignInLauncher

    fun startDriveSync() {
        val host = activity ?: context.findActivity()
        if (host == null) {
            toastMessage(context.getString(R.string.drive_sync_need_activity))
            return
        }
        GoogleDriveBackupService.syncLinkedAccountFromGoogle(host)
        if (GoogleDriveBackupService.isDriveScopeGranted(host)) {
            linkedEmail = GoogleDriveBackupService.linkedAccountEmail(host) ?: prefs.accountEmail
            scope.launch { pushToDrive() }
            return
        }
        pendingConnect = if (GoogleDriveBackupService.connectStartsAtDriveConsent(host)) {
            DriveConnectPending.DriveConsent
        } else {
            DriveConnectPending.AccountPicker
        }
        runCatching {
            driveSignInLauncher.launch(GoogleDriveBackupService.signInIntent(host))
        }.onFailure {
            pendingConnect = DriveConnectPending.None
            toastMessage(context.getString(R.string.drive_sync_connect_failed))
        }
    }

    BentoGlassSection(
        title = stringResource(R.string.drive_sync_title),
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
                when {
                    !linkedEmail.isNullOrBlank() && authProvider == AuthProvider.GOOGLE ->
                        R.string.drive_sync_google_linked_hint
                    linkedEmail.isNullOrBlank() && authProvider == AuthProvider.GOOGLE ->
                        R.string.drive_sync_google_user_hint
                    linkedEmail.isNullOrBlank() && authProvider == AuthProvider.LOCAL ->
                        R.string.drive_sync_local_user_hint
                    linkedEmail.isNullOrBlank() -> R.string.drive_sync_email_user_hint
                    else -> R.string.drive_sync_email_linked_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Text(
            text = stringResource(R.string.drive_sync_desc),
            style = MaterialTheme.typography.labelSmall,
            color = tc.TextSecondary,
        )
        Text(
            text = stringResource(
                R.string.drive_sync_last,
                if (lastSyncAt > 0L) dateFormat.format(Date(lastSyncAt))
                else stringResource(R.string.drive_sync_last_never),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = tc.TextSecondary,
        )
        val syncError = lastSyncError
        if (!syncError.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.drive_sync_last_error, syncError),
                style = MaterialTheme.typography.labelSmall,
                color = tc.AccentExpense,
            )
        }
        if (connectivity == ConnectivityStatus.Offline) {
            Text(
                text = stringResource(R.string.connectivity_offline_banner),
                style = MaterialTheme.typography.labelSmall,
                color = tc.AccentExpense,
            )
        }

        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.drive_sync_busy),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
            )
        }

        if (linkedEmail.isNullOrBlank()) {
            Button(
                onClick = { startDriveSync() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val connectLabel = when {
                        authProvider == AuthProvider.LOCAL -> R.string.drive_sync_now
                        authProvider == AuthProvider.GOOGLE &&
                            GoogleDriveBackupService.isDriveScopeGranted(context) ->
                            R.string.drive_sync_now
                        else -> R.string.drive_sync_connect
                    }
                    Icon(AppIcons.Cloud, contentDescription = stringResource(connectLabel))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(connectLabel))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.drive_sync_auto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
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
                onClick = { startDriveSync() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(AppIcons.Cloud, contentDescription = stringResource(R.string.drive_sync_now))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.drive_sync_now))
                }
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
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                        lastSyncError = null
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(R.string.drive_sync_disconnect))
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) showRestoreConfirm = false },
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
                            refreshSyncStatus()
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
