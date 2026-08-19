package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendRequest
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton

@Composable
internal fun FriendRequestsSection(
    incoming: List<FriendRequest>,
    outgoing: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    if (incoming.isEmpty() && outgoing.isEmpty()) return
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (incoming.isNotEmpty()) {
            Text(
                text = stringResource(R.string.friends_requests_incoming_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            incoming.forEach { request ->
                RequestRow(
                    nickname = request.peerNickname,
                    incoming = true,
                    onAccept = { onAccept(request.id) },
                    onDecline = { onDecline(request.id) },
                    onCancel = { },
                )
            }
        }
        if (outgoing.isNotEmpty()) {
            Text(
                text = stringResource(R.string.friends_requests_outgoing_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            outgoing.forEach { request ->
                RequestRow(
                    nickname = request.peerNickname,
                    incoming = false,
                    onAccept = { },
                    onDecline = { },
                    onCancel = { onCancel(request.id) },
                )
            }
        }
    }
}

@Composable
private fun RequestRow(
    nickname: String,
    incoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.friends_request_handle, nickname),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (incoming) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.friends_request_accept))
                }
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.friends_request_decline))
                }
            } else {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.friends_request_cancel))
                }
            }
        }
    }
}
