package com.truckerload.presentation.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.sync.FriendsLocationShareService

private val COLOR_PAST = 0xFF9CA3AF.toInt()
private val COLOR_REMAINING = 0xFF2563EB.toInt()

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
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    val viewModel: FriendsLiveMapViewModel = viewModel(
        factory = FriendsLiveMapViewModel.Factory(
            loadRepository = loadRepository,
            settingsDataStore = settings,
            authStore = authStore,
            friendsApi = friendsApi,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result.values.any { it }
    }

    LaunchedEffect(uiState.sharePathEnabled, hasLocationPermission, uiState.supabaseReady) {
        if (uiState.sharePathEnabled && uiState.supabaseReady && hasLocationPermission) {
            FriendsLocationShareService.start(context)
        } else {
            FriendsLocationShareService.stop(context)
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.friends_share_path_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.friends_share_path_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                Switch(
                    checked = uiState.sharePathEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                        viewModel.setSharePathEnabled(enabled)
                    },
                    colors = AppSwitchDefaults.colors(),
                )
            }

            if (!uiState.supabaseReady) {
                Text(
                    text = stringResource(R.string.friends_live_need_supabase),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.AccentPrimary,
                )
            }

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                if (uiState.isLoading && uiState.friends.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = tc.AccentPrimary,
                    )
                } else {
                    FriendsGoogleMap(
                        overlays = uiState.friends,
                        selectedFriendId = uiState.selectedFriendId,
                        onMarkerClick = viewModel::selectFriend,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.setShowOverlapsPanel(!uiState.showOverlapsPanel) }) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(stringResource(R.string.friends_overlap_button))
                }
            }

            if (uiState.showOverlapsPanel) {
                Text(
                    text = stringResource(R.string.friends_overlap_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                if (uiState.overlaps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.friends_overlap_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                } else {
                    uiState.overlaps.forEach { match ->
                        Text(
                            text = "${match.friendDisplayName}: ${match.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        OutlinedButton(onClick = {
                            viewModel.selectFriend(match.friendUserId)
                            viewModel.toggleShowPath(match.friendUserId)
                        }) {
                            Text(stringResource(R.string.friends_show_path))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.friends_list_title),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (uiState.friends.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.friends_list_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                        )
                    }
                }
                items(uiState.friends, key = { it.presence.userId }) { friend ->
                    val selected = friend.presence.userId == uiState.selectedFriendId
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = friend.presence.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) tc.AccentPrimary else tc.TextPrimary,
                        )
                        friend.route?.let { route ->
                            Text(
                                text = "${route.originLabel} → ${route.destinationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = friend.showPath,
                                onClick = { viewModel.toggleShowPath(friend.presence.userId) },
                                label = {
                                    Text(
                                        if (friend.showPath) {
                                            stringResource(R.string.friends_hide_path)
                                        } else {
                                            stringResource(R.string.friends_show_path)
                                        },
                                    )
                                },
                                colors = AppFilterChipDefaults.colors(),
                            )
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectFriend(friend.presence.userId) },
                                label = { Text(stringResource(R.string.friends_focus)) },
                                colors = AppFilterChipDefaults.colors(),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FriendsGoogleMap(
    overlays: List<FriendMapOverlay>,
    selectedFriendId: String?,
    onMarkerClick: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val map = mapView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_CREATE -> map.onCreate(null)
                Lifecycle.Event.ON_START -> map.onStart()
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                Lifecycle.Event.ON_STOP -> map.onStop()
                Lifecycle.Event.ON_DESTROY -> map.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }

    LaunchedEffect(overlays, selectedFriendId, mapView) {
        val map = mapView ?: return@LaunchedEffect
        map.getMapAsync { googleMap ->
            googleMap.clear()
            googleMap.uiSettings.isZoomControlsEnabled = true
            overlays.forEach { friend ->
                val pos = LatLng(friend.presence.latitude, friend.presence.longitude)
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(friend.presence.displayName)
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                if (friend.presence.userId == selectedFriendId) {
                                    BitmapDescriptorFactory.HUE_AZURE
                                } else {
                                    BitmapDescriptorFactory.HUE_ORANGE
                                },
                            ),
                        ),
                )
                marker?.tag = friend.presence.userId
                if (friend.showPath) {
                    if (friend.past.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friend.past.map { LatLng(it.lat, it.lng) })
                                .color(COLOR_PAST)
                                .width(10f),
                        )
                    }
                    if (friend.remaining.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friend.remaining.map { LatLng(it.lat, it.lng) })
                                .color(COLOR_REMAINING)
                                .width(10f),
                        )
                    }
                }
            }
            googleMap.setOnMarkerClickListener { marker ->
                (marker.tag as? String)?.let(onMarkerClick)
                false
            }
            val focus = overlays.firstOrNull { it.presence.userId == selectedFriendId }
                ?: overlays.firstOrNull()
            focus?.let {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.presence.latitude, it.presence.longitude),
                        6f,
                    ),
                )
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { mapView = it }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
