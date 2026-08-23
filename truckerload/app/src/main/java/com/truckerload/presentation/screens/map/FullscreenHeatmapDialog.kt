package com.truckerload.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.truckerload.presentation.icons.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.truckerload.R
import com.truckerload.presentation.components.GoogleMapsHeatmapCard
import com.truckerload.presentation.components.USStateMetric
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

/** Full-screen geographic efficiency map so the full US coverage is visible and pannable. */
@Composable
fun FullscreenHeatmapDialog(
    metrics: List<USStateMetric>,
    selectedCode: String,
    onStateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BentoGlassTheme.ScreenBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                GoogleMapsHeatmapCard(
                    metrics = metrics,
                    selectedCode = selectedCode,
                    refreshing = false,
                    onStateSelected = onStateSelected,
                    initialZoom = 3.5f,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(UiDimens.ToolbarTouchTarget),
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = tc.TextPrimary,
                    )
                }
            }
        }
    }
}
