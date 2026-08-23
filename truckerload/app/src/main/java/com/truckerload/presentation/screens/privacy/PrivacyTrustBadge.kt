package com.truckerload.presentation.screens.privacy

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun PrivacyTrustBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val cloudBackupEnabled by viewModel.cloudBackupEnabled.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    val label = stringResource(
        if (cloudBackupEnabled) R.string.privacy_badge_cloud_backup
        else R.string.privacy_badge_device_only,
    )
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = tc.CardBackground,
        contentColor = tc.TextSecondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = AppIcons.Lock,
                contentDescription = label,
                tint = tc.AccentPrimary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = AppTypography.Caption,
                color = tc.TextSecondary,
                maxLines = 1,
            )
        }
    }
}
