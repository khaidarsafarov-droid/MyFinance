package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.truckerload.R

@Composable
fun BotStatusBadge(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = if (active) {
        stringResource(R.string.home_cd_bot_active)
    } else {
        stringResource(R.string.home_cd_bot_inactive)
    }
    Box(
        modifier = modifier
            .size(12.dp)
            .semantics { contentDescription = description }
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        contentAlignment = Alignment.Center,
    ) {}
}
