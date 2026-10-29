package com.truckerload.presentation.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.components.EmptyState
import com.truckerload.presentation.components.ScreenLoadError
import com.truckerload.presentation.components.ScreenLoading
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.utils.adaptiveGalleryMinCellSize

@Composable
fun GalleryLoadScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: GalleryMediaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    LaunchedEffect(state.items) {
        val ids = state.items.mapTo(HashSet()) { it.id }
        val dropped = selected.removeAll { it !in ids }
        if (dropped && selected.isEmpty()) selecting = false
    }
    val unlinked = stringResource(R.string.filter_unlinked)
    val title = when {
        selecting -> stringResource(R.string.gallery_selected, selected.size)
        state.unlinked || state.title.isBlank() -> unlinked
        else -> state.title
    }
    GalleryLoadGridContent(
        title = title,
        route = if (selecting) "" else state.route,
        kind = state.kind,
        items = state.items,
        phase = state.phase,
        selecting = selecting,
        selectedIds = selected.toSet(),
        onBack = {
            if (selecting) {
                selecting = false
                selected.clear()
            } else {
                onBack()
            }
        },
        onRetry = viewModel::retry,
        onStartSelect = {
            selecting = true
        },
        onToggle = { id ->
            if (!selecting) {
                selecting = true
            }
            if (id in selected) selected.remove(id) else selected.add(id)
        },
        onOpen = onOpenItem,
        onShare = {
            val paths = state.items.filter { it.id in selected }.map { it.filePath }
            shareGalleryFiles(context, state.kind, paths)
        },
    )
}

@Composable
internal fun GalleryLoadGridContent(
    title: String,
    route: String,
    kind: GalleryKind,
    items: List<GalleryMediaRef>,
    phase: ScreenPhase,
    selecting: Boolean,
    selectedIds: Set<String>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStartSelect: () -> Unit,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onShare: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    SoftAppPageScaffold(
        title = title,
        subtitle = route.ifBlank { null },
        useDashboardHero = true,
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
        actions = {
            if (selecting && selectedIds.isNotEmpty()) {
                IconButton(onClick = onShare) {
                    Icon(
                        AppIcons.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = brand.heroText,
                    )
                }
            } else if (!selecting && items.isNotEmpty()) {
                IconButton(onClick = onStartSelect) {
                    Icon(
                        AppIcons.CheckCircle,
                        contentDescription = stringResource(R.string.gallery_select),
                        tint = brand.heroText,
                    )
                }
            }
        },
    ) { padding ->
        when (phase) {
            ScreenPhase.Loading -> ScreenLoading(modifier = Modifier.padding(padding))
            ScreenPhase.Error -> ScreenLoadError(onRetry = onRetry, modifier = Modifier.padding(padding))
            ScreenPhase.Empty -> EmptyState(
                icon = if (kind == GalleryKind.PHOTOS) AppIcons.PhotoLibrary else AppIcons.DocumentScanner,
                title = stringResource(
                    if (kind == GalleryKind.PHOTOS) R.string.gallery_empty_photos else R.string.gallery_empty_scans,
                ),
                message = stringResource(R.string.gallery_empty_filter),
                modifier = Modifier.padding(padding),
            )
            ScreenPhase.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(adaptiveGalleryMinCellSize()),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }, contentType = { "gallery-item" }) { item ->
                    val selected = item.id in selectedIds
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .combinedClickable(
                                onClick = {
                                    if (selecting) onToggle(item.id) else onOpen(item.id)
                                },
                                onLongClick = { onToggle(item.id) },
                            ),
                    ) {
                        GalleryThumb(
                            path = item.filePath,
                            kind = kind,
                            maxEdgePx = 384,
                            contentDescription = item.fileName,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.35f)),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    AppIcons.Check,
                                    contentDescription = null,
                                    tint = brand.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
