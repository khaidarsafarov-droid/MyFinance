package com.truckerload.presentation.screens.maintenance

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
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
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.presentation.theme.AppColors
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.util.Locale

@Composable
fun MaintenanceArchiveSection(
    archive: List<MaintenanceArchiveEntry>,
    isProcessingPhoto: Boolean,
    onAddArchive: () -> Unit,
    onDeleteArchive: (Long) -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val tc = LocalTruckColors.current

    SectionHeader(
        title = stringResource(R.string.maintenance_archive_section),
        onAdd = onAddArchive,
        addIcon = Icons.Default.CameraAlt,
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
    if (archive.isEmpty()) {
        EmptyHint(stringResource(R.string.maintenance_empty_archive))
    } else {
        archive.forEach { entry ->
            ArchiveCard(
                entry = entry,
                onDelete = { onDeleteArchive(entry.id) },
                onOpenPhoto = onOpenPhoto,
            )
        }
    }
}

@Composable
private fun ArchiveCard(
    entry: MaintenanceArchiveEntry,
    onDelete: () -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val tc = LocalTruckColors.current
    val hasReceipt = !entry.photoPath.isNullOrBlank()
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasReceipt) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onOpenPhoto(entry.photoPath!!) },
                ) {
                    AsyncImage(
                        model = File(entry.photoPath),
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Icon(
                        imageVector = Icons.Default.AttachFile,
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (entry.serviceName.isNotBlank()) {
                        Text(
                            entry.serviceName,
                            color = tc.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (hasReceipt) {
                        ReceiptAttachedBadge(onClick = { onOpenPhoto(entry.photoPath!!) })
                    }
                }
                Text(
                    entry.description,
                    color = if (entry.serviceName.isBlank()) tc.TextPrimary else tc.TextSecondary,
                    style = if (entry.serviceName.isBlank()) {
                        MaterialTheme.typography.titleSmall
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                )
                Text(entry.serviceDate, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                Text(
                    "$${String.format(Locale.US, "%,.2f", entry.amount)}",
                    color = tc.AccentExpense,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (hasReceipt) {
                IconButton(onClick = { onOpenPhoto(entry.photoPath!!) }) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = stringResource(R.string.maintenance_receipt_photo),
                        tint = AppColors.RpmGreen,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = tc.TextSecondary)
            }
        }
    }
}

@Composable
private fun ReceiptAttachedBadge(onClick: () -> Unit) {
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
                .background(AppColors.RpmGreen),
        )
        Text(
            text = stringResource(R.string.maintenance_has_receipt),
            color = AppColors.RpmGreen,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
