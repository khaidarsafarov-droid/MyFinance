package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.presentation.icons.AppIcons

@Composable
internal fun SettingsBody(
    driveSubtitle: String,
    driveMark: SettingsPremiumMark,
    csvMark: SettingsPremiumMark,
    telegramMark: SettingsPremiumMark,
    languageValue: String,
    unitsValue: String,
    weekValue: String,
    rpmSubtitle: String,
    quietValue: String,
    themeMode: AppThemeMode,
    oledDark: Boolean,
    dynamicColor: Boolean,
    reduceMotion: Boolean,
    notifyMissingWeek: Boolean,
    notifyMaintenance: Boolean,
    settingsViewModel: SettingsViewModel,
    onDrive: () -> Unit,
    onBackup: () -> Unit,
    onCsv: () -> Unit,
    onLanguage: () -> Unit,
    onUnits: () -> Unit,
    onWeek: () -> Unit,
    onRpm: () -> Unit,
    onTelegram: () -> Unit,
    onQuiet: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(stringResource(R.string.settings_section_data))
        SettingsCard {
            SettingsNavRow(
                icon = AppIcons.CloudStorage,
                title = stringResource(R.string.drive_sync_title),
                subtitle = driveSubtitle,
                mark = driveMark,
                onClick = onDrive,
            )
            SettingsNavRow(
                icon = AppIcons.Backup,
                title = stringResource(R.string.settings_row_backup),
                subtitle = stringResource(R.string.settings_row_backup_subtitle),
                onClick = onBackup,
            )
            SettingsNavRow(
                icon = AppIcons.TableChart,
                title = stringResource(R.string.export_csv),
                subtitle = stringResource(R.string.settings_row_csv_subtitle),
                mark = csvMark,
                onClick = onCsv,
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
        SettingsCard {
            ThemeSettingsSection(
                selected = themeMode,
                oledDark = oledDark,
                dynamicColor = dynamicColor,
            )
            AccessibilitySettingsSection(reduceMotion = reduceMotion)
            FeedbackSettingsSection(settingsViewModel = settingsViewModel)
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_language))
        SettingsCard {
            SettingsNavRow(
                icon = AppIcons.Flag,
                title = stringResource(R.string.settings_language_title),
                subtitle = stringResource(R.string.settings_language_desc),
                value = languageValue,
                onClick = onLanguage,
            )
            SettingsNavRow(
                icon = AppIcons.Route,
                title = stringResource(R.string.settings_units_title),
                subtitle = stringResource(R.string.settings_units_desc),
                value = unitsValue,
                onClick = onUnits,
            )
            SettingsNavRow(
                icon = AppIcons.CalendarMonth,
                title = stringResource(R.string.settings_week_start_title),
                subtitle = stringResource(R.string.settings_row_week_subtitle),
                value = weekValue,
                onClick = onWeek,
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_loads))
        SettingsCard {
            SettingsNavRow(
                icon = AppIcons.Gauge,
                title = stringResource(R.string.settings_rpm_thresholds_title),
                subtitle = rpmSubtitle,
                onClick = onRpm,
            )
            SettingsNavRow(
                icon = AppIcons.Send,
                title = stringResource(R.string.settings_row_telegram),
                subtitle = stringResource(R.string.settings_row_telegram_subtitle),
                mark = telegramMark,
                onClick = onTelegram,
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
        SettingsCard {
            NotificationSettingsSection(
                notifyMissingWeek = notifyMissingWeek,
                notifyMaintenance = notifyMaintenance,
            )
            SettingsNavRow(
                icon = AppIcons.Schedule,
                title = stringResource(R.string.settings_quiet_hours_title),
                subtitle = stringResource(R.string.settings_quiet_hours_desc),
                value = quietValue,
                onClick = onQuiet,
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_security))
        SettingsCard {
            BiometricSettingsSection()
            SettingsNavRow(
                icon = AppIcons.PrivacyTip,
                title = stringResource(R.string.settings_row_privacy),
                subtitle = stringResource(R.string.settings_row_privacy_subtitle),
                onClick = onPrivacy,
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_about))
        SettingsCard {
            SettingsShareAppSection()
            SettingsInfoRow(
                icon = AppIcons.Info,
                title = stringResource(R.string.settings_row_version),
                subtitle = stringResource(R.string.settings_version_subtitle),
                value = BuildConfig.VERSION_NAME,
            )
        }
        Spacer(Modifier.height(12.dp))
        DeleteAccountSection()
    }
}
