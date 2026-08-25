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
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

private val AboutGuideSections = listOf(
    R.string.about_guide_loads_title to R.string.about_guide_loads_body,
    R.string.about_guide_home_title to R.string.about_guide_home_body,
    R.string.about_guide_dispute_title to R.string.about_guide_dispute_body,
    R.string.about_guide_goal_title to R.string.about_guide_goal_body,
    R.string.about_guide_diesel_title to R.string.about_guide_diesel_body,
    R.string.about_guide_paycheck_title to R.string.about_guide_paycheck_body,
    R.string.about_guide_tax_title to R.string.about_guide_tax_body,
    R.string.about_guide_reports_title to R.string.about_guide_reports_body,
    R.string.about_guide_map_title to R.string.about_guide_map_body,
    R.string.about_guide_docs_title to R.string.about_guide_docs_body,
    R.string.about_guide_tools_title to R.string.about_guide_tools_body,
    R.string.about_guide_assistant_title to R.string.about_guide_assistant_body,
    R.string.about_guide_widget_title to R.string.about_guide_widget_body,
    R.string.about_guide_telegram_title to R.string.about_guide_telegram_body,
    R.string.about_guide_backup_title to R.string.about_guide_backup_body,
    R.string.about_guide_settings_title to R.string.about_guide_settings_body,
)

/**
 * In-app how-to overview opened from the drawer «О приложении».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit,
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

            AboutGuideSections.forEach { (titleRes, bodyRes) ->
                GuideSection(
                    title = stringResource(titleRes),
                    body = stringResource(bodyRes),
                )
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
