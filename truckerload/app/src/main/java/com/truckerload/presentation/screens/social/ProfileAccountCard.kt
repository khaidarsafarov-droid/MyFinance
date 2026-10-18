package com.truckerload.presentation.screens.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.utils.AnalyticsOwnerName
import java.time.ZoneId

@Composable
internal fun ProfileAccountCard(
    givenName: String,
    familyName: String,
    subscribed: Boolean,
    premiumPhase: ProfilePremiumPhase,
    premiumPrice: String?,
    onEditName: () -> Unit,
    onOpenSettings: () -> Unit,
    onPremium: () -> Unit,
) {
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val user by LocalUserProfileStore.current.profile.collectAsStateWithLifecycle()
    val session = authStore.sessionOrNull()
    val googleEmail = user?.email?.takeIf { !user?.googleId.isNullOrBlank() }
        ?: session?.email?.takeIf { session.provider == AuthProvider.GOOGLE }
    val prefs = remember(session?.userId) {
        GoogleDriveBackupPrefs(context, session?.userId ?: AccountIds.LOCAL_DEV)
    }
    val syncStamp = ProfileHeroState.driveSyncStamp(prefs.lastSyncAt, ZoneId.systemDefault())
    val display = AnalyticsOwnerName.display(givenName, familyName)
    val nameMissing = display.isBlank()
    val premiumSubtitle = when (premiumPhase) {
        ProfilePremiumPhase.ACTIVE -> stringResource(R.string.profile_premium_active_line)
        ProfilePremiumPhase.FREE_MONTH -> stringResource(R.string.profile_premium_trial)
        ProfilePremiumPhase.PAID -> stringResource(R.string.premium_subscribe_paid, premiumPrice.orEmpty())
        ProfilePremiumPhase.UNKNOWN -> stringResource(R.string.profile_premium_inactive)
    }

    ProfileCard {
        ProfileNavRow(
            icon = AppIcons.Person,
            title = stringResource(R.string.profile_name_section_title),
            subtitle = if (nameMissing) stringResource(R.string.profile_name_missing) else display,
            action = if (nameMissing) stringResource(R.string.profile_name_add) else null,
            onClick = onEditName,
        )
        ProfileNavRow(
            icon = AppIcons.Email,
            title = stringResource(R.string.profile_google_account),
            subtitle = googleEmail ?: stringResource(R.string.profile_google_unlinked),
            onClick = null,
        )
        ProfileNavRow(
            icon = AppIcons.CloudStorage,
            title = stringResource(R.string.drive_sync_title),
            subtitle = syncStamp ?: stringResource(R.string.profile_drive_never),
            premiumLocked = !subscribed,
            onClick = onOpenSettings,
        )
        ProfileNavRow(
            icon = AppIcons.WorkspacePremium,
            title = stringResource(R.string.premium_title),
            subtitle = premiumSubtitle,
            goldIcon = true,
            onClick = onPremium,
        )
    }
}
