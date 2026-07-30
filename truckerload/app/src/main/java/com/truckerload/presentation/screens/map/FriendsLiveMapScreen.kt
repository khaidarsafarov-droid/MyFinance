package com.truckerload.presentation.screens.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.sync.FriendsLocationShareService
import com.truckerload.utils.LocationHelper
import kotlinx.coroutines.launch

/**
 * Friends live map — composition only. Map overlays, permission, manage panel, and
 * fullscreen dialog live in dedicated files under this package.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsLiveMapScreen(
    onBack: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val loadRepository = LocalLoadRepository.current
    val settings = LocalSettingsDataStore.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    val locationHelper = remember(context) { LocationHelper(context) }
    val viewModel: FriendsLiveMapViewModel = viewModel(
        factory = FriendsLiveMapViewModel.Factory(
            loadRepository = loadRepository,
            settingsDataStore = settings,
            authStore = authStore,
            userProfileStore = userProfileStore,
            friendsApi = friendsApi,
            locationHelper = locationHelper,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermission = rememberFriendsMapLocationPermission()
    val hasLocationPermission = locationPermission.hasPermission

    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var centerOnMeNonce by remember { mutableIntStateOf(0) }
    var mapExpanded by remember { mutableStateOf(false) }
    var manageExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val mapContent = uiState.toMapContent(mapExpanded, hasMyLocation = myLocation != null)

    suspend fun refreshMyLocation(): LatLng? {
        if (!hasLocationPermission) return null
        val loc = locationHelper.getCurrentLocation()
        val lat = loc?.latitude
        val lng = loc?.longitude
        if (lat == null || lng == null) return null
        viewModel.updateMyLocation(lat, lng)
        return LatLng(lat, lng).also { myLocation = it }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationPermission.requestPermission()
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            myLocation = null
            return@LaunchedEffect
        }
        refreshMyLocation()
    }

    LaunchedEffect(uiState.sharePathEnabled, hasLocationPermission, uiState.supabaseReady) {
        if (uiState.sharePathEnabled && uiState.supabaseReady && hasLocationPermission) {
            FriendsLocationShareService.start(context)
        } else {
            FriendsLocationShareService.stop(context)
        }
    }

    val chrome: FriendsMapChrome = if (mapExpanded) {
        FriendsMapChrome.Fullscreen(
            centerOnMeNonce = centerOnMeNonce,
            showMyLocationLayer = hasLocationPermission,
        )
    } else {
        FriendsMapChrome.Preview
    }

    when (val mode = chrome) {
        FriendsMapChrome.Preview -> Unit
        is FriendsMapChrome.Fullscreen -> FullscreenFriendsMapDialog(
            overlays = uiState.friends,
            myPathPast = uiState.myPathPast,
            myPathRemaining = uiState.myPathRemaining,
            selectedFriendId = uiState.selectedFriendId,
            myLocation = myLocation,
            showMyLocationLayer = mode.showMyLocationLayer,
            centerOnMeNonce = mode.centerOnMeNonce,
            isLoading = uiState.isLoading && uiState.friends.isEmpty() && myLocation == null,
            onDismiss = { mapExpanded = false },
            onMarkerClick = viewModel::selectFriend,
            onCenterMe = {
                if (!hasLocationPermission) {
                    locationPermission.requestPermission()
                    return@FullscreenFriendsMapDialog
                }
                scope.launch {
                    refreshMyLocation()
                    centerOnMeNonce += 1
                }
            },
        )
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        ForestScreenTitle(stringResource(R.string.friends_live_map_title))
                        Text(
                            text = stringResource(R.string.friends_live_map_subtitle),
                            style = AppTypography.Subtitle,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                    actionIconContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.friends_share_path_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.sharePathEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasLocationPermission) {
                                locationPermission.requestPermission()
                            }
                            viewModel.setSharePathEnabled(enabled)
                        },
                        colors = AppSwitchDefaults.colors(),
                    )
                }
                if (!hasLocationPermission) {
                    Text(
                        text = stringResource(R.string.friends_need_location_permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.AccentPrimary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            when (val content = mapContent) {
                is FriendsMapContent.Failed -> item {
                    Text(
                        text = content.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> Unit
            }

            item {
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            if (!mapExpanded) {
                                FriendsGoogleMap(
                                    overlays = emptyList(),
                                    myPathPast = uiState.myPathPast,
                                    myPathRemaining = uiState.myPathRemaining,
                                    selectedFriendId = null,
                                    myLocation = myLocation,
                                    showMyLocationLayer = hasLocationPermission,
                                    centerOnMeNonce = 0,
                                    interactive = false,
                                    onMarkerClick = {},
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { mapExpanded = true },
                            )
                            if (mapContent is FriendsMapContent.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = tc.AccentPrimary,
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.friends_map_tap_open),
                            style = MaterialTheme.typography.labelMedium,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        uiState.myRouteSummary?.let { summary ->
                            Text(
                                text = stringResource(R.string.friends_my_route_label, summary),
                                style = MaterialTheme.typography.labelSmall,
                                color = tc.AccentPrimary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (uiState.friends.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.friends_online_count, uiState.friends.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { manageExpanded = !manageExpanded },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.friends_manage_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (manageExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                        )
                    }
                }
            }

            if (manageExpanded) {
                item {
                    FriendsMapBottomSheet(
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
