package com.truckerload.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.backup.GoogleDriveBackupService
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.premium.PremiumPaywallDialog
import com.truckerload.presentation.premium.rememberPremiumAccess
import com.truckerload.presentation.theme.LocalOledDarkEnabled
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.screenEnterAnimation
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.DistanceUnitPolicy
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.utils.AppLanguageManager
import java.text.DateFormatSymbols
import java.time.ZoneId

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    showBack: Boolean = false,
    onOpenPrivacy: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenUnits: () -> Unit = {},
    motionSettled: Boolean = true,
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settingsSnapshot.collectAsStateWithLifecycle()
    val themeMode = settings.themeMode
    val oledDark = settings.oledDark
    val dynamicColor = settings.dynamicColor
    val reduceMotion = settings.reduceMotion
    val quietHoursEnabled = settings.quietHoursEnabled
    val quietHoursStart = settings.quietHoursStart
    val quietHoursEnd = settings.quietHoursEnd
    val notifyMissingWeek = settings.notifyMissingWeek
    val notifyMaintenance = settings.notifyMaintenance
    val loadWeekStartDay = settings.loadWeekStartDay
    val dieselWeekStartDay = settings.dieselWeekStartDay
    val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
    val premium by rememberPremiumAccess().status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val tablet = useNavigationRail()
    val motionReduced = LocalReduceMotion.current
    val page = if (LocalOledDarkEnabled.current) SoftUiColors.BackgroundOled else com.truckerload.presentation.theme.LocalBrandPalette.current.page

    var detail by remember { mutableStateOf<SettingsDetail?>(null) }
    var paywallOpen by remember { mutableStateOf(false) }
    var syncToken by remember { mutableIntStateOf(0) }
    var cardsShown by remember { mutableStateOf(motionSettled) }
    LaunchedEffect(motionSettled) {
        if (motionSettled) cardsShown = true
    }

    val lastSyncAt = remember(syncToken, context) {
        GoogleDriveBackupService.prefs(context).lastSyncAt
    }
    val syncMoment = remember(lastSyncAt) {
        SettingsPremiumState.syncMoment(lastSyncAt, System.currentTimeMillis(), ZoneId.systemDefault())
    }
    val driveSubtitle = when {
        syncMoment == null -> stringResource(R.string.settings_row_drive_never)
        syncMoment.isToday -> stringResource(R.string.settings_row_drive_today, syncMoment.clock)
        else -> stringResource(R.string.settings_row_drive_at, syncMoment.dateTime)
    }
    val languageValue = AppLanguageManager.getSupportedLanguages()
        .firstOrNull { it.code == AppLanguageManager.getCurrentLanguageCode() }
        ?.nativeName
        ?: stringResource(R.string.settings_language_en)
    val unitsValue = stringResource(
        if (
            DistanceUnitPolicy.resolve(
                settings.explicitDistanceUnit,
                AppLanguageManager.getCurrentLanguageCode(),
            ).isMetric
        ) {
            R.string.settings_units_kilometers
        } else {
            R.string.settings_units_miles
        },
    )
    val weekValue = remember(locale, loadWeekStartDay) {
        DateFormatSymbols(locale).shortWeekdays
            .getOrNull(loadWeekStartDay.calendarDay)
            .orEmpty()
            .replace(".", "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val quietValue = if (quietHoursEnabled) {
        stringResource(
            R.string.settings_quiet_range,
            SettingsPremiumState.hourLabel(quietHoursStart),
            SettingsPremiumState.hourLabel(quietHoursEnd),
        )
    } else {
        stringResource(R.string.settings_quiet_off)
    }
    val minRate = if (DisplayUnits.isMetric) {
        MeasurementUnits.displayRatePerDistance(thresholds.minProfit)
    } else {
        thresholds.minProfit
    }
    val targetRate = if (DisplayUnits.isMetric) {
        MeasurementUnits.displayRatePerDistance(thresholds.targetProfit)
    } else {
        thresholds.targetProfit
    }
    val rpmSubtitle = stringResource(
        R.string.settings_row_rpm_value,
        SettingsPremiumState.rpmAmount(minRate),
        SettingsPremiumState.rpmAmount(targetRate),
    )

    fun openPremium(feature: PremiumFeature, detailWhenUnlocked: SettingsDetail) {
        if (premium.allows(feature)) detail = detailWhenUnlocked else paywallOpen = true
    }

    Scaffold(
        containerColor = page,
        contentWindowInsets = if (tablet) WindowInsets.safeDrawing else WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalContentScroll(),
        ) {
            SettingsPremiumHeroHost(
                showBack = showBack && !tablet,
                onBack = onBack,
            )
            AnimatedVisibility(
                visible = cardsShown,
                enter = screenEnterAnimation(motionReduced),
            ) {
                SettingsBody(
                    driveSubtitle = driveSubtitle,
                    driveMark = SettingsPremiumState.mark(true, premium.allows(PremiumFeature.DRIVE)),
                    csvMark = SettingsPremiumState.mark(true, premium.allows(PremiumFeature.EXPORT)),
                    telegramMark = SettingsPremiumState.mark(true, premium.allows(PremiumFeature.TELEGRAM)),
                    languageValue = languageValue,
                    unitsValue = unitsValue,
                    weekValue = weekValue,
                    rpmSubtitle = rpmSubtitle,
                    quietValue = quietValue,
                    themeMode = themeMode,
                    oledDark = oledDark,
                    dynamicColor = dynamicColor,
                    reduceMotion = reduceMotion,
                    notifyMissingWeek = notifyMissingWeek,
                    notifyMaintenance = notifyMaintenance,
                    settingsViewModel = settingsViewModel,
                    onDrive = { openPremium(PremiumFeature.DRIVE, SettingsDetail.DRIVE) },
                    onBackup = { detail = SettingsDetail.DATA },
                    onCsv = { openPremium(PremiumFeature.EXPORT, SettingsDetail.DATA) },
                    onLanguage = onOpenLanguage,
                    onUnits = onOpenUnits,
                    onWeek = { detail = SettingsDetail.WEEK },
                    onRpm = { detail = SettingsDetail.RPM },
                    onTelegram = { openPremium(PremiumFeature.TELEGRAM, SettingsDetail.TELEGRAM) },
                    onQuiet = { detail = SettingsDetail.QUIET },
                    onPrivacy = { detail = SettingsDetail.PRIVACY },
                )
            }
            Spacer(Modifier.height(if (tablet) 24.dp else 16.dp))
        }
    }

    detail?.let { open ->
        SettingsDetailSheet(
            detail = open,
            loadsStart = loadWeekStartDay,
            dieselStart = dieselWeekStartDay,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            settingsViewModel = settingsViewModel,
            onOpenPrivacy = onOpenPrivacy,
            onDismiss = {
                if (open == SettingsDetail.DRIVE) syncToken += 1
                detail = null
            },
        )
    }
    if (paywallOpen) {
        PremiumPaywallDialog(onDismiss = { paywallOpen = false })
    }
}
