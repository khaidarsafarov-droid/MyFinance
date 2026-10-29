package com.truckerload.presentation.screens.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.components.EmptyState
import com.truckerload.presentation.components.EmptyStateButton
import com.truckerload.presentation.components.ScreenLoadError
import com.truckerload.presentation.components.ScreenLoading
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppFilterChip
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.appFormField
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(
    initialTab: GalleryKind,
    onBack: () -> Unit,
    onOpenLoad: (GalleryKind, String) -> Unit,
    onTakePhoto: () -> Unit,
    onScan: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    viewModel.ensureInitialTab(initialTab)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scanRepository = LocalScanRepository.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val scansDir = File(context.getExternalFilesDir(null), "scans")
            runCatching { scanRepository.cleanupOrphanScanFiles(scansDir) }
        }
    }
    val unlinked = stringResource(R.string.filter_unlinked)
    val locale = Locale.getDefault()
    val cards = remember(uiState.items, uiState.loads, uiState.filter, uiState.query, unlinked, locale) {
        GalleryCatalog.buildCards(
            items = uiState.items,
            loads = uiState.loads,
            filter = uiState.filter,
            query = uiState.query,
            now = Calendar.getInstance(),
            unlinkedLabel = unlinked,
            locale = locale,
        )
    }
    val tab = if (uiState.phase == ScreenPhase.Loading) initialTab else uiState.tab
    val phase = when (uiState.phase) {
        ScreenPhase.Loading, ScreenPhase.Error -> uiState.phase
        else -> if (cards.isEmpty()) ScreenPhase.Empty else ScreenPhase.Content
    }
    GalleryScreenContent(
        tab = tab,
        filter = uiState.filter,
        query = uiState.query,
        cards = cards,
        phase = phase,
        filteredEmpty = uiState.items.isNotEmpty() && cards.isEmpty(),
        onBack = onBack,
        onTab = viewModel::setTab,
        onFilter = viewModel::setFilter,
        onQuery = viewModel::setQuery,
        onRetry = viewModel::retry,
        onOpenLoad = { onOpenLoad(tab, it) },
        onCapture = if (tab == GalleryKind.PHOTOS) onTakePhoto else onScan,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryScreenContent(
    tab: GalleryKind,
    filter: GalleryFilter,
    query: String,
    cards: List<GalleryLoadCard>,
    phase: ScreenPhase,
    filteredEmpty: Boolean,
    onBack: () -> Unit,
    onTab: (GalleryKind) -> Unit,
    onFilter: (GalleryFilter) -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenLoad: (String) -> Unit,
    onCapture: () -> Unit,
) {
    SoftAppPageScaffold(
        title = stringResource(R.string.drawer_documents),
        useDashboardHero = true,
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GalleryTabRow(tab = tab, onTab = onTab)
            GalleryFilterRow(filter = filter, onFilter = onFilter)
            if (filter == GalleryFilter.BY_LOAD) {
                GallerySearchField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when (phase) {
                ScreenPhase.Loading -> ScreenLoading(modifier = Modifier.padding(horizontal = 16.dp))
                ScreenPhase.Error -> ScreenLoadError(onRetry = onRetry)
                ScreenPhase.Empty -> EmptyState(
                    icon = if (tab == GalleryKind.PHOTOS) AppIcons.PhotoLibrary else AppIcons.DocumentScanner,
                    title = stringResource(
                        if (tab == GalleryKind.PHOTOS) R.string.gallery_empty_photos else R.string.gallery_empty_scans,
                    ),
                    message = stringResource(
                        when {
                            filteredEmpty -> R.string.gallery_empty_filter
                            tab == GalleryKind.PHOTOS -> R.string.empty_photos_message
                            else -> R.string.gallery_empty_scans_body
                        },
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    primaryAction = {
                        if (!filteredEmpty) {
                            EmptyStateButton(
                                label = stringResource(
                                    if (tab == GalleryKind.PHOTOS) R.string.empty_take_photo else R.string.scanner,
                                ),
                                onClick = onCapture,
                                icon = if (tab == GalleryKind.PHOTOS) AppIcons.PhotoCamera else AppIcons.DocumentScanner,
                            )
                        }
                    },
                )
                ScreenPhase.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(cards, key = { it.loadKey }, contentType = { "gallery-load" }) { card ->
                        GalleryLoadCardRow(
                            card = card,
                            kind = tab,
                            onClick = { onOpenLoad(card.loadKey) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTabRow(tab: GalleryKind, onTab: (GalleryKind) -> Unit) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GalleryKind.entries.forEach { kind ->
            val selected = kind == tab
            val label = stringResource(
                if (kind == GalleryKind.PHOTOS) R.string.gallery_tab_photos else R.string.gallery_tab_scans,
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { this.selected = selected }
                    .clickable { onTab(kind) },
                shape = RoundedCornerShape(50),
                color = if (selected) brand.primary else brand.card,
                contentColor = if (selected) brand.onPrimary else brand.textPrimary,
                border = BorderStroke(1.dp, if (selected) brand.primary else brand.cardBorder),
            ) {
                Text(
                    text = label,
                    color = if (selected) brand.onPrimary else brand.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFilterRow(filter: GalleryFilter, onFilter: (GalleryFilter) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GalleryFilter.entries.forEach { item ->
            AppFilterChip(
                selected = filter == item,
                onClick = { onFilter(item) },
                label = {
                    Text(
                        stringResource(
                            when (item) {
                                GalleryFilter.ALL -> R.string.filter_all
                                GalleryFilter.TODAY -> R.string.filter_today
                                GalleryFilter.THIS_WEEK -> R.string.filter_this_week
                                GalleryFilter.BY_LOAD -> R.string.filter_by_load
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun GallerySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val brand = LocalBrandPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .appFormField(),
        placeholder = {
            Text(
                text = stringResource(R.string.gallery_search_hint),
                color = tc.TextSecondary,
            )
        },
        leadingIcon = {
            Icon(
                AppIcons.Search,
                contentDescription = stringResource(R.string.common_search),
                tint = tc.TextPrimary,
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = tc.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = tc.TextPrimary,
            unfocusedTextColor = tc.TextPrimary,
            disabledTextColor = tc.TextSecondary,
            cursorColor = brand.primary,
            focusedBorderColor = brand.primary,
            unfocusedBorderColor = tc.TextSecondary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedPlaceholderColor = tc.TextSecondary,
            unfocusedPlaceholderColor = tc.TextSecondary,
            focusedLeadingIconColor = tc.TextPrimary,
            unfocusedLeadingIconColor = tc.TextPrimary,
        ),
    )
}

@Composable
private fun GalleryLoadCardRow(
    card: GalleryLoadCard,
    kind: GalleryKind,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val count = pluralStringResource(
        if (kind == GalleryKind.PHOTOS) R.plurals.gallery_photo_count else R.plurals.gallery_scan_count,
        card.count,
        card.count,
    )
    BentoGlassClickableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = card.tripId,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tc.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.route.isNotBlank()) {
                Text(
                    text = card.route,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = card.dateLabel, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
                Text(text = count, style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
            if (card.thumbnailPaths.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    card.thumbnailPaths.forEach { path ->
                        GalleryThumb(
                            path = path,
                            kind = kind,
                            maxEdgePx = 192,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GalleryThumb(
    path: String,
    kind: GalleryKind,
    maxEdgePx: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val bitmap = rememberGalleryThumbnail(path, maxEdgePx)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = if (kind == GalleryKind.PHOTOS) AppIcons.PhotoCamera else AppIcons.DocumentScanner,
                contentDescription = contentDescription,
                tint = tc.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
