package com.truckerload.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.truckerload.presentation.components.EmptyState
import com.truckerload.presentation.components.EmptyStateButton
import com.truckerload.presentation.icons.AppIcons

@Composable
internal fun HomeEmptyJournal(
    title: String,
    body: String,
    ctaLabel: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = AppIcons.LocalShipping,
        title = title,
        message = body,
        modifier = modifier,
        leading = { EmptyJournalMark() },
        primaryAction = {
            EmptyStateButton(label = ctaLabel, onClick = onCta)
        },
    )
}
