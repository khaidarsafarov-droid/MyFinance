package com.truckerload.presentation.screens.maintenance

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.theme.AppColors
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val repository = LocalMaintenanceRepository.current
    val viewModel: MaintenanceViewModel = viewModel(
        factory = MaintenanceViewModel.Factory(application, repository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            viewModel.processReceiptPhoto(uri, deleteAfterCopy = pendingCameraFile)
        } else {
            pendingCameraFile?.delete()
        }
        pendingCameraFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            viewModel.processReceiptPhoto(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val file = createTempReceiptFile(context.cacheDir)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        viewModel.dismissReceiptSourcePicker()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = createTempReceiptFile(context.cacheDir)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        viewModel.dismissReceiptSourcePicker()
        galleryLauncher.launch("image/*")
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.maintenance_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item(key = "active_header") {
                SectionHeader(
                    title = stringResource(R.string.maintenance_active_section),
                    onAdd = viewModel::openAddTask,
                )
            }
            if (uiState.activeProgress.isEmpty()) {
                item(key = "active_empty") {
                    EmptyHint(stringResource(R.string.maintenance_empty_tasks))
                }
            } else {
                items(
                    items = uiState.activeProgress,
                    key = { it.task.id },
                ) { progress ->
                    ActiveTaskCard(
                        progress = progress,
                        onComplete = { viewModel.completeTask(progress.task.id) },
                        onDelete = { viewModel.deleteTask(progress.task.id) },
                    )
                }
            }

            if (uiState.completedTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    Text(
                        text = stringResource(R.string.maintenance_completed_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(
                    items = uiState.completedTasks.take(10),
                    key = { "done_${it.id}" },
                ) { task ->
                    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = tc.AccentPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, color = tc.TextPrimary)
                                Text(task.startDate, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                            }
                            IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
                            }
                        }
                    }
                }
            }

            item(key = "archive_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.maintenance_archive_section),
                        onAdd = viewModel::openReceiptSourcePicker,
                        addIcon = Icons.Default.CameraAlt,
                    )
                    Text(
                        text = stringResource(R.string.maintenance_archive_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                    if (uiState.isProcessingPhoto) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = stringResource(R.string.maintenance_ocr_processing),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                        )
                    }
                    if (uiState.archive.isEmpty()) {
                        EmptyHint(stringResource(R.string.maintenance_empty_archive))
                    }
                }
            }
            if (uiState.archive.isNotEmpty()) {
                items(
                    items = uiState.archive,
                    key = { "archive_${it.id}" },
                ) { entry ->
                    ArchiveCard(
                        entry = entry,
                        onDelete = { viewModel.deleteArchive(entry.id) },
                        onOpenPhoto = { path -> viewModel.openReceiptViewer(path) },
                    )
                }
            }
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (uiState.showReceiptSourcePicker) {
        ReceiptSourceDialog(
            onDismiss = viewModel::dismissReceiptSourcePicker,
            onCamera = { launchCamera() },
            onGallery = { launchGallery() },
        )
    }

    if (uiState.showAddTask) {
        AddTaskDialog(
            draft = uiState.taskDraft,
            isSaving = uiState.isSaving,
            errorKey = uiState.errorMessage,
            onDismiss = viewModel::dismissAddTask,
            onChange = viewModel::updateTaskDraft,
            onSave = viewModel::saveTask,
        )
    }

    if (uiState.showAddArchive) {
        AddArchiveDialog(
            draft = uiState.archiveDraft,
            isSaving = uiState.isSaving,
            errorKey = uiState.errorMessage,
            onDismiss = viewModel::dismissAddArchive,
            onChange = viewModel::updateArchiveDraft,
            onSave = viewModel::saveArchive,
            onRetakePhoto = viewModel::openReceiptSourcePicker,
        )
    }

    uiState.viewingReceiptPath?.let { path ->
        ReceiptPhotoViewerDialog(
            path = path,
            onDismiss = viewModel::dismissReceiptViewer,
        )
    }
}

