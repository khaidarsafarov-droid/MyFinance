package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.preferences.CommunityHintArea
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.truckerload.presentation.components.TlButton as Button

@Composable
internal fun CommunityEmptyHint(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
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

/**
 * How-to copy for a community area. Shown only until the user starts using it
 * (CTA tap or real content in that area).
 */
@Composable
internal fun CommunityFirstUseHint(
    area: CommunityHintArea,
    message: String,
    hasContent: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val settings = LocalSettingsDataStore.current
    val used by settings.communityHintUsed(area).collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    fun dismiss() {
        scope.launch { settings.markCommunityHintUsed(area) }
    }
    LaunchedEffect(hasContent) {
        if (hasContent) {
            withContext(NonCancellable) { settings.markCommunityHintUsed(area) }
        }
    }
    if (!used && !hasContent) {
        CommunityEmptyHint(
            message = message,
            actionLabel = actionLabel,
            onAction = if (onAction != null) {
                {
                    dismiss()
                    onAction()
                }
            } else {
                null
            },
        )
    }
}

@Composable
internal fun CommunityAddFriendsHint(
    area: CommunityHintArea,
    onOpenFriends: () -> Unit,
    hasContent: Boolean = false,
) {
    CommunityFirstUseHint(
        area = area,
        message = stringResource(R.string.community_empty_peers),
        hasContent = hasContent,
        actionLabel = stringResource(R.string.community_add_friends),
        onAction = onOpenFriends,
    )
}
