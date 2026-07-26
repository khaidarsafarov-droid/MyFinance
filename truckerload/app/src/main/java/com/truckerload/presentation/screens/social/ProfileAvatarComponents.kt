package com.truckerload.presentation.screens.social

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Person
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
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
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
    val context = LocalContext.current
    val model = remember(avatarUrl) { resolveAvatarModel(avatarUrl) }
    val imageRequest = remember(model) {
        model?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }

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
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = tc.AccentPrimary,
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    val context = LocalContext.current
    // Hoisted above sheet visibility so dismissing the sheet for gallery/camera does not drop the image.
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }
    var awaitingExternalPicker by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun beginCrop(bitmap: Bitmap?) {
        awaitingExternalPicker = false
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            Toast.makeText(context, context.getString(R.string.profile_photo_load_failed), Toast.LENGTH_SHORT).show()
            return
        }
        cropSource = AvatarCropUtils.prepareBitmapForCrop(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            awaitingExternalPicker = false
            return@rememberLauncherForActivityResult
        }
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                AvatarCropUtils.decodeSampledBitmap(stream)
            }
        }.getOrNull()
        beginCrop(bitmap)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = captureFile
        captureFile = null
        if (!success || file == null || !file.exists()) {
            awaitingExternalPicker = false
            file?.delete()
            if (!success) return@rememberLauncherForActivityResult
            Toast.makeText(context, context.getString(R.string.profile_photo_load_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val bitmap = runCatching {
            file.inputStream().use { AvatarCropUtils.decodeSampledBitmap(it) }
        }.getOrNull()
        file.delete()
        beginCrop(bitmap)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            awaitingExternalPicker = false
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val file = File(context.cacheDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
        captureFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        awaitingExternalPicker = true
        cameraLauncher.launch(uri)
    }

    // Crop UI stays mounted even when the bottom sheet was dismissed for the system picker.
    cropSource?.let { bitmap ->
        AvatarCropScreen(
            source = bitmap,
            onConfirm = { cropped ->
                cropSource = null
                onBitmapSelected(cropped)
                onDismiss()
            },
            onCancel = {
                cropSource = null
                onDismiss()
            },
        )
        return
    }

    if (!visible) return

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val file = File(context.cacheDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
            captureFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            awaitingExternalPicker = true
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Don't clear picker state while the system gallery/camera is open.
            if (!awaitingExternalPicker) onDismiss()
        },
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
                onClick = {
                    awaitingExternalPicker = true
                    galleryLauncher.launch("image/*")
                },
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
    return if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") ||
        avatarUrl.startsWith("file://") || avatarUrl.startsWith("content://")
    ) {
        avatarUrl
    } else {
        File(avatarUrl).takeIf { it.exists() }
    }
}
