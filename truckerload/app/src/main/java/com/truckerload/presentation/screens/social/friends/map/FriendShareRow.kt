package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendLiveStatus
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppColors
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FriendShareRow(
    link: FriendShareLink,
    overlay: FriendMapOverlay?,
    liveStatus: FriendLiveStatus,
    selected: Boolean,
    editing: Boolean,
    onEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSavePrefs: (Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    onTogglePath: () -> Unit,
    onFocusMap: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var shareLoc by remember(link.friendUserId, link.shareMyLocation) {
        mutableStateOf(link.shareMyLocation)
    }
    var shareRoute by remember(link.friendUserId, link.shareMyRoute) {
        mutableStateOf(link.shareMyRoute)
    }
    val statusColor = when (liveStatus) {
        FriendLiveStatus.ONLINE -> AppColors.RpmGreen
        FriendLiveStatus.RECENT -> AppColors.RpmYellow
        FriendLiveStatus.OFFLINE -> AppColors.TextMuted
    }
    val statusLabel = stringResource(
        when (liveStatus) {
            FriendLiveStatus.ONLINE -> R.string.friends_status_online
            FriendLiveStatus.RECENT -> R.string.friends_status_recent
            FriendLiveStatus.OFFLINE -> R.string.friends_status_offline
        },
    )
    val handle = "@${link.friendNickname.ifBlank { "Driver" }}"

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, top = 2.dp, end = 4.dp),
            ) {
                Text(
                    text = handle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) tc.AccentPrimary else tc.TextPrimary,
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
                Text(
                    text = stringResource(
                        R.string.friends_share_summary,
                        if (link.shareMyLocation) "✓" else "—",
                        if (link.shareMyRoute) "✓" else "—",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                )
                overlay?.route?.let { route ->
                    Text(
                        text = "${route.originLabel} → ${route.destinationLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.TextSecondary,
                    )
                }
            }
            FriendOverflowMenu(
                showPath = overlay?.showPath == true,
                canMap = overlay != null,
                onTogglePath = onTogglePath,
                onFocusMap = onFocusMap,
                onEditShare = { if (editing) onCloseEdit() else onEdit() },
                onDelete = onDelete,
            )
        }
        if (editing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_me), color = tc.TextPrimary)
                Switch(
                    checked = shareLoc,
                    onCheckedChange = { shareLoc = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_route), color = tc.TextPrimary)
                Switch(
                    checked = shareRoute,
                    onCheckedChange = { shareRoute = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Button(
                onClick = {
                    onSavePrefs(shareLoc, shareRoute)
                    onCloseEdit()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.friends_prefs_save))
            }
        }
    }
}

@Composable
internal fun FriendOverflowMenu(
    showPath: Boolean,
    canMap: Boolean,
    onTogglePath: () -> Unit,
    onFocusMap: () -> Unit,
    onEditShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.friends_friend_menu_cd),
                tint = LocalTruckColors.current.TextSecondary,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (canMap) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (showPath) R.string.friends_hide_path else R.string.friends_show_path,
                            ),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onTogglePath()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.friends_focus)) },
                    onClick = {
                        menuOpen = false
                        onFocusMap()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.friends_edit_share)) },
                onClick = {
                    menuOpen = false
                    onEditShare()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.friends_remove)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}
