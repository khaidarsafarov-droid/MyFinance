package com.truckerload.presentation.screens.settings

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AppThemeMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.screens.settings.ThemeSettingsSection
import com.truckerload.presentation.screens.settings.LanguageSettingsSection
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.SoftTabletTwoPane
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    showBack: Boolean = false,
    onOpenPrivacy: () -> Unit = {},
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
    val oledDark by settingsDataStore.oledDark.collectAsStateWithLifecycle(initialValue = false)
    val dynamicColor by settingsDataStore.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val reduceMotion by settingsDataStore.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val quietHoursEnabled by settingsDataStore.quietHoursEnabled.collectAsStateWithLifecycle(initialValue = false)
    val quietHoursStart by settingsDataStore.quietHoursStart.collectAsStateWithLifecycle(initialValue = 22)
    val quietHoursEnd by settingsDataStore.quietHoursEnd.collectAsStateWithLifecycle(initialValue = 7)
    val notifyMissingWeek by settingsDataStore.notifyMissingWeek.collectAsStateWithLifecycle(initialValue = true)
    val notifyMaintenance by settingsDataStore.notifyMaintenance.collectAsStateWithLifecycle(initialValue = true)
    val appLanguage by settingsDataStore.language.collectAsStateWithLifecycle(initialValue = com.truckerload.data.preferences.AppLanguage.RU)
    val loadWeekStartDay by settingsDataStore.loadWeekStartDay.collectAsStateWithLifecycle(
        initialValue = com.truckerload.domain.week.WeekStartDay.DEFAULT,
    )
    val dieselWeekStartDay by settingsDataStore.dieselWeekStartDay.collectAsStateWithLifecycle(
        initialValue = com.truckerload.domain.week.WeekStartDay.DEFAULT,
    )
    val tc = LocalTruckColors.current
    val store = LocalRpmThresholdsStore.current
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val thresholds by store.thresholds.collectAsStateWithLifecycle()
    var minInput by remember(thresholds) { mutableStateOf(thresholds.minProfit.toString()) }
    var targetInput by remember(thresholds) { mutableStateOf(thresholds.targetProfit.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val tabletChrome = useNavigationRail()
    SoftAppPageScaffold(
        title = stringResource(R.string.settings_title),
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalContentScroll()
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
        ) {
            SoftTabletTwoPane(
                start = {
                    Column {
                        ThemeSettingsSection(
                            selected = themeMode,
                            oledDark = oledDark,
                            dynamicColor = dynamicColor,
                        )
                        AccessibilitySettingsSection(reduceMotion = reduceMotion)
                        LanguageSettingsSection(selected = appLanguage)
                        WeekStartSettingsSection(
                            loadsStart = loadWeekStartDay,
                            dieselStart = dieselWeekStartDay,
                        )
                        FeedbackSettingsSection(settingsViewModel = settingsViewModel)
                    }
                },
                end = {
                    Column {
                        BiometricSettingsSection()
                        PrivacySettingsSection(onOpenPrivacy = onOpenPrivacy)
                        NotificationSettingsSection(
                            quietHoursEnabled = quietHoursEnabled,
                            quietHoursStart = quietHoursStart,
                            quietHoursEnd = quietHoursEnd,
                            notifyMissingWeek = notifyMissingWeek,
                            notifyMaintenance = notifyMaintenance,
                        )
                        TelegramSettingsSection()
                    }
                },
            )

            BentoGlassSection(title = stringResource(R.string.settings_rpm_thresholds_title)) {
                RpmColorLegend(compact = true)
                val fieldColors = AppTextFieldDefaults.outlined()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { minInput = it; error = null },
                        label = { Text(stringResource(R.string.settings_red_threshold_short)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it; error = null },
                        label = { Text(stringResource(R.string.settings_green_threshold_short)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = fieldColors,
                    )
                }
                error?.let { err ->
                    Text(
                        text = err,
                        color = tc.AccentExpense,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        val min = minInput.replace(",", ".").toDoubleOrNull()
                        val target = targetInput.replace(",", ".").toDoubleOrNull()
                        when {
                            min == null -> error = context.getString(R.string.settings_red_threshold_error)
                            target == null -> error = context.getString(R.string.settings_green_threshold_error)
                            else -> store.save(min, target)
                                .onSuccess {
                                    error = null
                                    android.widget.Toast.makeText(context, context.getString(R.string.settings_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { error = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text(stringResource(R.string.settings_rpm_save))
                }
            }

            SettingsDataSection(settingsViewModel = settingsViewModel)

            GoogleDriveSyncSection(tc = tc)

            SettingsShareAppSection()

            DeleteAccountSection()

            Text(
                text = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
