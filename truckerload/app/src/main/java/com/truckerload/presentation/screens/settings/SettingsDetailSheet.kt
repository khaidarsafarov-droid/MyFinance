package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.domain.week.WeekStartDay
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalTruckColors

internal enum class SettingsDetail {
    DRIVE,
    DATA,
    WEEK,
    RPM,
    TELEGRAM,
    QUIET,
    PRIVACY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDetailSheet(
    detail: SettingsDetail,
    loadsStart: WeekStartDay,
    dieselStart: WeekStartDay,
    quietHoursEnabled: Boolean,
    quietHoursStart: Int,
    quietHoursEnd: Int,
    settingsViewModel: SettingsViewModel,
    onOpenPrivacy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val brand = LocalBrandPalette.current
    val tc = LocalTruckColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = brand.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            when (detail) {
                SettingsDetail.DRIVE -> GoogleDriveSyncSection(tc = tc)
                SettingsDetail.DATA -> SettingsDataSection(settingsViewModel = settingsViewModel)
                SettingsDetail.WEEK -> WeekStartSettingsSection(
                    loadsStart = loadsStart,
                    dieselStart = dieselStart,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                SettingsDetail.RPM -> SettingsRpmForm()
                SettingsDetail.TELEGRAM -> TelegramSettingsSection()
                SettingsDetail.QUIET -> QuietHoursEditor(
                    quietHoursEnabled = quietHoursEnabled,
                    quietHoursStart = quietHoursStart,
                    quietHoursEnd = quietHoursEnd,
                )
                SettingsDetail.PRIVACY -> PrivacySettingsSection(
                    onOpenPrivacy = {
                        onDismiss()
                        onOpenPrivacy()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
