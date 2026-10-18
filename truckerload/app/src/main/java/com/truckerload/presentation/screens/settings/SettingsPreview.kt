package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.TruckerLoadTheme

@Composable
internal fun SettingsStylePreview(
    model: SettingsPremiumHeroModel,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsPremiumHero(
                model = model,
                showBack = true,
                onBack = {},
                purchasing = false,
                onManage = {},
                onPurchase = {},
                onRestore = {},
            )
            SettingsSectionHeader(stringResource(R.string.settings_section_data))
            SettingsCard {
                SettingsNavRow(
                    icon = AppIcons.CloudStorage,
                    title = stringResource(R.string.drive_sync_title),
                    subtitle = if (model.phase == SettingsPremiumPhase.ACTIVE) {
                        stringResource(R.string.settings_row_drive_today, "09:41")
                    } else {
                        stringResource(R.string.settings_row_drive_subtitle)
                    },
                    mark = if (model.phase == SettingsPremiumPhase.ACTIVE) {
                        SettingsPremiumMark.CROWN
                    } else {
                        SettingsPremiumMark.LOCK
                    },
                    onClick = {},
                )
                SettingsNavRow(
                    icon = AppIcons.Backup,
                    title = stringResource(R.string.settings_row_backup),
                    subtitle = stringResource(R.string.settings_row_backup_subtitle),
                    onClick = {},
                )
                SettingsNavRow(
                    icon = AppIcons.TableChart,
                    title = stringResource(R.string.export_csv),
                    subtitle = stringResource(R.string.settings_row_csv_subtitle),
                    mark = if (model.phase == SettingsPremiumPhase.ACTIVE) {
                        SettingsPremiumMark.CROWN
                    } else {
                        SettingsPremiumMark.LOCK
                    },
                    onClick = {},
                )
            }
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                SettingsSwitchRow(
                    icon = AppIcons.Palette,
                    title = stringResource(R.string.settings_dynamic_color_title),
                    subtitle = stringResource(R.string.settings_dynamic_subtitle),
                    checked = false,
                    onCheckedChange = {},
                )
            }
        }
    }
}

@Preview(name = "Active light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsActiveLightPreview() {
    SettingsStylePreview(
        model = SettingsPremiumHeroModel(SettingsPremiumPhase.ACTIVE, "$2.99"),
        dark = false,
    )
}

@Preview(name = "Inactive light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsInactiveLightPreview() {
    SettingsStylePreview(
        model = SettingsPremiumHeroModel(SettingsPremiumPhase.FREE_MONTH, "$2.99"),
        dark = false,
    )
}

@Preview(name = "Active dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsActiveDarkPreview() {
    SettingsStylePreview(
        model = SettingsPremiumHeroModel(SettingsPremiumPhase.ACTIVE, "$2.99"),
        dark = true,
    )
}
