package com.truckerload.presentation.screens.scanner

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.model.ScanDocumentFinder
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.appFormField
import com.truckerload.utils.ShareHelper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var selectedId by remember { mutableStateOf<String?>(null) }
    var scanToDelete by remember { mutableStateOf<ScanEntity?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<ScanDocumentCategory?>(null) }

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

    val categoryCounts = remember(rows) {
        rows.groupingBy { it.category }.eachCount()
    }
    val visibleRows = remember(rows, filter, query) {
        rows.filter { row ->
            ScanDocumentFinder.matches(
                storedCategory = row.scan.category,
                fileName = row.scan.fileName,
                ocrText = row.scan.ocrText,
                tripId = row.tripId,
                routeLabel = row.routeLabel,
                dateLabel = row.dateLabel,
                filter = filter,
                query = query,
            )
        }
    }
    val selected = selectedId?.let { id -> rows.find { it.scan.id == id } }

    SoftAppPageScaffold(
        title = stringResource(R.string.drawer_documents),
        subtitle = stringResource(R.string.scan_gallery_subtitle),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .appFormField(),
                    placeholder = { Text(stringResource(R.string.scan_search_hint)) },
                    leadingIcon = {
                        Icon(AppIcons.Search, contentDescription = stringResource(R.string.common_search))
                    },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlined(),
                )
                ScanCategoryFilterChips(
                    selected = filter,
                    counts = categoryCounts,
                    totalCount = rows.size,
                    onSelect = { filter = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (visibleRows.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.no_scans_in_filter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tc.TextSecondary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visibleRows, key = { it.scan.id }) { row ->
                            ScanSummaryCard(
                                row = row,
                                onClick = { selectedId = row.scan.id },
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { row ->
        ScanDetailDialog(
            row = row,
            onDismiss = { selectedId = null },
            onOpen = { openPdf(context, row.scan) },
            onShare = {
                val file = File(row.scan.filePath)
                if (file.exists()) ShareHelper(context).sharePdf(file)
            },
            onDelete = {
                selectedId = null
                scanToDelete = row.scan
            },
            onCategory = { category ->
                scope.launch { scanRepository.updateScanCategory(row.scan.id, category) }
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
                    text = stringResource(row.category.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.AccentPrimary,
                    maxLines = 1,
                )
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
