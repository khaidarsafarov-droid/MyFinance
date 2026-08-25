package com.truckerload.presentation.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.AnalyticsShareFormat

@Composable
fun AnalyticsShareDialog(
    onDismiss: () -> Unit,
    onPick: (AnalyticsShareFormat) -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.analytics_share_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.analytics_share_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FormatRow(
                    title = stringResource(R.string.analytics_share_as_text),
                    hint = stringResource(R.string.analytics_share_as_text_hint),
                    onClick = { onPick(AnalyticsShareFormat.TEXT) },
                )
                FormatRow(
                    title = stringResource(R.string.analytics_share_as_csv),
                    hint = stringResource(R.string.analytics_share_as_csv_hint),
                    onClick = { onPick(AnalyticsShareFormat.CSV) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun FormatRow(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
    }
}
