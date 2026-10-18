package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
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
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalOledDarkEnabled
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.TruckerLoadTheme

@Composable
internal fun ProfileStylePreview(
    subscribed: Boolean,
    empty: Boolean,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
        val page = if (LocalOledDarkEnabled.current) SoftUiColors.BackgroundOled else LocalBrandPalette.current.page
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(page)
                .verticalScroll(rememberScrollState()),
        ) {
            ProfileHero(
                givenName = if (empty) "" else "Khaidar",
                familyName = if (empty) "" else "Safarov",
                avatarUrl = null,
                subscribed = subscribed,
                totalLoads = if (empty) 0 else 303,
                totalRevenue = if (empty) 0.0 else 683_700.0,
                averageRpm = if (empty) 0.0 else 2.70,
                redThreshold = 2.0,
                greenThreshold = 2.50,
                showMenu = true,
                showBack = false,
                onBack = {},
                onEditName = {},
                onAddLoad = {},
            )
            ProfileTotals(loads = if (empty) 0 else 303, miles = if (empty) 0 else 253_316)
            ProfileSectionHeader(stringResource(R.string.profile_account_title))
            ProfileCard {
                ProfileNavRow(
                    icon = AppIcons.Person,
                    title = stringResource(R.string.profile_name_section_title),
                    subtitle = if (empty) {
                        stringResource(R.string.profile_name_missing)
                    } else {
                        "Khaidar Safarov"
                    },
                    action = if (empty) stringResource(R.string.profile_name_add) else null,
                    onClick = {},
                )
                ProfileNavRow(
                    icon = AppIcons.CloudStorage,
                    title = stringResource(R.string.drive_sync_title),
                    subtitle = stringResource(R.string.profile_drive_never),
                    premiumLocked = !subscribed,
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileLightPreview() {
    ProfileStylePreview(subscribed = true, empty = false, dark = false)
}

@Preview(name = "Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEmptyPreview() {
    ProfileStylePreview(subscribed = false, empty = true, dark = false)
}

@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileDarkPreview() {
    ProfileStylePreview(subscribed = true, empty = false, dark = true)
}
