package com.truckerload.presentation.screens.social.friends.map

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
import androidx.compose.runtime.DisposableEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.sync.FriendsLocationShareScheduler
import com.truckerload.utils.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsLiveMapScreen(
    onBack: () -> Unit = {},
    viewModel: FriendsMapViewModel = hiltViewModel(),
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermission = rememberFriendsMapLocationPermission()
    val hasLocationPermission = locationPermission.hasPermission

    var myLocation by remember { mutableStateOf<LatLngPoint?>(null) }
    var centerOnMeNonce by remember { mutableIntStateOf(0) }
    var mapExpanded by remember { mutableStateOf(false) }
    var manageExpanded by remember { mutableStateOf(false) }
    var addFriendExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshMyLocation(): LatLngPoint? {
        if (!hasLocationPermission) return null
        val loc = locationHelper.getCurrentLocation()
        val lat = loc?.latitude
        val lng = loc?.longitude
        if (lat == null || lng == null) return null
        viewModel.updateMyLocation(lat, lng)
        return LatLngPoint(lat, lng).also { myLocation = it }
    }

    LaunchedEffect(hasLocationPermission, uiState.locationBatterySaver) {
        if (!hasLocationPermission) {
            myLocation = null
            return@LaunchedEffect
        }
        // Foreground-only GPS: 5 s default, 10 s with battery saver. No background tracking.
        while (true) {
            refreshMyLocation()
            delay(viewModel.locationPollIntervalMs())
        }
    }

    DisposableEffect(uiState.sharePathEnabled, hasLocationPermission, uiState.supabaseReady) {
        val liveMap = uiState.sharePathEnabled && uiState.supabaseReady && hasLocationPermission
        if (liveMap) {
            FriendsLocationShareScheduler.onFriendsMapOpened(context)
        } else {
            FriendsLocationShareScheduler.sync(context)
        }
        onDispose {
            FriendsLocationShareScheduler.onFriendsMapClosed(context)
        }
    }

    if (mapExpanded) {
        FullscreenFriendsMapDialog(
            overlays = uiState.friends,
            myPathPast = uiState.myPathPast,
            myPathRemaining = uiState.myPathRemaining,
            selectedFriendId = uiState.selectedFriendId,
            myLocation = myLocation,
            myAvatarUrl = uiState.myAvatarUrl,
            routeDisplayMode = uiState.routeDisplayMode,
            showMyLocationLayer = hasLocationPermission,
            centerOnMeNonce = centerOnMeNonce,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.friends_route_truck_toggle),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.routeVehicleTruck,
                        onCheckedChange = viewModel::setRouteVehicleTruck,
                        colors = AppSwitchDefaults.colors(),
                    )
                }
                FriendsRouteModeSelector(
                    selected = uiState.routeDisplayMode,
                    onSelect = viewModel::setRouteDisplayMode,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.friends_battery_saver_toggle),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.locationBatterySaver,
                        onCheckedChange = viewModel::setLocationBatterySaver,
                        colors = AppSwitchDefaults.colors(),
                    )
                }
            }

            uiState.errorMessage?.let { err ->
                item {
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
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
                                    overlays = uiState.friends,
                                    myPathPast = uiState.myPathPast,
                                    myPathRemaining = uiState.myPathRemaining,
                                    selectedFriendId = uiState.selectedFriendId,
                                    myLocation = myLocation,
                                    myAvatarUrl = uiState.myAvatarUrl,
                                    routeDisplayMode = uiState.routeDisplayMode,
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
                            if (uiState.isLoading && myLocation == null && !mapExpanded) {
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
                        if (uiState.myPathRemaining.size >= 2) {
                            val eta = formatRouteEta(
                                distanceMeters = uiState.myRouteDistanceMeters,
                                durationSeconds = uiState.myRouteDurationSeconds,
                            )
                            if (eta != null) {
                                Text(
                                    text = eta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tc.TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (uiState.myRouteIsRoadNetwork) {
                                Text(
                                    text = stringResource(
                                        if (uiState.routeVehicleTruck) {
                                            R.string.friends_route_road_truck
                                        } else {
                                            R.string.friends_route_road_car
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tc.TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.friends_route_straight_fallback),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.friends_manage_section),
                                style = MaterialTheme.typography.titleSmall,
                                color = tc.TextPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { manageExpanded = !manageExpanded }
                                    .padding(vertical = 8.dp),
                            )
                            FriendsAddFriendHeaderButton(
                                expanded = addFriendExpanded,
                                onClick = {
                                    if (!addFriendExpanded) {
                                        manageExpanded = true
                                        addFriendExpanded = true
                                    } else {
                                        addFriendExpanded = false
                                    }
                                },
                            )
                            IconButton(onClick = { manageExpanded = !manageExpanded }) {
                                Icon(
                                    if (manageExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = tc.TextSecondary,
                                )
                            }
                        }
                    }
                    if (manageExpanded || addFriendExpanded) {
                        FriendsMapManageSection(
                            uiState = uiState,
                            viewModel = viewModel,
                            addFriendExpanded = addFriendExpanded,
                            onAddFriendExpandedChange = { expanded ->
                                addFriendExpanded = expanded
                                if (expanded) manageExpanded = true
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

internal fun formatRouteEta(distanceMeters: Long?, durationSeconds: Long?): String? {
    if (distanceMeters == null && durationSeconds == null) return null
    val parts = mutableListOf<String>()
    if (distanceMeters != null && distanceMeters > 0L) {
        val miles = distanceMeters / 1609.344
        parts += String.format("%.0f mi", miles)
    }
    if (durationSeconds != null && durationSeconds > 0L) {
        val totalMin = (durationSeconds + 59) / 60
        val hours = totalMin / 60
        val mins = totalMin % 60
        parts += if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
