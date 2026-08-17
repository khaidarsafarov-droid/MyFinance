package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun AddLoadModeSelector(
    selected: AddLoadInputMode,
    onSelect: (AddLoadInputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.add_load_mode_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Text(
            text = stringResource(R.string.add_load_mode_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeChip(
                selected = selected == AddLoadInputMode.PASTE,
                onClick = { onSelect(AddLoadInputMode.PASTE) },
                label = stringResource(R.string.add_load_mode_paste),
                icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
            ModeChip(
                selected = selected == AddLoadInputMode.MANUAL,
                onClick = { onSelect(AddLoadInputMode.MANUAL) },
                label = stringResource(R.string.add_load_mode_manual),
                icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
            ModeChip(
                selected = selected == AddLoadInputMode.DOCUMENT,
                onClick = { onSelect(AddLoadInputMode.DOCUMENT) },
                label = stringResource(R.string.add_load_mode_document),
                icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon,
        modifier = modifier,
        shape = AppFilterChipDefaults.shape(),
        colors = AppFilterChipDefaults.colors(),
    )
}
