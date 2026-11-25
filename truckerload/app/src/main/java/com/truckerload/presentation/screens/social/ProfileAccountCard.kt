package com.truckerload.presentation.screens.social

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.utils.AnalyticsOwnerName

@Composable
internal fun ProfileAccountCard(
    givenName: String,
    familyName: String,
    premiumPhase: ProfilePremiumPhase,
    premiumPrice: String?,
    onEditName: () -> Unit,
    onOpenSettings: () -> Unit,
    onPremium: () -> Unit,
) {
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
            icon = AppIcons.Backup,
            title = stringResource(R.string.settings_row_backup),
            subtitle = stringResource(R.string.settings_row_backup_subtitle),
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
