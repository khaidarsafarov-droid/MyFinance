package com.truckerload.presentation.screens.attach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.attach.AttachLoadSelection
import com.truckerload.domain.model.Load
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AttachPickMode {
    CAMERA,
    SCANNER,
}

private enum class AttachPickBrowseMode {
    THIS_WEEK,
    ALL,
}

/**
 * Picker shown when Camera / Scan is opened from the home-screen widget.
 * Quick list = last [AttachLoadSelection.QUICK_PICK_LIMIT] loads of the current trucking week;
 * user can also browse/search the full journal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachLoadPickScreen(
    mode: AttachPickMode,
    onLoadSelected: (Load) -> Unit,
    onCancel: () -> Unit,
    onAddLoad: () -> Unit = onCancel,
    loadRepository: LoadRepository = LocalLoadRepository.current,
) {
    val tc = LocalTruckColors.current
    var browseMode by remember { mutableStateOf(AttachPickBrowseMode.THIS_WEEK) }
    var weekLoads by remember { mutableStateOf<List<Load>?>(null) }
    var allLoads by remember { mutableStateOf<List<Load>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var hasAnyLoads by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        weekLoads = withContext(Dispatchers.IO) {
            loadRepository.getRecentLoadsThisWeek(AttachLoadSelection.QUICK_PICK_LIMIT)
        }
        // Detect empty journal for empty-state CTA without loading full list yet.
        hasAnyLoads = withContext(Dispatchers.IO) {
            loadRepository.getLoadsForLinking(limit = 1).isNotEmpty()
        }
    }

    LaunchedEffect(browseMode) {
        if (browseMode == AttachPickBrowseMode.ALL && allLoads == null) {
            allLoads = withContext(Dispatchers.IO) {
                loadRepository.getLoadsForLinking(limit = 200)
            }
        }
    }

    val displayedLoads: List<Load>? = when (browseMode) {
        AttachPickBrowseMode.THIS_WEEK -> weekLoads
        AttachPickBrowseMode.ALL -> allLoads?.let {
            AttachLoadSelection.filterBrowse(it, searchQuery)
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            when (browseMode) {
                                AttachPickBrowseMode.THIS_WEEK -> R.string.select_load
                                AttachPickBrowseMode.ALL -> R.string.attach_pick_browse_title
                            },
                        ),
                        color = tc.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (browseMode == AttachPickBrowseMode.ALL) {
                                browseMode = AttachPickBrowseMode.THIS_WEEK
                                searchQuery = ""
                            } else {
                                onCancel()
                            }
                        },
                    ) {
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
        },
    ) { padding ->
        when {
            displayedLoads == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            browseMode == AttachPickBrowseMode.THIS_WEEK && displayedLoads.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!hasAnyLoads) {
                        Text(
                            text = stringResource(R.string.attach_pick_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = tc.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.attach_pick_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onAddLoad) {
                            Text(stringResource(R.string.home_add_load_button))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.attach_pick_empty_week),
                            style = MaterialTheme.typography.titleMedium,
                            color = tc.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.attach_pick_empty_week_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { browseMode = AttachPickBrowseMode.ALL }) {
                            Text(stringResource(R.string.attach_pick_browse_all))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onAddLoad) {
                            Text(stringResource(R.string.home_add_load_button))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(
                                when {
                                    browseMode == AttachPickBrowseMode.ALL ->
                                        R.string.attach_pick_browse_hint
                                    mode == AttachPickMode.CAMERA ->
                                        R.string.attach_pick_camera_hint
                                    else -> R.string.attach_pick_scanner_hint
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (browseMode == AttachPickBrowseMode.ALL) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                placeholder = {
                                    Text(stringResource(R.string.home_search_hint))
                                },
                                label = { Text(stringResource(R.string.common_search)) },
                            )
                        }
                    }
                    if (displayedLoads.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.attach_pick_search_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    } else {
                        items(displayedLoads, key = { it.id }) { load ->
                            AttachLoadRow(load = load, onClick = { onLoadSelected(load) })
                        }
                    }
                    if (browseMode == AttachPickBrowseMode.THIS_WEEK) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { browseMode = AttachPickBrowseMode.ALL },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.attach_pick_browse_all))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachLoadRow(
    load: Load,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val routeLabel = load.route.ifBlank {
        listOf(load.pointA, load.pointB).filter { it.isNotBlank() }.joinToString(" → ")
    }.ifBlank { load.tripId }
    BentoGlassClickableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.LocalShipping,
                contentDescription = null,
                tint = tc.AccentPrimary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = load.tripId.ifBlank { load.id },
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = routeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(load.date)
                        if (load.totalMiles > 0) {
                            append(" · ")
                            append(String.format(Locale.US, "%,.0f mi", load.totalMiles))
                        }
                        if (load.totalRate > 0) {
                            append(" · ")
                            append(String.format(Locale.US, "$%,.0f", load.totalRate))
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}
