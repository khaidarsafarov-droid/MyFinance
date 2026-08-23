package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.OneUiTokens
import com.truckerload.presentation.theme.UiDimens

/**
 * One UI large-title header: icons in a thumb-friendly row, oversized title below.
 */
@Composable
fun OneUiLargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = OneUiTokens.ScreenHorizontal,
                end = OneUiTokens.ScreenHorizontal,
                top = OneUiTokens.TitleTopPadding,
                bottom = OneUiTokens.TitleBottomPadding,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Text(
            text = title,
            style = AppTypography.ScreenTitle,
            color = cs.onBackground,
            modifier = Modifier.padding(top = 4.dp),
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun OneUiBackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
    ) {
        Icon(
            AppIcons.ArrowBack,
            contentDescription = stringResource(R.string.common_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Lower-screen action zone for one-handed use (primary buttons, confirmations).
 */
@Composable
fun OneUiBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(
                    horizontal = OneUiTokens.ScreenHorizontal,
                    vertical = OneUiTokens.BottomActionPadding,
                ),
            content = content,
        )
    }
}

@Composable
fun oneUiContentPadding(): PaddingValues = PaddingValues(
    start = OneUiTokens.ScreenHorizontal,
    end = OneUiTokens.ScreenHorizontal,
    bottom = 8.dp,
)
