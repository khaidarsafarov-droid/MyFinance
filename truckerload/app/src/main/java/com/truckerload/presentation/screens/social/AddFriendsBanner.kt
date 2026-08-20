package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R

/**
 * Persistent CTA until the friend circle is large enough for ranking.
 * Not a first-use hint — does not dismiss itself.
 */
@Composable
internal fun AddFriendsBanner(
    visible: Boolean,
    onOpenFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    CommunityEmptyHint(
        message = stringResource(R.string.community_need_friends_for_ranking),
        actionLabel = stringResource(R.string.community_add_friends),
        onAction = onOpenFriends,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
