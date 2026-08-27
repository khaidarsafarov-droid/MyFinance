package com.truckerload.presentation.screens.maintenance

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.domain.maintenance.MaintenanceVisit
import com.truckerload.domain.maintenance.MaintenanceVisits
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.util.Locale

@Composable
fun MaintenanceArchiveSection(
    archive: List<MaintenanceArchiveEntry>,
    isProcessingPhoto: Boolean,
    onAddArchive: () -> Unit,
    onDeleteVisit: (List<Long>) -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val visits = remember(archive) { MaintenanceVisits.group(archive) }

    SectionHeader(
        title = stringResource(R.string.maintenance_archive_section),
        onAdd = onAddArchive,
    )
    Text(
        text = stringResource(R.string.maintenance_archive_hint),
        style = MaterialTheme.typography.bodySmall,
        color = tc.TextSecondary,
    )
    if (isProcessingPhoto) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.maintenance_ocr_processing),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
    }
    if (visits.isEmpty()) {
        EmptyHint(stringResource(R.string.maintenance_empty_archive))
    } else {
        visits.forEach { visit ->
            VisitCard(
                visit = visit,
                onDelete = { onDeleteVisit(visit.ids) },
                onOpenPhoto = onOpenPhoto,
            )
        }
    }
}

@Composable
private fun VisitCard(
    visit: MaintenanceVisit,
    onDelete: () -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val receiptPath = visit.photoPath?.takeIf { it.isNotBlank() }
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (receiptPath != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onOpenPhoto(receiptPath) },
                ) {
                    AsyncImage(
                        model = File(receiptPath),
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Icon(
                        imageVector = AppIcons.AttachFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val title = visit.shopName.ifBlank {
                        visit.lines.firstOrNull()?.description.orEmpty()
                    }
                    if (title.isNotBlank()) {
                        Text(
                            title,
                            color = tc.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (receiptPath != null) {
                        ReceiptAttachedBadge(onClick = { onOpenPhoto(receiptPath) })
                    }
                }
                Text(visit.serviceDate, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                val showLineBreakdown = visit.lines.size > 1 || visit.shopName.isNotBlank()
                if (showLineBreakdown) {
                    visit.lines.forEach { line ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = line.description.ifBlank { stringResource(R.string.maintenance_line_name) },
                                color = tc.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", line.amount)}",
                                color = tc.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Text(
                    stringResource(
                        R.string.maintenance_lines_total,
                        String.format(Locale.US, "%,.2f", visit.total),
                    ),
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (receiptPath != null) {
                IconButton(onClick = { onOpenPhoto(receiptPath) }) {
                    Icon(
                        AppIcons.Photo,
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        tint = LocalTruckColors.current.Success,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(AppIcons.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
            }
        }
    }
}

@Composable
private fun ReceiptAttachedBadge(onClick: () -> Unit) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tc.Success),
        )
        Text(
            text = stringResource(R.string.maintenance_has_receipt),
            color = tc.Success,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
