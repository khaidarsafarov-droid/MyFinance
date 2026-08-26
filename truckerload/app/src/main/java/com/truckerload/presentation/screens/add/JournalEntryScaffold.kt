package com.truckerload.presentation.screens.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.OneUiBottomActionBar
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScaffold(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    errorMessage: String?,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tc = LocalTruckColors.current
    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(title, color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, enabled = saveEnabled) {
                            Icon(
                                AppIcons.Delete,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = tc.Danger,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
        bottomBar = {
            OneUiBottomActionBar {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = saveEnabled,
                ) {
                    Text(stringResource(R.string.common_save))
                }
                errorMessage?.let {
                    Text(it, color = tc.Danger, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalContentScroll()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun JournalWeekSelectorRow(
    weekNumber: Int,
    weekLabel: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Text(stringResource(R.string.add_select_week), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(AppIcons.ChevronLeft, contentDescription = null, tint = tc.TextPrimary)
        }
        Text(
            stringResource(R.string.add_or_edit_week_format, weekNumber, weekLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNextWeek) {
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = tc.TextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDatePickerDialog(
    recordedAtMillis: Long,
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    openTimePickerAfterConfirm: Boolean = false,
    onOpenTimePicker: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val cal = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        yearRange = IntRange(cal.get(Calendar.YEAR) - 2, cal.get(Calendar.YEAR) + 1),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = tc.CardBackground,
        ),
        confirmButton = {
            TextButton(onClick = {
                dateState.selectedDateMillis?.let(onConfirm)
                onDismiss()
                if (openTimePickerAfterConfirm) onOpenTimePicker()
            }) { Text(stringResource(R.string.common_ok)) }
        },
    ) { DatePicker(state = dateState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalTimePickerDialog(
    recordedAtMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val tc = LocalTruckColors.current
    val cal = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
    val timeState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timeState.hour, timeState.minute)
                onDismiss()
            }) { Text(stringResource(R.string.common_ok)) }
        },
        text = { TimePicker(state = timeState) },
    )
}

@Composable
fun JournalSaveConfirmDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    onEditDateTime: () -> Unit,
    dateTimeLabel: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextPrimary,
        title = { Text(stringResource(R.string.common_save)) },
        text = {
            Column(modifier = Modifier.dialogBodyScroll()) {
                Text(stringResource(R.string.common_date_time), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(onClick = onEditDateTime),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dateTimeLabel, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onEditDateTime) {
                        Icon(AppIcons.Edit, contentDescription = stringResource(R.string.common_edit))
                    }
                }
                body()
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.height(48.dp),
                enabled = saveEnabled,
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
