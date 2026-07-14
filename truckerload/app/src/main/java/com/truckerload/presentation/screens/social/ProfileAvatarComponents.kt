package com.truckerload.presentation.screens.social

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.io.File

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    isUploading: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val model = remember(avatarUrl) { resolveAvatarModel(avatarUrl) }

    Box(
        modifier = modifier
            .size(UiDimens.AvatarProfile)
            .clip(CircleShape)
            .background(tc.AccentPrimary.copy(alpha = 0.25f))
            .then(
                if (onClick != null && !isUploading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text("👤", style = MaterialTheme.typography.headlineMedium)
        }

        if (onClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(tc.AccentPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.profile_change_photo),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = tc.AccentPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAvatarPickerSheet(
    visible: Boolean,
    hasAvatar: Boolean,
    onDismiss: () -> Unit,
    onBitmapSelected: (Bitmap) -> Unit,
    onRemove: () -> Unit,
) {
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }

    cropSource?.let { bitmap ->
        AvatarCropScreen(
            source = bitmap,
            onConfirm = { cropped ->
                cropSource = null
                onBitmapSelected(cropped)
                onDismiss()
            },
            onCancel = { cropSource = null },
        )
        return
    }

    if (!visible) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = AvatarCropUtils.decodeSampledBitmap(stream) ?: return@rememberLauncherForActivityResult
            cropSource = AvatarCropUtils.prepareBitmapForCrop(bitmap)
            onDismiss()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            cropSource = it
            onDismiss()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_change_photo),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_photo_from_gallery))
            }
            Button(
                onClick = { launchCamera() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(stringResource(R.string.profile_photo_take_selfie))
            }
            if (hasAvatar) {
                Button(
                    onClick = {
                        onRemove()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(stringResource(R.string.profile_photo_remove))
                }
            }
        }
    }
}

private fun resolveAvatarModel(avatarUrl: String?): Any? {
    if (avatarUrl.isNullOrBlank()) return null
    return if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
        avatarUrl
    } else {
        File(avatarUrl)
    }
}
