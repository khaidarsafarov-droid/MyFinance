package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.components.SoftActionChip

@Composable
internal fun CommunityOverflowMenu(
    onOpenVoiceRooms: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SoftActionChip(
            icon = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.community_more_cd),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.voice_rooms)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Mic, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onOpenVoiceRooms()
                },
            )
        }
    }
}
