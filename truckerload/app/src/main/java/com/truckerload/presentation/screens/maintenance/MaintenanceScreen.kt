package com.truckerload.presentation.screens.maintenance

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.verticalContentScroll
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MaintenanceViewModel = hiltViewModel()
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

    SoftAppPageScaffold(
        title = stringResource(R.string.maintenance_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalContentScroll()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MaintenanceTasksSection(
                activeProgress = uiState.activeProgress,
                completedTasks = uiState.completedTasks,
                onAddTask = viewModel::openAddTask,
                onCompleteTask = viewModel::completeTask,
                onDeleteTask = viewModel::deleteTask,
            )
            MaintenanceArchiveSection(
                archive = uiState.archive,
                isProcessingPhoto = uiState.isProcessingPhoto,
                onAddArchive = viewModel::openReceiptSourcePicker,
                onDeleteArchive = viewModel::deleteArchive,
                onOpenPhoto = viewModel::openReceiptViewer,
            )
            Spacer(modifier = Modifier.height(24.dp))
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
