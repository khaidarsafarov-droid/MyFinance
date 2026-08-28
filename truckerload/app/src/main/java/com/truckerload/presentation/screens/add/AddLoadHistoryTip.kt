package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

/**
 * Empty-state hint on Add load: a whole Telegram group chat can be exported
 * and imported (bot `/import`, or paste / file in the app).
 */
@Composable
fun AddLoadHistoryTip(modifier: Modifier = Modifier) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = AppIcons.Info,
                contentDescription = null,
                tint = tc.AccentPrimary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_load_history_tip_title),
                    style = AppTypography.CardTitle,
                    color = tc.TextPrimary,
                )
                Text(
                    text = stringResource(R.string.add_load_history_tip_body),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}

fun shouldShowAddLoadHistoryTip(
    mode: AddLoadInputMode,
    rawText: String,
    isExtractingDocument: Boolean,
): Boolean {
    if (rawText.isNotBlank() || isExtractingDocument) return false
    return mode == AddLoadInputMode.PASTE || mode == AddLoadInputMode.DOCUMENT
}
