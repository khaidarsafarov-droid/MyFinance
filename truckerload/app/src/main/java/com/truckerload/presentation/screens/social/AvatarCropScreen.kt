package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.LocalTruckColors
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropScreen(
    source: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val preparedBitmap = remember(source) { AvatarCropUtils.prepareBitmapForCrop(source) }
    val tc = LocalTruckColors.current
    val density = LocalDensity.current
    var cropLayout by remember(preparedBitmap) { mutableStateOf<AvatarCropLayout?>(null) }
    var userScale by remember(preparedBitmap) { mutableFloatStateOf(1f) }
    var offset by remember(preparedBitmap) { mutableStateOf(Offset.Zero) }
    // Only seed pan/zoom once per bitmap — never wipe the user's framing on remeasure.
    var transformInitialized by remember(preparedBitmap) { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_crop_photo)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val containerWidth = with(density) { maxWidth.toPx() }
                val containerHeight = with(density) { maxHeight.toPx() }
                if (containerWidth <= 0f || containerHeight <= 0f) {
                    return@BoxWithConstraints
                }
                val cropDiameter = min(containerWidth, containerHeight) * 0.72f
                val fitScale = AvatarCropUtils.fitScale(
                    bitmapWidth = preparedBitmap.width,
                    bitmapHeight = preparedBitmap.height,
                    containerWidth = containerWidth,
                    containerHeight = containerHeight,
                )
                val minScale = AvatarCropUtils.minUserScale(
                    cropDiameter = cropDiameter,
                    bitmapWidth = preparedBitmap.width,
                    bitmapHeight = preparedBitmap.height,
                    fitScale = fitScale,
                )
                val layout = remember(containerWidth, containerHeight, preparedBitmap) {
                    AvatarCropLayout(
                        containerWidth = containerWidth,
                        containerHeight = containerHeight,
                        cropDiameter = cropDiameter,
                        fitScale = fitScale,
                        minScale = minScale,
                    )
                }

                LaunchedEffect(layout, transformInitialized) {
                    cropLayout = layout
                    if (!transformInitialized) {
                        userScale = layout.minScale
                        offset = Offset.Zero
                        transformInitialized = true
                    } else {
                        // Keep framing when the crop viewport is remeasured (insets, rotation, etc.).
                        userScale = userScale.coerceIn(layout.minScale, layout.minScale * 4f)
                        offset = AvatarCropUtils.clampOffset(
                            offset = offset,
                            userScale = userScale,
                            fitScale = layout.fitScale,
                            bitmapWidth = preparedBitmap.width,
                            bitmapHeight = preparedBitmap.height,
                            containerWidth = layout.containerWidth,
                            containerHeight = layout.containerHeight,
                            cropDiameter = layout.cropDiameter,
                        )
                    }
                }

                fun clampCurrentOffset(scale: Float = userScale, pan: Offset = offset): Offset {
                    val currentLayout = layout
                    return AvatarCropUtils.clampOffset(
                        offset = pan,
                        userScale = scale,
                        fitScale = currentLayout.fitScale,
                        bitmapWidth = preparedBitmap.width,
                        bitmapHeight = preparedBitmap.height,
                        containerWidth = currentLayout.containerWidth,
                        containerHeight = currentLayout.containerHeight,
                        cropDiameter = currentLayout.cropDiameter,
                    )
                }

                val displayWidthDp = with(density) { (preparedBitmap.width * fitScale).toDp() }
                val displayHeightDp = with(density) { (preparedBitmap.height * fitScale).toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Full-viewport gestures so pan/zoom keep working when the image is scaled up.
                        .pointerInput(layout, minScale) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (userScale * zoom).coerceIn(minScale, minScale * 4f)
                                val newOffset = clampCurrentOffset(newScale, offset + pan)
                                userScale = newScale
                                offset = newOffset
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = preparedBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.profile_photo),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(displayWidthDp, displayHeightDp)
                            .graphicsLayer {
                                scaleX = userScale
                                scaleY = userScale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )

                    AvatarCropOverlay(
                        cropDiameter = with(density) { cropDiameter.toDp() },
                        accentColor = tc.AccentPrimary,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val layout = cropLayout ?: return@Button
                        val clamped = AvatarCropUtils.clampOffset(
                            offset = offset,
                            userScale = userScale,
                            fitScale = layout.fitScale,
                            bitmapWidth = preparedBitmap.width,
                            bitmapHeight = preparedBitmap.height,
                            containerWidth = layout.containerWidth,
                            containerHeight = layout.containerHeight,
                            cropDiameter = layout.cropDiameter,
                        )
                        val cropped = AvatarCropUtils.cropSquare(
                            source = preparedBitmap,
                            containerWidth = layout.containerWidth,
                            containerHeight = layout.containerHeight,
                            cropDiameter = layout.cropDiameter,
                            fitScale = layout.fitScale,
                            userScale = userScale.coerceIn(layout.minScale, layout.minScale * 4f),
                            offset = clamped,
                        )
                        onConfirm(cropped)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cropLayout != null,
                ) {
                    Text(stringResource(R.string.common_save))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}

private data class AvatarCropLayout(
    val containerWidth: Float,
    val containerHeight: Float,
    val cropDiameter: Float,
    val fitScale: Float,
    val minScale: Float,
)

@Composable
private fun AvatarCropOverlay(
    cropDiameter: androidx.compose.ui.unit.Dp,
    accentColor: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = cropDiameter.toPx() / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val left = center.x - radius
        val right = center.x + radius
        val top = center.y - radius
        val bottom = center.y + radius
        val overlayColor = Color.Black.copy(alpha = 0.58f)

        drawRect(overlayColor, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(overlayColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(overlayColor, topLeft = Offset(0f, top), size = Size(left, bottom - top))
        drawRect(overlayColor, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
        drawCircle(
            color = accentColor,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
