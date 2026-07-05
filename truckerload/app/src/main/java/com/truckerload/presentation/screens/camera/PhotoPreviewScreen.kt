package com.truckerload.presentation.screens.camera

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PhotoManager
import com.truckerload.utils.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    photo: CapturedPhoto,
    onCancel: () -> Unit,
    onRetake: () -> Unit,
    onSaved: () -> Unit,
    onSave: () -> Unit,
    onOpenGallery: () -> Unit = {},
    saveSuccess: Boolean,
    saveError: Boolean = false,
    onSaveErrorShown: () -> Unit = {},
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val bitmap = remember(photo.file.absolutePath) {
        BitmapFactory.decodeFile(photo.file.absolutePath)?.asImageBitmap()
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.photo_saved))
            onSaved()
        }
    }

    LaunchedEffect(saveError) {
        if (saveError) {
            snackbarHostState.showSnackbar(context.getString(R.string.photo_save_error))
            onSaveErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.camera),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Text(
                text = "📍 ${photo.locationData.cityStateLine}",
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            if (photo.locationData.zipCode.isNotBlank()) {
                Text(
                    text = photo.locationData.zipCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            }
            Text(
                text = "📅 ${PhotoManager.formatDateTime(photo.timestamp)}",
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = "📌 ${photo.locationData.coordinatesLine}",
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(stringResource(R.string.retake))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tc.AccentProfit,
                    ),
                ) {
                    Text(stringResource(R.string.common_save))
                }
                Button(
                    onClick = { ShareHelper(context).sharePhoto(photo.file) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tc.AccentPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.share))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.photo_gallery))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}

@Composable
fun CameraFlowScreen(
    onFinished: () -> Unit,
    onOpenGallery: () -> Unit = {},
    viewModel: CameraViewModel = viewModel(
        factory = CameraViewModel.Factory(
            LocalContext.current,
            LocalPhotoRepository.current,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val photo = uiState.capturedPhoto

    if (photo == null) {
        CameraScreen(
            onPhotoReady = { },
            onBack = onFinished,
            viewModel = viewModel,
        )
    } else {
        PhotoPreviewScreen(
            photo = photo,
            onCancel = {
                viewModel.discardCapturedPhoto()
                onFinished()
            },
            onRetake = {
                viewModel.discardCapturedPhoto()
            },
            onSave = viewModel::persistCapturedPhoto,
            onOpenGallery = onOpenGallery,
            saveSuccess = uiState.saveSuccess,
            saveError = uiState.errorMessage == "save_failed",
            onSaveErrorShown = viewModel::clearError,
            onSaved = onFinished,
        )
    }
}