@Composable
private fun ReceiptSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = {
            Text(stringResource(R.string.maintenance_scan_source_title), color = tc.TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.maintenance_scan_source_hint),
                    color = tc.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.camera))
                }
                OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_photo_from_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun ReceiptPhotoViewerDialog(
    path: String,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.maintenance_receipt_photo),
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                    tint = tc.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onAdd: () -> Unit,
    addIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Add,
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
private fun EmptyHint(text: String) {
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
                    Icons.Default.Build,
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
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ArchiveCard(
    entry: MaintenanceArchiveEntry,
    onDelete: () -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val hasReceipt = !entry.photoPath.isNullOrBlank()
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasReceipt) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onOpenPhoto(entry.photoPath!!) },
                ) {
                    AsyncImage(
                        model = File(entry.photoPath),
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (entry.serviceName.isNotBlank()) {
                        Text(
                            entry.serviceName,
                            color = tc.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (hasReceipt) {
                        ReceiptAttachedBadge(onClick = { onOpenPhoto(entry.photoPath!!) })
                    }
                }
                Text(
                    entry.description,
                    color = if (entry.serviceName.isBlank()) tc.TextPrimary else tc.TextSecondary,
                    style = if (entry.serviceName.isBlank()) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                )
                Text(entry.serviceDate, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                Text(
                    "$${String.format(Locale.US, "%,.2f", entry.amount)}",
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (hasReceipt) {
                IconButton(onClick = { onOpenPhoto(entry.photoPath!!) }) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        tint = AppColors.RpmGreen,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
            }
        }
    }
}

@Composable
private fun ReceiptAttachedBadge(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AppColors.RpmGreen),
        )
        Text(
            text = stringResource(R.string.maintenance_has_receipt),
            color = AppColors.RpmGreen,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    draft: TaskDraft,
    isSaving: Boolean,
    errorKey: String?,
    onDismiss: () -> Unit,
    onChange: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showStartPicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    val fieldColors = AppTextFieldDefaults.outlined()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(stringResource(R.string.maintenance_add_task_title), color = tc.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { value -> onChange { it.copy(title = value) } },
                    label = { Text(stringResource(R.string.maintenance_field_title)) },
                    placeholder = { Text(stringResource(R.string.maintenance_field_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                Text(
                    text = stringResource(R.string.maintenance_field_date),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draft.startDate,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showStartPicker = true }) {
                        Text(stringResource(R.string.common_edit))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.reminderType == MaintenanceReminderType.MILES,
                        onClick = { onChange { it.copy(reminderType = MaintenanceReminderType.MILES) } },
                        label = { Text(stringResource(R.string.maintenance_by_miles)) },
                    )
                    FilterChip(
                        selected = draft.reminderType == MaintenanceReminderType.DATE,
                        onClick = { onChange { it.copy(reminderType = MaintenanceReminderType.DATE) } },
                        label = { Text(stringResource(R.string.maintenance_by_date)) },
                    )
                }
                if (draft.reminderType == MaintenanceReminderType.MILES) {
                    OutlinedTextField(
                        value = draft.intervalMiles,
                        onValueChange = { value -> onChange { it.copy(intervalMiles = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                        label = { Text(stringResource(R.string.maintenance_interval_miles)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.maintenance_due_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = tc.TextSecondary,
                            )
                            Text(draft.dueDate, color = tc.TextPrimary)
                        }
                        TextButton(onClick = { showDuePicker = true }) {
                            Text(stringResource(R.string.common_edit))
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.odometerAtStart,
                    onValueChange = { value -> onChange { it.copy(odometerAtStart = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                    label = { Text(stringResource(R.string.maintenance_current_odometer)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                errorKey?.let { key ->
                    Text(errorText(key), color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )

    if (showStartPicker) {
        IsoDatePickerDialog(
            initial = draft.startDate,
            onDismiss = { showStartPicker = false },
            onConfirm = { date ->
                onChange { it.copy(startDate = date) }
                showStartPicker = false
            },
        )
    }
    if (showDuePicker) {
        IsoDatePickerDialog(
            initial = draft.dueDate,
            onDismiss = { showDuePicker = false },
            onConfirm = { date ->
                onChange { it.copy(dueDate = date) }
                showDuePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddArchiveDialog(
    draft: ArchiveDraft,
    isSaving: Boolean,
    errorKey: String?,
    onDismiss: () -> Unit,
    onChange: ((ArchiveDraft) -> ArchiveDraft) -> Unit,
    onSave: () -> Unit,
    onRetakePhoto: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    val fieldColors = AppTextFieldDefaults.outlined()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tc.CardBackground,
        title = { Text(stringResource(R.string.maintenance_edit_receipt_title), color = tc.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.maintenance_edit_receipt_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                if (!draft.photoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(draft.photoPath),
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                    )
                }
                OutlinedButton(onClick = onRetakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.maintenance_retake_photo))
                }
                OutlinedTextField(
                    value = draft.serviceName,
                    onValueChange = { value -> onChange { it.copy(serviceName = value) } },
                    label = { Text(stringResource(R.string.maintenance_archive_service)) },
                    placeholder = { Text(stringResource(R.string.maintenance_archive_service_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    label = { Text(stringResource(R.string.maintenance_archive_what)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = draft.serviceDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.maintenance_archive_when)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(stringResource(R.string.common_edit))
                        }
                    },
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = { value -> onChange { it.copy(amount = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                    label = { Text(stringResource(R.string.maintenance_archive_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                errorKey?.let { key ->
                    Text(errorText(key), color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )

    if (showDatePicker) {
        IsoDatePickerDialog(
            initial = draft.serviceDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onChange { it.copy(serviceDate = date) }
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsoDatePickerDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val zone = ZoneId.systemDefault()
    val initialMillis = runCatching {
        LocalDate.parse(initial).atStartOfDay(zone).toInstant().toEpochMilli()
    }.getOrElse { System.currentTimeMillis() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: return@TextButton
                val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                onConfirm(date)
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = tc.CardBackground),
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun errorText(key: String): String = when (key) {
    "empty_title" -> stringResource(R.string.maintenance_error_empty_title)
    "invalid_interval" -> stringResource(R.string.maintenance_error_interval)
    "invalid_odometer" -> stringResource(R.string.maintenance_error_odometer)
    "invalid_due_date" -> stringResource(R.string.maintenance_error_due_date)
    "empty_description" -> stringResource(R.string.maintenance_error_description)
    "invalid_amount" -> stringResource(R.string.maintenance_error_amount)
    else -> stringResource(R.string.common_save_failed)
}

private fun createTempReceiptFile(cacheDir: File): File {
    val dir = File(cacheDir, "maintenance").apply { mkdirs() }
    return File(dir, "capture_${System.currentTimeMillis()}.jpg")
}
