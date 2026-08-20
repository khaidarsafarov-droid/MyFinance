package com.truckerload.presentation.screens.privacy

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun CrowdStatsConsentDialog(
    onParticipate: () -> Unit,
    onNotNow: () -> Unit,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onNotNow,
        containerColor = tc.CardBackground,
        titleContentColor = tc.TextPrimary,
        textContentColor = tc.TextSecondary,
        title = { Text(stringResource(R.string.community_crowd_consent_title)) },
        text = { Text(stringResource(R.string.community_crowd_consent_body)) },
        confirmButton = {
            TextButton(onClick = onParticipate) {
                Text(stringResource(R.string.community_crowd_consent_participate), color = tc.AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.community_crowd_consent_not_now), color = tc.TextSecondary)
            }
        },
    )
}
