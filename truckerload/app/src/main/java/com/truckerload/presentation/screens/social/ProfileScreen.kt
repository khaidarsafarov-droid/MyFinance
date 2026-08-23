package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.SoftActionChip
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.SoftEmptyFill
import com.truckerload.presentation.components.SoftTabletTwoPane
import com.truckerload.presentation.utils.useNavigationRail

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
    var showAvatarPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tabletChrome = useNavigationRail()

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

    SoftAppPageScaffold(
        title = stringResource(R.string.profile),
        modifier = modifier,
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        showPhoneMenu = !showBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            SoftActionChip(
                icon = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_profile),
                onClick = onEdit,
            )
        },
    ) { padding ->
        if (profile == null) {
            SoftEmptyFill(
                message = stringResource(R.string.social_loading),
                modifier = Modifier.padding(padding),
            )
            return@SoftAppPageScaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SoftTabletTwoPane(
                    start = {
                        PremiumProfileHeader(
                            profile = profile,
                            isUploadingAvatar = uiState.isUploadingAvatar,
                            onAvatarClick = { showAvatarPicker = true },
                            onNameClick = onEdit,
                        )
                    },
                    end = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PremiumStatsRow(profile)
                            ProfileAuthSyncSection()
                        }
                    },
                )
            }
            item {
                ProfileCompletionReminders(onFillProfessional = onEdit)
            }
            if (profile.badges.isNotEmpty()) {
                item { ProfileBadgesSection(profile) }
            }
            if (profile.about.isNotBlank()) {
                item { ProfileAboutSection(profile.about) }
            }
            if (profile.preferredRoutes.isNotEmpty() || profile.homeState.isNotBlank()) {
                item { ProfileTerritorySection(profile) }
            }
            if (profile.phoneNumber != null || profile.telegramUsername != null) {
                item { ProfileContactsSection(profile) }
            }
        }
    }
}
