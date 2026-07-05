package com.truckerload.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PhotoManager
import java.io.File

@Composable
fun PhotoGridItem(
    photo: PhotoEntity,
    loadLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val fileExists = remember(photo.filePath) { File(photo.filePath).exists() }
    val thumbnail = remember(photo.filePath) {
        if (!fileExists) return@remember null
        BitmapFactory.decodeFile(photo.filePath)?.asImageBitmap()
    }

    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = photo.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("📷", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Text(
                text = PhotoManager.formatDateTime(photo.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            val location = listOf(photo.city, photo.state).filter { it.isNotBlank() }.joinToString(", ")
            if (location.isNotBlank()) {
                Text(
                    text = "📍 $location",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            loadLabel?.let { label ->
                Text(
                    text = "🚛 $label",
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.AccentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
