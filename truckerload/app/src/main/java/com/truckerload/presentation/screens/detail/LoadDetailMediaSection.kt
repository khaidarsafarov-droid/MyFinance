package com.truckerload.presentation.screens.detail

import com.truckerload.presentation.icons.AppIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.utils.rememberDecodedBitmap

@Composable
internal fun LoadDetailMediaSection(
    photos: List<PhotoEntity>,
    scans: List<ScanEntity>,
    onPhotoClick: (String) -> Unit,
    onCamera: () -> Unit,
    onScanner: () -> Unit,
    onScanClick: (ScanEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.load_detail_photos_scans),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TlOutlinedButton(
                    onClick = onCamera,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        AppIcons.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.camera),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TlOutlinedButton(
                    onClick = onScanner,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        AppIcons.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.scanner),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos, key = { it.id }, contentType = { "photo" }) { photo ->
                        val thumb = rememberDecodedBitmap(photo.filePath)
                        if (thumb != null) {
                            Image(
                                bitmap = thumb,
                                contentDescription = photo.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPhotoClick(photo.id) },
                            )
                        }
                    }
                }
            }
            scans.forEach { scan ->
                Text(
                    text = scan.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onScanClick(scan) }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
