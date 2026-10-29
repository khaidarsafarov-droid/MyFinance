package com.truckerload.presentation.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.components.EmptyState
import com.truckerload.presentation.components.ScreenLoadError
import com.truckerload.presentation.components.ScreenLoading
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors

@Composable
fun GalleryViewerScreen(
    onBack: () -> Unit,
    viewModel: GalleryMediaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<GalleryMediaRef?>(null) }
    var seenContent by remember { mutableStateOf(false) }
    LaunchedEffect(state.phase) {
        if (state.phase == ScreenPhase.Content) seenContent = true
        if (seenContent && state.phase == ScreenPhase.Empty) onBack()
    }
    val unlinked = stringResource(R.string.filter_unlinked)
    val title = if (state.unlinked || state.title.isBlank()) unlinked else state.title
    when (state.phase) {
        ScreenPhase.Loading -> SoftAppPageScaffold(
            title = title,
            useDashboardHero = true,
            showBack = true,
            onBack = onBack,
            showPhoneMenu = false,
        ) { padding ->
            ScreenLoading(modifier = Modifier.padding(padding))
        }
        ScreenPhase.Error -> SoftAppPageScaffold(
            title = title,
            useDashboardHero = true,
            showBack = true,
            onBack = onBack,
            showPhoneMenu = false,
        ) { padding ->
            ScreenLoadError(onRetry = viewModel::retry, modifier = Modifier.padding(padding))
        }
        ScreenPhase.Empty -> SoftAppPageScaffold(
            title = title,
            useDashboardHero = true,
            showBack = true,
            onBack = onBack,
            showPhoneMenu = false,
        ) { padding ->
            EmptyState(
                icon = AppIcons.PhotoLibrary,
                title = stringResource(R.string.photo_file_missing),
                message = stringResource(R.string.gallery_empty_filter),
                modifier = Modifier.padding(padding),
            )
        }
        ScreenPhase.Content -> if (state.items.isNotEmpty()) {
            GalleryViewerContent(
                title = title,
                items = state.items,
                focusId = state.focusId,
                onBack = onBack,
                onShare = { item -> shareGalleryFiles(context, state.kind, listOf(item.filePath)) },
                onDelete = { pendingDelete = it },
            )
        }
    }
    val condemned = pendingDelete
    if (condemned != null) {
        val tc = LocalTruckColors.current
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.common_delete), color = tc.TextPrimary) },
            text = {
                Text(
                    stringResource(
                        if (state.kind == GalleryKind.PHOTOS) {
                            R.string.photo_delete_confirm
                        } else {
                            R.string.scan_delete_confirm
                        },
                    ),
                    color = tc.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.delete(condemned.id, state.kind)
                    },
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
internal fun GalleryViewerContent(
    title: String,
    items: List<GalleryMediaRef>,
    focusId: String?,
    onBack: () -> Unit,
    onShare: (GalleryMediaRef) -> Unit,
    onDelete: (GalleryMediaRef) -> Unit,
) {
    val brand = LocalBrandPalette.current
    val pagerState = rememberPagerState(pageCount = { items.size })
    var focused by remember(focusId) { mutableStateOf(false) }
    LaunchedEffect(focusId, items) {
        if (focused) return@LaunchedEffect
        val index = items.indexOfFirst { it.id == focusId }
        if (index >= 0) {
            pagerState.scrollToPage(index)
            focused = true
        }
    }
    val page = pagerState.currentPage.coerceIn(0, items.lastIndex)
    val current = items[page]
    var scale by remember(page) { mutableStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }
    SoftAppPageScaffold(
        title = title,
        subtitle = stringResource(R.string.gallery_page, page + 1, items.size),
        useDashboardHero = true,
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
        actions = {
            IconButton(onClick = { onShare(current) }) {
                Icon(AppIcons.Share, contentDescription = stringResource(R.string.share), tint = brand.heroText)
            }
            IconButton(onClick = { onDelete(current) }) {
                Icon(AppIcons.Delete, contentDescription = stringResource(R.string.common_delete), tint = brand.heroText)
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SoftUiColors.BackgroundDark),
            userScrollEnabled = scale <= 1.02f,
            key = { items[it].id },
        ) { index ->
            val item = items[index]
            val active = index == page
            ZoomableGalleryPage(
                item = item,
                scale = if (active) scale else 1f,
                offset = if (active) offset else Offset.Zero,
                onTransform = { zoom, pan ->
                    if (!active) return@ZoomableGalleryPage
                    val next = (scale * zoom).coerceIn(1f, 5f)
                    scale = next
                    offset = if (next <= 1.02f) {
                        Offset.Zero
                    } else {
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-4000f, 4000f),
                            y = (offset.y + pan.y).coerceIn(-4000f, 4000f),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ZoomableGalleryPage(
    item: GalleryMediaRef,
    scale: Float,
    offset: Offset,
    onTransform: (zoom: Float, pan: Offset) -> Unit,
) {
    val bitmap = rememberGalleryThumbnail(item.filePath, 1440)
    val transform = rememberUpdatedState(onTransform)
    val zoomed = rememberUpdatedState(scale > 1.02f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .galleryPinchZoom { zoom, pan, pointers ->
                if (pointers >= 2 || zoomed.value) {
                    transform.value(zoom, pan)
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin.Center
                    },
            )
        } else {
            Text(
                text = stringResource(R.string.photo_file_missing),
                color = SoftUiColors.TextPrimaryDark,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

/**
 * Pinch always zooms. One-finger drags pan only while zoomed, so the pager
 * can still swipe between pages at the default scale.
 */
private fun Modifier.galleryPinchZoom(
    onTransform: (zoom: Float, pan: Offset, pointers: Int) -> Boolean,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            val consume = onTransform(event.calculateZoom(), event.calculatePan(), pressed)
            if (consume) {
                event.changes.forEach { change ->
                    if (change.positionChanged()) change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
