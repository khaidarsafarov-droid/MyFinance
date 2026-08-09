package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

/**
 * Tablet landscape list | detail shell.
 * List pane stays readable (~40%); detail fills the rest.
 */
@Composable
fun ListDetailLayout(
    listContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listWeight: Float = 0.42f,
    detailWeight: Float = 0.58f,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(listWeight)
                .fillMaxHeight()
                .widthIn(min = UiDimens.ListPaneMinWidth),
        ) {
            listContent()
        }
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 0.5.dp,
            color = BentoGlassTheme.CardBorderMuted,
        )
        Box(
            modifier = Modifier
                .weight(detailWeight)
                .fillMaxHeight(),
        ) {
            detailContent()
        }
    }
}

@Composable
fun ListDetailEmptyPane(
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.tablet_detail_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.tablet_detail_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
