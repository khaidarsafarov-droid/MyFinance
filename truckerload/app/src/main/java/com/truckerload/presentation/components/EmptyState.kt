package com.truckerload.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.TruckerLoadTheme

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryAction: @Composable () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(horizontal = 8.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tc.AccentPrimary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = tc.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            textAlign = TextAlign.Center,
        )
        primaryAction()
    }
}

@Composable
fun EmptyStateButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TlButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(label)
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStatePreview() {
    TruckerLoadTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        EmptyState(
            icon = AppIcons.LocalShipping,
            title = stringResource(R.string.home_empty_all_title),
            message = stringResource(R.string.home_empty_filtered_body),
            primaryAction = {
                EmptyStateButton(
                    label = stringResource(R.string.empty_add_first_load),
                    onClick = {},
                )
            },
        )
    }
}

@Preview(name = "Dynamic", showBackground = true)
@Composable
private fun EmptyStateDynamicPreview() {
    TruckerLoadTheme(darkTheme = false, dynamicColor = true) {
        EmptyState(
            icon = AppIcons.LocalGasStation,
            title = stringResource(R.string.diesel_empty_week),
            message = stringResource(R.string.empty_diesel_message),
            primaryAction = {
                EmptyStateButton(label = stringResource(R.string.empty_log_diesel), onClick = {})
            },
        )
    }
}
