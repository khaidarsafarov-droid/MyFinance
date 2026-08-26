package com.truckerload.presentation.screens.privacy

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val cloudBackup by viewModel.cloudBackupEnabled.collectAsStateWithLifecycle()
    val tabletChrome = useNavigationRail()

    SoftAppPageScaffold(
        title = stringResource(R.string.privacy_screen_title),
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalContentScroll()
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrivacyTrustCard(
                icon = AppIcons.Lock,
                title = stringResource(R.string.privacy_card_loads_title),
                body = stringResource(
                    if (cloudBackup) R.string.privacy_card_loads_body_cloud
                    else R.string.privacy_card_loads_body_device,
                ),
            )
        }
    }
}
