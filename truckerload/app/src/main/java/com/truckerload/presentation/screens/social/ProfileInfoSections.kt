package com.truckerload.presentation.screens.social

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
internal fun ProfileAboutSection(about: String) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.about_me), style = AppTypography.CardTitle, color = tc.TextPrimary)
            Text(about, style = AppTypography.Subtitle, color = tc.TextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
internal fun ProfileTerritorySection(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.social_territory), style = AppTypography.CardTitle, color = tc.TextPrimary)
            val isRussian = Locale.getDefault().language.equals("ru", ignoreCase = true)
            val countryLabel = CountryCatalog.byIso2(profile.homeState)
                ?.let { "${it.displayName(isRussian)} (${it.iso2})" }
                ?: profile.homeState
            if (countryLabel.isNotBlank()) {
                Text(
                    "${stringResource(R.string.home_country)}: $countryLabel",
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (profile.preferredRoutes.isNotEmpty()) {
                Text(
                    "${stringResource(R.string.social_favorite_routes)}: ${profile.preferredRoutes.joinToString(", ")}",
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (profile.maxRadius > 0 && profile.maxRadius != 500) {
                Text(
                    "${stringResource(R.string.social_max_radius)}: ${profile.maxRadius} mi",
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun ProfileContactsSection(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.social_contacts), style = AppTypography.CardTitle, color = tc.TextPrimary)
            profile.phoneNumber?.let { phone ->
                ContactRow(
                    label = "📞 $phone",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                        }
                    },
                )
            }
            profile.telegramUsername?.let { username ->
                val handle = username.removePrefix("@")
                ContactRow(
                    label = "💬 @$handle",
                    onClick = {
                        runCatching {
                            val telegramUri = "https://t.me/$handle".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, telegramUri))
                        }
                    },
                )
            }
            profile.whatsappNumber?.let { whatsapp ->
                ContactRow(
                    label = "📱 $whatsapp",
                    onClick = {
                        runCatching {
                            val waUri = "https://wa.me/${whatsapp.filter { it.isDigit() }}".toUri()
                            context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun ContactRow(
    label: String,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Text(
        text = label,
        style = AppTypography.Subtitle,
        color = tc.AccentPrimary,
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
internal fun ProfileAuthSyncSection() {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val profile by userProfileStore.profile.collectAsStateWithLifecycle()
    val session = authStore.sessionOrNull()
    val prefs = remember(session?.userId) {
        GoogleDriveBackupPrefs(context, session?.userId ?: AccountIds.LOCAL_DEV)
    }
    val lastSync = prefs.lastSyncAt
    val dateFormat = remember {
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    }
    val syncLabel = if (lastSync > 0L) {
        stringResource(R.string.profile_last_drive_sync, dateFormat.format(java.util.Date(lastSync)))
    } else {
        stringResource(R.string.profile_last_drive_sync_never)
    }
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val googleLabel = profile?.email?.takeIf { !profile?.googleId.isNullOrBlank() }
                ?: session?.email?.takeIf { session.provider == AuthProvider.GOOGLE }
            if (!googleLabel.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.profile_google_linked, googleLabel),
                    style = AppTypography.Subtitle,
                    color = tc.TextPrimary,
                )
            }
            Text(
                text = syncLabel,
                style = AppTypography.Caption,
                color = tc.TextSecondary,
            )
        }
    }
}
