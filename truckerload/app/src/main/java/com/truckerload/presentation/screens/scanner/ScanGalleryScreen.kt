package com.truckerload.presentation.screens.scanner

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PDFGenerator
import com.truckerload.utils.PhotoManager
import com.truckerload.utils.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanGalleryScreen(
    onBack: () -> Unit,
) {
    val repository = LocalScanRepository.current
    val scans by repository.watchScans().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    var scanToDelete by remember { mutableStateOf<ScanEntity?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val scansDir = File(context.getExternalFilesDir(null), "scans")
            runCatching { repository.cleanupOrphanScanFiles(scansDir) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scans_gallery)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (scans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_scans),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanListItem(
                        scan = scan,
                        onOpen = { openPdf(context, scan) },
                        onShare = {
                            val file = File(scan.filePath)
                            if (file.exists()) ShareHelper(context).sharePdf(file)
                        },
                        onDelete = { scanToDelete = scan },
                    )
                    HorizontalDivider(color = tc.Divider.copy(alpha = 0.3f))
                }
            }
        }
    }

    scanToDelete?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.scan_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            File(scan.filePath).delete()
                            repository.deleteScan(scan.id)
                            scanToDelete = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { scanToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ScanListItem(
    scan: ScanEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val fileExists = remember(scan.filePath) { File(scan.filePath).exists() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(text = scan.fileName, style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
        Text(
            text = stringResource(R.string.scan_pages, scan.pageCount),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Text(
            text = stringResource(R.string.scan_size, PDFGenerator.formatFileSize(scan.fileSizeBytes)),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Text(
            text = PhotoManager.formatDateTime(scan.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        if (!fileExists) {
            Text(
                text = stringResource(R.string.scan_file_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (scan.ocrText.isNotBlank()) {
            Text(
                text = scan.ocrText.take(120) + if (scan.ocrText.length > 120) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onOpen, enabled = fileExists, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_open))
            }
            OutlinedButton(onClick = onShare, enabled = fileExists, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.send_to))
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.common_delete))
            }
        }
    }
}

private fun openPdf(context: android.content.Context, scan: ScanEntity) {
    val file = File(scan.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.scan_open)))
}
