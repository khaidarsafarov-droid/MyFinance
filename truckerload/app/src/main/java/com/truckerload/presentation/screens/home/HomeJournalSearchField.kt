package com.truckerload.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.presentation.theme.BentoGlassSearchField

/**
 * Collects the immediate search text. Callers that paint the list should read
 * only the debounced query so a keystroke does not recompose the scaffold.
 */
@Composable
internal fun HomeJournalSearchField(
    viewModel: HomeViewModel,
    placeholder: String,
    visibleWhenBlank: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    if (!visibleWhenBlank && query.isBlank()) return
    BentoGlassSearchField(
        value = query,
        onValueChange = viewModel::setSearchQuery,
        placeholder = placeholder,
        modifier = modifier,
    )
}
