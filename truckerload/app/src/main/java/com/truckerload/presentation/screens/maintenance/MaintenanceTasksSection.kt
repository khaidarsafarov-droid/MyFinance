package com.truckerload.presentation.screens.maintenance

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
fun MaintenanceTasksSection(
    activeProgress: List<MaintenanceProgress>,
    completedTasks: List<MaintenanceTask>,
    onAddTask: () -> Unit,
    onCompleteTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
) {
    val tc = LocalTruckColors.current

    SectionHeader(
        title = stringResource(R.string.maintenance_active_section),
        onAdd = onAddTask,
    )
    if (activeProgress.isEmpty()) {
        EmptyHint(stringResource(R.string.maintenance_empty_tasks))
    } else {
        activeProgress.forEach { progress ->
            ActiveTaskCard(
                progress = progress,
                onComplete = { onCompleteTask(progress.task.id) },
                onDelete = { onDeleteTask(progress.task.id) },
            )
        }
    }

    if (completedTasks.isNotEmpty()) {
        Text(
            text = stringResource(R.string.maintenance_completed_section),
            style = MaterialTheme.typography.titleMedium,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        completedTasks.take(10).forEach { task ->
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.CheckCircle, contentDescription = null, tint = tc.AccentPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, color = tc.TextPrimary)
                        Text(task.startDate, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                    }
                    IconButton(onClick = { onDeleteTask(task.id) }) {
                        Icon(AppIcons.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    onAdd: () -> Unit,
    addIcon: androidx.compose.ui.graphics.vector.ImageVector = AppIcons.Add,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = tc.TextPrimary)
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        ) {
            Icon(addIcon, contentDescription = title, tint = tc.AccentPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
internal fun EmptyHint(text: String) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = tc.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActiveTaskCard(
    progress: MaintenanceProgress,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val task = progress.task
    val urgent = progress.isDue && task.reminderType == MaintenanceReminderType.MILES
    BentoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (urgent) tc.AccentExpense.copy(alpha = 0.55f) else null,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.Build,
                    contentDescription = null,
                    tint = if (urgent) tc.AccentExpense else tc.AccentPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tc.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (progress.isDue) {
                    Text(
                        text = stringResource(
                            if (urgent) R.string.maintenance_urgent_badge else R.string.maintenance_due_badge,
                        ),
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Text(
                text = stringResource(R.string.maintenance_service_date, task.startDate),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            when (task.reminderType) {
                MaintenanceReminderType.MILES -> {
                    val driven = String.format(Locale.US, "%,.0f", progress.milesDrivenSinceStart)
                    val remaining = String.format(Locale.US, "%,.0f", progress.milesRemaining ?: 0.0)
                    val estimated = String.format(Locale.US, "%,.0f", progress.estimatedOdometer ?: 0.0)
                    val target = String.format(Locale.US, "%,.0f", progress.targetOdometer ?: 0.0)
                    Text(
                        stringResource(
                            R.string.maintenance_miles_progress,
                            driven,
                            remaining,
                            progress.loadsCounted,
                        ),
                        color = if (urgent) tc.AccentExpense else tc.TextPrimary,
                    )
                    if (urgent) {
                        Text(
                            stringResource(R.string.maintenance_urgent_message),
                            color = tc.AccentExpense,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Text(
                        stringResource(R.string.maintenance_odometer_estimate, estimated, target),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (urgent) tc.AccentExpense else tc.AccentPrimary,
                    )
                }
                MaintenanceReminderType.DATE -> {
                    val days = progress.daysRemaining
                    Text(
                        text = when {
                            days == null -> task.dueDate.orEmpty()
                            days < 0 -> stringResource(R.string.maintenance_overdue_days, -days)
                            days == 0L -> stringResource(R.string.maintenance_due_today)
                            else -> stringResource(R.string.maintenance_days_left, days)
                        },
                        color = if (progress.isDue) tc.AccentExpense else tc.TextPrimary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onComplete, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.maintenance_mark_done))
                }
                IconButton(onClick = onDelete) {
                    Icon(AppIcons.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
                }
            }
        }
    }
}
