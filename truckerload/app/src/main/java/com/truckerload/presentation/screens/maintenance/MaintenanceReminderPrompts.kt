package com.truckerload.presentation.screens.maintenance

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truckerload.R
import com.truckerload.domain.maintenance.MaintenanceReminderAccess
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.ExactAlarmAccess
import com.truckerload.sync.MaintenanceAlarmStore
import com.truckerload.utils.BatteryOptimizationHelper
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun MaintenanceReminderHost(taskSaved: SharedFlow<Unit>) {
    val context = LocalContext.current
    val store = remember { MaintenanceAlarmStore(context) }
    var queue by remember { mutableStateOf<List<MaintenanceReminderAccess.Prompt>>(emptyList()) }
    var notificationsGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var exactAllowed by remember { mutableStateOf(ExactAlarmAccess.canScheduleExact(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = notificationsGranted(context)
                exactAllowed = ExactAlarmAccess.canScheduleExact(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted || notificationsGranted(context)
        queue = queue.drop(1)
    }
    LaunchedEffect(taskSaved) {
        taskSaved.collect {
            queue = MaintenanceReminderAccess.prompts(snapshot(context, store))
        }
    }
    val current = queue.firstOrNull()
    LaunchedEffect(current) {
        if (current == MaintenanceReminderAccess.Prompt.Notifications) {
            store.setFlag(MaintenanceAlarmStore.FLAG_NOTIF_ASKED)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            ReminderBanner(
                text = stringResource(R.string.maintenance_notif_permission_denied),
                action = stringResource(R.string.maintenance_notif_open_settings),
                onAction = { openNotificationSettings(context) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
            ReminderBanner(
                text = stringResource(R.string.maintenance_exact_alarm_banner),
                action = stringResource(R.string.maintenance_exact_alarm_settings),
                onAction = { ExactAlarmAccess.openSettings(context) },
            )
        }
    }
    when (current) {
        MaintenanceReminderAccess.Prompt.NotificationsDenied -> PermissionDialog(
            title = stringResource(R.string.maintenance_notif_permission_title),
            body = stringResource(R.string.maintenance_notif_permission_denied),
            confirm = stringResource(R.string.maintenance_notif_open_settings),
            onConfirm = {
                openNotificationSettings(context)
                queue = queue.drop(1)
            },
            onDismiss = { queue = queue.drop(1) },
        )
        MaintenanceReminderAccess.Prompt.ExactAlarm -> PermissionDialog(
            title = stringResource(R.string.maintenance_exact_alarm_title),
            body = stringResource(R.string.maintenance_exact_alarm_body),
            confirm = stringResource(R.string.maintenance_exact_alarm_settings),
            onConfirm = {
                store.setFlag(MaintenanceAlarmStore.FLAG_EXACT_PROMPT)
                ExactAlarmAccess.openSettings(context)
                queue = queue.drop(1)
            },
            onDismiss = {
                store.setFlag(MaintenanceAlarmStore.FLAG_EXACT_PROMPT)
                queue = queue.drop(1)
            },
        )
        MaintenanceReminderAccess.Prompt.Battery -> PermissionDialog(
            title = stringResource(R.string.maintenance_battery_title),
            body = stringResource(R.string.maintenance_battery_body),
            confirm = stringResource(R.string.maintenance_battery_allow),
            dismiss = stringResource(R.string.maintenance_battery_later),
            onConfirm = {
                store.setFlag(MaintenanceAlarmStore.FLAG_BATTERY_PROMPT)
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                queue = queue.drop(1)
            },
            onDismiss = {
                store.setFlag(MaintenanceAlarmStore.FLAG_BATTERY_PROMPT)
                queue = queue.drop(1)
            },
        )
        MaintenanceReminderAccess.Prompt.Notifications,
        null,
        -> Unit
    }
}

@Composable
private fun ReminderBanner(
    text: String,
    action: String,
    onAction: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = text, color = tc.TextPrimary)
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun PermissionDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = stringResource(R.string.maintenance_battery_later),
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(title, color = tc.TextPrimary) },
        text = { Text(body, color = tc.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismiss) }
        },
    )
}

private fun notificationsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun snapshot(
    context: Context,
    store: MaintenanceAlarmStore,
): MaintenanceReminderAccess.Snapshot {
    val activity = findActivity(context)
    val asked = store.isFlagSet(MaintenanceAlarmStore.FLAG_NOTIF_ASKED)
    val canRequest = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> true
        notificationsGranted(context) -> false
        !asked -> true
        else -> activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    }
    return MaintenanceReminderAccess.Snapshot(
        sdkInt = Build.VERSION.SDK_INT,
        notificationsGranted = notificationsGranted(context),
        canRequestNotifications = canRequest,
        canScheduleExactAlarms = ExactAlarmAccess.canScheduleExact(context),
        ignoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context),
        exactPromptShown = store.isFlagSet(MaintenanceAlarmStore.FLAG_EXACT_PROMPT),
        batteryPromptShown = store.isFlagSet(MaintenanceAlarmStore.FLAG_BATTERY_PROMPT),
    )
}

private fun findActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}
