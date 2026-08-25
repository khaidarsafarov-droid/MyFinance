package com.truckerload.presentation.screens.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PDFGenerator
import java.io.File

@Composable
internal fun ScanDetailDialog(
    row: ScanListRow,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCategory: (ScanDocumentCategory) -> Unit,
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
                Text(
                    text = stringResource(R.string.scan_category_pick),
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
                ScanCategoryPickerChips(
                    selected = row.category,
                    onSelect = onCategory,
                )
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
