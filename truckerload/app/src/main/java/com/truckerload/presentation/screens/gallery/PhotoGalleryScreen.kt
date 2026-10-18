package com.truckerload.presentation.screens.gallery

import com.truckerload.presentation.components.DashboardHero
import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.components.EmptyState
import com.truckerload.presentation.components.EmptyStateButton
import com.truckerload.presentation.theme.rememberHeldScreenPhase
import com.truckerload.presentation.components.PhotoGridItem
import com.truckerload.presentation.components.ScreenLoadError
import com.truckerload.presentation.components.ScreenLoading
import com.truckerload.presentation.theme.AppFilterChip
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.utils.adaptiveGalleryMinCellSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoGalleryScreen(
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onTakePhoto: () -> Unit = {},
) {
    val viewModel: PhotoGalleryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loads by viewModel.loadsForLinking.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DashboardHero(
                title = stringResource(R.string.photo_gallery),
                showBack = true,
                onBack = onBack,
            )
        },
    ) { padding ->
        BentoGlassScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PhotoGalleryFilter.entries.forEach { filter ->
                            AppFilterChip(
                                selected = uiState.filter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = {
                                    Text(
                                        when (filter) {
                                            PhotoGalleryFilter.ALL -> stringResource(R.string.filter_all)
                                            PhotoGalleryFilter.TODAY -> stringResource(R.string.filter_today)
                                            PhotoGalleryFilter.THIS_WEEK -> stringResource(R.string.filter_this_week)
                                            PhotoGalleryFilter.BY_LOAD -> stringResource(R.string.filter_by_load)
                                        },
                                    )
                                },
                            )
                        }
                    }
                    if (uiState.filter == PhotoGalleryFilter.BY_LOAD && loads.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppFilterChip(
                                selected = uiState.selectedLoadId == null,
                                onClick = { viewModel.setLoadFilter(null) },
                                label = { Text(stringResource(R.string.filter_unlinked)) },
                            )
                            loads.take(12).forEach { load ->
                                AppFilterChip(
                                    selected = uiState.selectedLoadId == load.id,
                                    onClick = { viewModel.setLoadFilter(load.id) },
                                    label = { Text(load.tripId.ifBlank { load.id.take(8) }) },
                                )
                            }
                        }
                    }
                }

                when (rememberHeldScreenPhase(uiState.phase)) {
                    ScreenPhase.Loading -> ScreenLoading(modifier = Modifier.padding(horizontal = 4.dp))
                    ScreenPhase.Error -> ScreenLoadError(onRetry = viewModel::retry)
                    ScreenPhase.Empty -> EmptyState(
                        icon = AppIcons.PhotoLibrary,
                        title = stringResource(R.string.no_photos),
                        message = stringResource(R.string.empty_photos_message),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        primaryAction = {
                            EmptyStateButton(
                                label = stringResource(R.string.empty_take_photo),
                                onClick = onTakePhoto,
                                icon = AppIcons.PhotoCamera,
                            )
                        },
                    )
                    ScreenPhase.Content -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(adaptiveGalleryMinCellSize()),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.photos, key = { it.id }) { photo ->
                            PhotoGridItem(
                                photo = photo,
                                loadLabel = photo.loadId?.let { uiState.loadLabels[it] },
                                onClick = { onPhotoClick(photo.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}
