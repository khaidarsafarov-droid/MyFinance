package com.truckerload.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.components.GoogleMapsHeatmapCard
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSelectedStateStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.DarkGlassScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit = {},
    embedded: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val loadRepository = LocalLoadRepository.current
    val selectedStateStore = LocalSelectedStateStore.current
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModel.Factory(loadRepository, selectedStateStore)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (embedded) {
        MapScreenBody(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            viewModel = viewModel,
        )
    } else {
        Scaffold(
            containerColor = BentoGlassTheme.ScreenBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            DarkGlassScreenTitle(stringResource(R.string.map_title))
                            Text(
                                text = stringResource(R.string.map_subtitle_all_loads, uiState.totalLoads),
                                style = AppTypography.Subtitle,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = tc.TextPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BentoGlassTheme.ScreenBackground,
                        titleContentColor = tc.TextPrimary,
                    ),
                )
            }
        ) { padding ->
            MapScreenBody(
                padding = padding,
                uiState = uiState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun MapScreenBody(
    padding: PaddingValues,
    uiState: MapUiState,
    viewModel: MapViewModel,
) {
    val tc = LocalTruckColors.current
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = tc.AccentPrimary)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            GoogleMapsHeatmapCard(
                metrics = uiState.metrics,
                selectedCode = uiState.selectedStateCode,
                refreshing = false,
                onStateSelected = viewModel::setSelectedState,
            )
        }
    }
}
