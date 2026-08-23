package com.truckerload.presentation.screens.scanner

import com.truckerload.presentation.icons.AppIcons

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PDFGenerator
import com.truckerload.utils.ShareHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ScanListRow(
    val scan: ScanEntity,
    val tripId: String,
    val dateLabel: String,
    val routeLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanGalleryScreen(
    onBack: () -> Unit,
) {
    val scanRepository = LocalScanRepository.current
    val loadRepository = LocalLoadRepository.current
    val scans by scanRepository.watchScans().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<ScanListRow>>(emptyList()) }
    var selected by remember { mutableStateOf<ScanListRow?>(null) }
    var scanToDelete by remember { mutableStateOf<ScanEntity?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val scansDir = File(context.getExternalFilesDir(null), "scans")
            runCatching { scanRepository.cleanupOrphanScanFiles(scansDir) }
        }
    }

    LaunchedEffect(scans) {
        rows = withContext(Dispatchers.IO) {
            scans.map { scan ->
                val load = scan.loadId?.takeIf { it.isNotBlank() }?.let { loadRepository.getLoadById(it) }
                ScanListRow(
                    scan = scan,
                    tripId = resolveTripId(scan, load),
                    dateLabel = formatScanDate(scan.timestamp),
                    routeLabel = resolveRoute(scan, load),
                )
            }
        }
    }

    SoftAppPageScaffold(
        title = stringResource(R.string.scans_gallery),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rows, key = { it.scan.id }) { row ->
                    ScanSummaryCard(
                        row = row,
                        onClick = { selected = row },
                    )
                }
            }
        }
    }

    selected?.let { row ->
        ScanDetailDialog(
            row = row,
            onDismiss = { selected = null },
            onOpen = {
                openPdf(context, row.scan)
            },
            onShare = {
                val file = File(row.scan.filePath)
                if (file.exists()) ShareHelper(context).sharePdf(file)
            },
            onDelete = {
                selected = null
                scanToDelete = row.scan
            },
        )
    }

    scanToDelete?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            title = { Text(stringResource(R.string.common_delete), color = tc.TextPrimary) },
            text = { Text(stringResource(R.string.scan_delete_confirm), color = tc.TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            File(scan.filePath).delete()
                            scanRepository.deleteScan(scan.id)
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
private fun ScanSummaryCard(
    row: ScanListRow,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.Description,
                contentDescription = null,
                tint = tc.AccentPrimary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.tripId,
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    maxLines = 1,
                )
                Text(
                    text = row.routeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = tc.TextSecondary,
            )
        }
    }
}

@Composable
private fun ScanDetailDialog(
    row: ScanListRow,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val scan = row.scan
    val fileExists = remember(scan.filePath) { File(scan.filePath).exists() }
    val detailRows = remember(scan, row) {
        buildList {
            add(R.string.scan_detail_trip to row.tripId)
            add(R.string.scan_detail_date to row.dateLabel)
            add(R.string.scan_detail_route to row.routeLabel)
            add(R.string.scan_detail_pages to scan.pageCount.toString())
            add(R.string.scan_detail_size to PDFGenerator.formatFileSize(scan.fileSizeBytes))
            add(R.string.scan_detail_file to scan.fileName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.scan_detail_title),
                color = tc.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detailRows.forEach { (labelRes, value) ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = tc.TextSecondary,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextPrimary,
                        )
                    }
                }
                if (!fileExists) {
                    Text(
                        text = stringResource(R.string.scan_file_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (scan.ocrText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.scan_detail_ocr),
                        style = MaterialTheme.typography.labelMedium,
                        color = tc.TextSecondary,
                    )
                    scan.ocrText
                        .lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .take(24)
                        .forEach { line ->
                            Text(
                                text = "• $line",
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextPrimary,
                            )
                        }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onOpen,
                    enabled = fileExists,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.scan_open),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onShare,
                    enabled = fileExists,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.send_to),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        maxLines = 1,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

private fun resolveTripId(scan: ScanEntity, load: Load?): String {
    load?.tripId?.takeIf { it.isNotBlank() }?.let { return it }
    val fromName = scan.fileName.substringBefore('_').substringBefore('.')
    if (fromName.isNotBlank() && fromName != scan.fileName) return fromName
    return scan.fileName
}

private fun resolveRoute(scan: ScanEntity, load: Load?): String {
    load?.let {
        val route = it.route.ifBlank {
            listOf(it.pointA, it.pointB).filter { p -> p.isNotBlank() }.joinToString(" → ")
        }
        if (route.isNotBlank()) return route
    }
    return extractRouteFromOcr(scan.ocrText)
        ?: "—"
}

private fun extractRouteFromOcr(ocr: String): String? {
    if (ocr.isBlank()) return null
    val lines = ocr.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    val shipper = lines.firstOrNull { it.startsWith("Shipper:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
    val consignee = lines.firstOrNull {
        it.startsWith("Consignee:", ignoreCase = true) ||
            it.startsWith("Receiver:", ignoreCase = true) ||
            it.startsWith("Delivery:", ignoreCase = true)
    }?.substringAfter(':')?.trim()
    return when {
        !shipper.isNullOrBlank() && !consignee.isNullOrBlank() -> "$shipper → $consignee"
        !shipper.isNullOrBlank() -> shipper
        else -> null
    }
}

private fun formatScanDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
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
