package com.truckerload.presentation.screens.expenses

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.MiscExpenseReceiptStore
import com.truckerload.utils.ShareHelper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Attach a parking / scales / wash receipt photo — no OCR, just a file to send to the company.
 */
@Composable
fun MiscExpenseReceiptAttachSection(
    receiptPhotoPath: String?,
    initialReceiptPhotoPath: String?,
    enabled: Boolean,
    onReceiptPathChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun discardUnsaved(path: String?) {
        if (!path.isNullOrBlank() && path != initialReceiptPhotoPath) {
            MiscExpenseReceiptStore.deleteIfManaged(context, path)
        }
    }

    fun persistUri(uri: Uri) {
        scope.launch {
            busy = true
            val previousUnsaved = receiptPhotoPath
            val path = withContext(Dispatchers.IO) {
                val next = MiscExpenseReceiptStore.persistFromUri(
                    context = context,
                    source = uri,
                    previousPath = null,
                )
                if (next != null) discardUnsaved(previousUnsaved)
                next
            }
            busy = false
            if (path != null) onReceiptPathChange(path)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        pendingCameraUri = null
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null && file.isFile && file.length() > 0L) {
            discardUnsaved(receiptPhotoPath)
            onReceiptPathChange(file.absolutePath)
        } else {
            file?.delete()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) persistUri(uri) }

    fun launchCamera() {
        val file = MiscExpenseReceiptStore.createCameraCaptureFile(context)
        pendingCameraFile = file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchCamera() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.misc_expense_receipt_hint),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) launchCamera() else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = enabled && !busy,
            ) {
                Icon(AppIcons.PhotoCamera, contentDescription = null)
                Text(stringResource(R.string.misc_expense_receipt_camera), maxLines = 1)
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = enabled && !busy,
            ) {
                Icon(AppIcons.PhotoLibrary, contentDescription = null)
                Text(stringResource(R.string.misc_expense_receipt_gallery), maxLines = 1)
            }
        }

        val path = receiptPhotoPath
        if (!path.isNullOrBlank() && File(path).isFile) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.misc_expense_receipt_attached),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.misc_expense_receipt_attached),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.Success,
                )
                Row {
                    TlTextButton(
                        onClick = {
                            ShareHelper(context).sharePhoto(File(path))
                        },
                        enabled = enabled && !busy,
                    ) {
                        Text(stringResource(R.string.misc_expense_receipt_share))
                    }
                    TlTextButton(
                        onClick = {
                            discardUnsaved(path)
                            onReceiptPathChange(null)
                        },
                        enabled = enabled && !busy,
                    ) {
                        Text(
                            text = stringResource(R.string.misc_expense_receipt_remove),
                            color = tc.AccentExpense,
                        )
                    }
                }
            }
        }
    }
}
