package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalRegistrationService
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun ProfileCompletionReminders(
    onFillProfessional: () -> Unit,
    onFillCommunity: () -> Unit,
) {
    val progress by LocalRegistrationService.current.watchProgress().collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    if (!progress.professionalPending && !progress.communityPending) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (progress.professionalPending) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_reminder_professional),
                        style = AppTypography.Subtitle,
                        color = tc.TextPrimary,
                    )
                    OutlinedButton(onClick = onFillProfessional, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_reminder_fill))
                    }
                }
            }
        }
        if (progress.communityPending) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_reminder_community),
                        style = AppTypography.Subtitle,
                        color = tc.TextPrimary,
                    )
                    OutlinedButton(onClick = onFillCommunity, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.profile_reminder_fill))
                    }
                }
            }
        }
    }
}
