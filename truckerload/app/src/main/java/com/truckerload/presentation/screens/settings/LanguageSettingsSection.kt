package com.truckerload.presentation.screens.settings

import com.truckerload.presentation.icons.AppIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.AppLanguageManager

@Composable
fun LanguageSettingsSection(
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val current = AppLanguageManager.getSupportedLanguages()
        .firstOrNull { it.code == AppLanguageManager.getCurrentLanguageCode() }

    BentoGlassSection(
        title = stringResource(R.string.settings_language_title),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLanguage)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    AppIcons.Flag,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = current?.nativeName ?: stringResource(R.string.settings_language_en),
                        color = tc.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_language_desc),
                        color = tc.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = tc.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
