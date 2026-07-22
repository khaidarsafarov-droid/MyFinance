package com.truckerload.presentation.screens.social

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import com.truckerload.presentation.components.LocalOpenDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import java.util.Locale
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile
    val tc = LocalTruckColors.current
    val openDrawer = LocalOpenDrawer.current
    var showAvatarPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.avatarError) {
        uiState.avatarError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearAvatarError()
        }
    }

    ProfileAvatarPickerSheet(
        visible = showAvatarPicker,
        hasAvatar = !profile?.avatarUrl.isNullOrBlank(),
        onDismiss = { showAvatarPicker = false },
        onBitmapSelected = viewModel::uploadAvatar,
        onRemove = viewModel::removeAvatar,
    )

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    } else {
                        IconButton(onClick = openDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.common_menu), tint = tc.TextPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_profile))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.social_loading), color = tc.TextSecondary)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PremiumProfileHeader(
                    profile = profile,
                    isUploadingAvatar = uiState.isUploadingAvatar,
                    onAvatarClick = { showAvatarPicker = true },
                )
            }
            item { PremiumStatsRow(profile) }
            if (profile.badges.isNotEmpty()) {
                item { ProfileBadgesSection(profile) }
            }
            if (profile.about.isNotBlank()) {
                item { ProfileAboutSection(profile.about) }
            }
            if (profile.preferredRoutes.isNotEmpty() || profile.homeState.isNotBlank()) {
                item { ProfileTerritorySection(profile) }
            }
            if (profile.followers > 0 || profile.following > 0) {
                item { ProfileSocialSection(profile) }
            }
            if (profile.phoneNumber != null || profile.telegramUsername != null) {
                item { ProfileContactsSection(profile) }
            }
        }
    }
}

@Composable
private fun PremiumProfileHeader(
    profile: EnhancedDriverProfile,
    isUploadingAvatar: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
  Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(tc.AccentPrimary.copy(alpha = 0.55f), tc.SurfaceSecondary),
                    ),
                ),
        )
        BentoGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-36).dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileAvatar(
                        avatarUrl = profile.avatarUrl,
                        isUploading = isUploadingAvatar,
                        onClick = onAvatarClick,
                    )
                    Column {
                        Text(
                            profile.displayName.ifBlank { stringResource(R.string.profile_name_placeholder) },
                            style = AppTypography.CardTitle,
                            color = tc.TextPrimary,
                        )
                        val truckMeta = buildList {
                            if (profile.truckType != com.truckerload.domain.social.TruckType.OTHER) {
                                add("${profile.truckType.emoji} ${profile.truckType.label}")
                            }
                            if (profile.experienceYears > 0) {
                                add("${profile.experienceYears} ${stringResource(R.string.experience_years)}")
                            }
                        }
                        if (truckMeta.isNotEmpty()) {
                            Text(
                                truckMeta.joinToString(" · "),
                                style = AppTypography.Subtitle,
                                color = tc.TextSecondary,
                            )
                        }
                        val isRussian = Locale.getDefault().language.equals("ru", ignoreCase = true)
                        val locationLabel = CountryCatalog.byIso2(profile.homeState)
                            ?.let { "${it.iso2} · ${it.displayName(isRussian)}" }
                            ?: profile.homeState
                        if (locationLabel.isNotBlank() || profile.status != com.truckerload.domain.social.DriverStatus.OFFLINE) {
                            Text(
                                buildString {
                                    if (locationLabel.isNotBlank()) append("📍 $locationLabel")
                                    if (locationLabel.isNotBlank()) append(" · ")
                                    append(profile.status.label)
                                },
                                style = AppTypography.Subtitle,
                                color = tc.TextSecondary,
                            )
                        }
                    }
                }
                if (profile.ratingCount > 0) {
                    Text(
                        text = "⭐ ${"%.1f".format(profile.rating)} ★ (${profile.ratingCount}) · 🏅 ${profile.reputation} ${stringResource(R.string.social_reputation_short)}",
                        style = AppTypography.Subtitle,
                        color = tc.AccentPrimary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                profile.currentRoute?.takeIf { it.isNotBlank() }?.let { route ->
                    Text("🛣️ $route", style = AppTypography.Subtitle, color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumStatsRow(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_loads),
                value = profile.totalLoads.toString(),
                accent = tc.AccentPrimary,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_miles),
                value = "%,d".format(profile.totalMiles),
                accent = tc.AccentProfit,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_revenue),
                value = MoneyFormat.formatCurrency(profile.totalRevenue),
                accent = tc.AccentPrimary,
            )
            BentoGlassMetricCell(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.social_stat_rpm),
                value = MoneyFormat.formatCurrency(profile.averageRpm),
                accent = tc.AccentProfit,
            )
        }
    }
}

@Composable
private fun ProfileBadgesSection(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${stringResource(R.string.badges)} (${profile.badges.size})",
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = profile.badges.joinToString(" ") { it.icon },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            profile.badges.take(6).forEach { badge ->
                Text(
                    text = "${badge.icon} ${badge.name}",
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileAboutSection(about: String) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.about_me), style = AppTypography.CardTitle, color = tc.TextPrimary)
            Text(about, style = AppTypography.Subtitle, color = tc.TextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ProfileTerritorySection(profile: EnhancedDriverProfile) {
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
private fun ProfileSocialSection(profile: EnhancedDriverProfile) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${profile.followers}", style = AppTypography.CardTitle, color = tc.TextPrimary)
                Text(stringResource(R.string.social_followers), style = AppTypography.Subtitle, color = tc.TextSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${profile.following}", style = AppTypography.CardTitle, color = tc.TextPrimary)
                Text(stringResource(R.string.social_following), style = AppTypography.Subtitle, color = tc.TextSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${"%.0f".format(profile.onTimePercentage)}%", style = AppTypography.CardTitle, color = tc.AccentPrimary)
                Text(stringResource(R.string.social_on_time), style = AppTypography.Subtitle, color = tc.TextSecondary)
            }
        }
    }
}

@Composable
private fun ProfileContactsSection(profile: EnhancedDriverProfile) {
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
private fun ContactRow(
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
            .clickable(onClick = onClick),
    )
}
