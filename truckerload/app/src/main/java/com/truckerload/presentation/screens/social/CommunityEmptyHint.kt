package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.components.TlButton as Button

@Composable
internal fun CommunityEmptyHint(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message, style = AppTypography.Subtitle, color = tc.TextSecondary)
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
internal fun CommunityAddFriendsHint(onOpenFriends: () -> Unit) {
    CommunityEmptyHint(
        message = stringResource(R.string.community_empty_peers),
        actionLabel = stringResource(R.string.community_add_friends),
        onAction = onOpenFriends,
    )
}
