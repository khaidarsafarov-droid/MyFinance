package com.truckerload.presentation.screens.social

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.backup.GoogleDriveBackupPrefs
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.domain.friends.NicknameValidator
import com.truckerload.domain.geo.CountryCatalog
import java.util.Locale
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel(),
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            item { ProfileNicknameSection() }
            item { ProfileAuthSyncSection() }
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
private fun ProfileNicknameSection() {
    val tc = LocalTruckColors.current
    val userProfileStore = LocalUserProfileStore.current
    val authStore = LocalAuthStore.current
    val authProfile by userProfileStore.profile.collectAsStateWithLifecycle()
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    var draft by remember(authProfile?.nickname) { mutableStateOf(authProfile?.nickname.orEmpty()) }
    var editing by remember { mutableStateOf(authProfile?.nickname.isNullOrBlank()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentNick = authProfile?.nickname.orEmpty()

    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.friends_my_nickname_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.friends_my_nickname_hint),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
            if (!editing && currentNick.isNotBlank()) {
                Text(
                    text = stringResource(R.string.friends_my_nickname_current, currentNick),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.AccentPrimary,
                )
                TlOutlinedButton(
                    onClick = {
                        draft = currentNick
                        editing = true
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.friends_change_nickname_button))
                }
            } else {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        message = null
                    },
                    label = { Text(stringResource(R.string.friends_nickname_label)) },
                    placeholder = { Text(stringResource(R.string.friends_nickname_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = AppTextFieldDefaults.outlined(),
                )
                TlButton(
                    onClick = {
                        val handle = NicknameValidator.sanitizeOrNull(draft)
                        if (handle == null) {
                            message = "invalid"
                            return@TlButton
                        }
                        busy = true
                        scope.launch {
                            val result = if (friendsApi.isConfigured()) {
                                friendsApi.upsertMyNickname(handle, authProfile?.displayName)
                            } else {
                                Result.success(Unit)
                            }
                            busy = false
                            val err = result.exceptionOrNull()?.message
                            if (result.isFailure &&
                                err != SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                            ) {
                                message = err ?: "error"
                                return@launch
                            }
                            // Persist locally even when cloud schema is missing so the
                            // nickname still shows on this device until SQL is applied.
                            val current = userProfileStore.profile.value
                            if (current != null) {
                                userProfileStore.saveProfile(current.copy(nickname = handle))
                            }
                            message = if (
                                err == SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                            ) {
                                SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                            } else {
                                "saved"
                            }
                            editing = err != SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (currentNick.isBlank()) {
                            stringResource(R.string.friends_add_nickname_button)
                        } else {
                            stringResource(R.string.friends_nickname_save)
                        },
                    )
                }
                if (currentNick.isNotBlank()) {
                    TlOutlinedButton(
                        onClick = {
                            draft = currentNick
                            editing = false
                            message = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
            val feedback = message
            when (feedback) {
                "invalid" -> Text(
                    stringResource(R.string.friends_nickname_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.Subtitle,
                )
                "saved" -> Text(
                    stringResource(R.string.friends_nickname_saved),
                    color = tc.AccentPrimary,
                    style = AppTypography.Subtitle,
                )
                SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING -> Text(
                    stringResource(R.string.friends_nickname_schema_missing),
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.Subtitle,
                )
                null -> Unit
                else -> Text(feedback, color = MaterialTheme.colorScheme.error, style = AppTypography.Subtitle)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (locationLabel.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = tc.TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = buildString {
                                        if (locationLabel.isNotBlank()) append(locationLabel)
                                        if (locationLabel.isNotBlank()) append(" · ")
                                        append(profile.status.label.substringAfter(' ', profile.status.label))
                                    },
                                    style = AppTypography.Subtitle,
                                    color = tc.TextSecondary,
                                )
                            }
                        }
                    }
                }
                if (profile.ratingCount > 0) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${"%.1f".format(profile.rating)} (${profile.ratingCount}) ·",
                            style = AppTypography.Subtitle,
                            color = tc.AccentPrimary,
                        )
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = tc.AccentPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${profile.reputation} ${stringResource(R.string.social_reputation_short)}",
                            style = AppTypography.Subtitle,
                            color = tc.AccentPrimary,
                        )
                    }
                }
                profile.currentRoute?.takeIf { it.isNotBlank() }?.let { route ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(route, style = AppTypography.Subtitle, color = tc.TextSecondary)
                    }
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

@Composable
private fun ProfileAuthSyncSection() {
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
