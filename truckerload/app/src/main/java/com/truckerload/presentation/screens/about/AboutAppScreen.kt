package com.truckerload.presentation.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

/**
 * In-app how-to overview opened from the drawer «О приложении».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit,
    onWriteImprove: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    SoftAppPageScaffold(
        title = stringResource(R.string.about_app_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.home_brand_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = tc.AccentPrimary,
            )
            Text(
                text = stringResource(
                    R.string.about_app_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            Text(
                text = stringResource(R.string.about_app_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextPrimary,
            )

            GuideSection(
                title = stringResource(R.string.about_guide_loads_title),
                body = stringResource(R.string.about_guide_loads_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_home_title),
                body = stringResource(R.string.about_guide_home_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_goal_title),
                body = stringResource(R.string.about_guide_goal_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_tools_title),
                body = stringResource(R.string.about_guide_tools_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_docs_title),
                body = stringResource(R.string.about_guide_docs_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_reports_title),
                body = stringResource(R.string.about_guide_reports_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_backup_title),
                body = stringResource(R.string.about_guide_backup_body),
            )
            GuideSection(
                title = stringResource(R.string.about_guide_settings_title),
                body = stringResource(R.string.about_guide_settings_body),
            )

            OutlinedButton(
                onClick = onWriteImprove,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.improve_title))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideSection(
    title: String,
    body: String,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = BentoGlassTheme.CardBorderMuted)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tc.TextPrimary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
    }
}
