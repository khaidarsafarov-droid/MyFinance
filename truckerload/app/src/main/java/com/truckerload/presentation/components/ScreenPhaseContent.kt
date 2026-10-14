package com.truckerload.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons

/** Placeholder while Room has not returned the list yet. */
@Composable
fun ScreenLoading(modifier: Modifier = Modifier, rows: Int = 4) {
    LoadListSkeleton(
        modifier = modifier,
        rows = rows,
        contentDescription = stringResource(R.string.screen_loading),
    )
}

/** Failure with a Retry action. Keeps the driver on the same screen. */
@Composable
fun ScreenLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = AppIcons.Refresh,
        title = stringResource(R.string.screen_error_title),
        message = stringResource(R.string.screen_error_message),
        modifier = modifier,
        primaryAction = {
            EmptyStateButton(
                label = stringResource(R.string.empty_retry),
                onClick = onRetry,
            )
        },
    )
}
