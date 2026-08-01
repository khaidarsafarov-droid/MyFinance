package com.truckerload.presentation.di

import androidx.compose.runtime.compositionLocalOf
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository

val LocalAuthStore = compositionLocalOf<AuthStore> { error("No AuthStore provided") }
val LocalAuthCredentialsStore = compositionLocalOf<AuthCredentialsStore> { error("No AuthCredentialsStore provided") }
val LocalUserProfileStore = compositionLocalOf<UserProfileStore> { error("No UserProfileStore provided") }
val LocalLoadRepository = compositionLocalOf<LoadRepository> { error("No LoadRepository provided") }
val LocalAiRepository = compositionLocalOf<AiRepository?> { null }
val LocalPaycheckRepository = compositionLocalOf<PaycheckRepository> { error("No PaycheckRepository provided") }
val LocalDieselRepository = compositionLocalOf<DieselRepository> { error("No DieselRepository provided") }
val LocalWeekRepository = compositionLocalOf<WeekRepository> { error("No WeekRepository provided") }
val LocalRpmThresholdsStore = compositionLocalOf<RpmThresholdsStore> { error("No RpmThresholdsStore provided") }
val LocalSelectedStateStore = compositionLocalOf<SelectedStateStore> { error("No SelectedStateStore provided") }
val LocalStatsSelectionStore = compositionLocalOf<StatsSelectionStore> { error("No StatsSelectionStore provided") }
val LocalWeeklyProfitGoalStore = compositionLocalOf<WeeklyProfitGoalStore> { error("No WeeklyProfitGoalStore provided") }
val LocalLastUsedDefaultsStore = compositionLocalOf<LastUsedDefaultsStore> { error("No LastUsedDefaultsStore provided") }
val LocalSettingsDataStore = compositionLocalOf<com.truckerload.data.preferences.SettingsDataStore> {
    error("No SettingsDataStore provided")
}
val LocalAnalyticsRepository = compositionLocalOf<com.truckerload.data.repository.AnalyticsRepository> {
    error("No AnalyticsRepository provided")
}
val LocalPhotoRepository = compositionLocalOf<com.truckerload.data.repository.PhotoRepository> {
    error("No PhotoRepository provided")
}
val LocalScanRepository = compositionLocalOf<com.truckerload.data.repository.ScanRepository> {
    error("No ScanRepository provided")
}
val LocalProfileRepository = compositionLocalOf<com.truckerload.data.repository.social.ProfileRepository> {
    error("No ProfileRepository provided")
}
val LocalSocialSyncCoordinator = compositionLocalOf<com.truckerload.data.repository.social.SocialSyncCoordinator> {
    error("No SocialSyncCoordinator provided")
}
val LocalVoiceRepository = compositionLocalOf<com.truckerload.data.repository.VoiceRepository> {
    error("No VoiceRepository provided")
}
val LocalMaintenanceRepository = compositionLocalOf<com.truckerload.data.repository.MaintenanceRepository> {
    error("No MaintenanceRepository provided")
}
