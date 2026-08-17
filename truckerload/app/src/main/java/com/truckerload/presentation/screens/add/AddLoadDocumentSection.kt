package com.truckerload.presentation.screens.add

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File

@Composable
fun AddLoadDocumentSection(
    extractedText: String,
    documentName: String?,
    isExtracting: Boolean,
    onDocumentPicked: (Uri, String?) -> Unit,
    onExtractedTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            onDocumentPicked(uri, "image/jpeg")
        } else {
            pendingCameraFile?.delete()
        }
        pendingCameraFile = null
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)
            onDocumentPicked(uri, mime)
        }
    }

    fun launchCamera() {
        val file = File.createTempFile("load_photo_", ".jpg", context.cacheDir)
        pendingCameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera()
    }

    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.add_load_document_hint),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    enabled = !isExtracting,
                ) {
                    Text(stringResource(R.string.add_load_take_photo))
                }
                OutlinedButton(
                    onClick = { fileLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    enabled = !isExtracting,
                ) {
                    Text(stringResource(R.string.add_load_choose_file))
                }
            }
            documentName?.let { name ->
                Text(
                    text = stringResource(R.string.add_load_document_name, name),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextPrimary,
                )
            }
            if (isExtracting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                        color = tc.AccentPrimary,
                    )
                    Text(
                        stringResource(R.string.add_load_document_reading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                    )
                }
            }
            if (extractedText.isNotBlank() && !isExtracting) {
                OutlinedTextField(
                    value = extractedText,
                    onValueChange = onExtractedTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    label = { Text(stringResource(R.string.add_load_document_text_label)) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(BentoGlassTheme.CellRadius),
                    colors = AppTextFieldDefaults.outlined(),
                )
            }
        }
    }
}
