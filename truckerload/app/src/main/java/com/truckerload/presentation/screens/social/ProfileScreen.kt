package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

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
