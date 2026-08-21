package com.truckerload.presentation.screens.social.friends.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.truckerload.R
import com.truckerload.domain.friends.FriendsRouteDisplay
import com.truckerload.domain.friends.FriendsRouteDisplayMode
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val COLOR_PAST = 0xFF9CA3AF.toInt()
internal val COLOR_REMAINING = 0xFF2563EB.toInt()

private fun LatLngPoint.toGms(): LatLng = LatLng(lat, lng)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenFriendsMapDialog(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint>,
    myPathRemaining: List<LatLngPoint>,
    selectedFriendId: String?,
    myLocation: LatLngPoint?,
    myAvatarUrl: String?,
    routeDisplayMode: FriendsRouteDisplayMode,
    showMyLocationLayer: Boolean,
    centerOnMeNonce: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onMarkerClick: (String) -> Unit,
    onCenterMe: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BentoGlassTheme.ScreenBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FriendsGoogleMap(
                    overlays = overlays,
                    myPathPast = myPathPast,
                    myPathRemaining = myPathRemaining,
                    selectedFriendId = selectedFriendId,
                    myLocation = myLocation,
                    myAvatarUrl = myAvatarUrl,
                    routeDisplayMode = routeDisplayMode,
                    showMyLocationLayer = showMyLocationLayer,
                    centerOnMeNonce = centerOnMeNonce,
                    interactive = true,
                    onMarkerClick = onMarkerClick,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(UiDimens.ToolbarTouchTarget),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.friends_map_close),
                        tint = tc.TextPrimary,
                    )
                }
                FloatingActionButton(
                    onClick = onCenterMe,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = tc.AccentPrimary,
                    contentColor = tc.Background,
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.friends_center_on_me),
                    )
                }
                Text(
                    text = stringResource(R.string.friends_map_legend) + " · " + stringResource(
                        if (routeDisplayMode == FriendsRouteDisplayMode.TRAVELED) {
                            R.string.friends_route_mode_traveled
                        } else {
                            R.string.friends_route_mode_remaining
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = tc.AccentPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsGoogleMap(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint> = emptyList(),
    myPathRemaining: List<LatLngPoint> = emptyList(),
    selectedFriendId: String?,
    myLocation: LatLngPoint?,
    myAvatarUrl: String? = null,
    routeDisplayMode: FriendsRouteDisplayMode = FriendsRouteDisplayMode.REMAINING,
    showMyLocationLayer: Boolean,
    centerOnMeNonce: Int,
    onMarkerClick: (String) -> Unit,
    interactive: Boolean = true,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val meLabel = stringResource(R.string.friends_me_marker)
    val destLabel = stringResource(R.string.friends_my_destination_marker)
    val friendFallback = stringResource(R.string.friends_map_friend_fallback)
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

    LaunchedEffect(
        overlays,
        myPathPast,
        myPathRemaining,
        selectedFriendId,
        myLocation,
        myAvatarUrl,
        routeDisplayMode,
        showMyLocationLayer,
        interactive,
        mapView,
    ) {
        val map = mapView ?: return@LaunchedEffect
        val density = context.resources.displayMetrics.density
        val avatarPx = (48 * density).toInt().coerceAtLeast(40)
        val mePhoto = withContext(Dispatchers.IO) { FriendMapMarkerBitmap.loadPhoto(myAvatarUrl, avatarPx) }
        val friendPhotos = withContext(Dispatchers.IO) {
            overlays.associate { friend ->
                friend.presence.userId to FriendMapMarkerBitmap.loadPhoto(friend.presence.avatarUrl, avatarPx)
            }
        }
        map.getMapAsync { googleMap ->
            googleMap.clear()
            googleMap.uiSettings.isZoomControlsEnabled = interactive
            googleMap.uiSettings.isScrollGesturesEnabled = interactive
            googleMap.uiSettings.isZoomGesturesEnabled = interactive
            googleMap.uiSettings.isRotateGesturesEnabled = interactive
            googleMap.uiSettings.isTiltGesturesEnabled = interactive
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            runCatching {
                // Custom "I am" avatar replaces the default Google blue dot.
                googleMap.isMyLocationEnabled = showMyLocationLayer && interactive && myLocation == null
            }
            myLocation?.let { me ->
                addPersonMarker(
                    map = googleMap,
                    position = me.toGms(),
                    title = meLabel,
                    density = density,
                    ringColor = FriendMapMarkerBitmap.RING_ME,
                    photo = mePhoto,
                    tag = null,
                    zIndex = 3f,
                )
            }
            val visiblePast = FriendsRouteDisplay.pastToDraw(routeDisplayMode, myPathPast)
            val visibleRemaining = FriendsRouteDisplay.remainingToDraw(routeDisplayMode, myPathRemaining)
            if (visiblePast.size >= 2) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(visiblePast.map { it.toGms() })
                        .color(COLOR_PAST)
                        .width(12f),
                )
            }
            if (visibleRemaining.size >= 2) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(visibleRemaining.map { it.toGms() })
                        .color(COLOR_REMAINING)
                        .width(12f),
                )
                visibleRemaining.lastOrNull()?.let { dest ->
                    val destIcon = BitmapDescriptorFactory.fromBitmap(
                        FriendMapMarkerBitmap.createDestination(density, destLabel),
                    )
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(dest.toGms())
                            .title(destLabel)
                            .icon(destIcon)
                            .anchor(0.5f, 1f)
                            .zIndex(0.5f),
                    )
                }
            }
            overlays.forEach { friend ->
                val pos = LatLng(friend.presence.latitude, friend.presence.longitude)
                val selected = friend.presence.userId == selectedFriendId
                addPersonMarker(
                    map = googleMap,
                    position = pos,
                    title = friend.presence.displayName.ifBlank { friendFallback },
                    density = density,
                    ringColor = if (selected) {
                        FriendMapMarkerBitmap.RING_FRIEND_SELECTED
                    } else {
                        FriendMapMarkerBitmap.RING_FRIEND
                    },
                    photo = friendPhotos[friend.presence.userId],
                    tag = friend.presence.userId,
                    zIndex = if (selected) 2f else 1f,
                )
                if (friend.showPath) {
                    val friendPast = FriendsRouteDisplay.pastToDraw(routeDisplayMode, friend.past)
                    val friendRemaining = FriendsRouteDisplay.remainingToDraw(routeDisplayMode, friend.remaining)
                    if (friendPast.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friendPast.map { it.toGms() })
                                .color(COLOR_PAST)
                                .width(10f),
                        )
                    }
                    if (friendRemaining.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friendRemaining.map { it.toGms() })
                                .color(COLOR_REMAINING)
                                .width(10f),
                        )
                    }
                }
            }
            googleMap.setOnMarkerClickListener { marker ->
                if (!interactive) return@setOnMarkerClickListener true
                (marker.tag as? String)?.let(onMarkerClick)
                false
            }
            val selected = overlays.firstOrNull { it.presence.userId == selectedFriendId }
            val routePoints = (visiblePast + visibleRemaining).map { it.toGms() }
            when {
                selected != null -> {
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(selected.presence.latitude, selected.presence.longitude),
                            8f,
                        ),
                    )
                }
                routePoints.size >= 2 && centerOnMeNonce == 0 -> {
                    val bounds = LatLngBounds.builder().also { b ->
                        routePoints.forEach(b::include)
                        myLocation?.let { b.include(it.toGms()) }
                        overlays.forEach { friend ->
                            b.include(LatLng(friend.presence.latitude, friend.presence.longitude))
                        }
                    }.build()
                    runCatching {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                    }.onFailure {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 7f))
                    }
                }
                myLocation != null && overlays.isNotEmpty() && centerOnMeNonce == 0 -> {
                    val bounds = LatLngBounds.builder().also { b ->
                        b.include(myLocation.toGms())
                        overlays.forEach { friend ->
                            b.include(LatLng(friend.presence.latitude, friend.presence.longitude))
                        }
                    }.build()
                    runCatching {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                    }.onFailure {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation.toGms(), 12f))
                    }
                }
                myLocation != null && centerOnMeNonce == 0 -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation.toGms(), 12f))
                }
                overlays.isNotEmpty() && centerOnMeNonce == 0 -> {
                    val f = overlays.first()
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(f.presence.latitude, f.presence.longitude),
                            6f,
                        ),
                    )
                }
            }
        }
    }

    LaunchedEffect(centerOnMeNonce, myLocation, mapView) {
        if (centerOnMeNonce == 0) return@LaunchedEffect
        val target = myLocation ?: return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        map.getMapAsync { googleMap ->
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target.toGms(), 14f))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(
                ctx,
                GoogleMapOptions()
                    .liteMode(!interactive)
                    .mapToolbarEnabled(false)
                    .zoomControlsEnabled(interactive)
                    .scrollGesturesEnabled(interactive)
                    .zoomGesturesEnabled(interactive)
                    .rotateGesturesEnabled(interactive)
                    .tiltGesturesEnabled(interactive),
            ).also { map ->
                mapView = map
                if (!interactive) {
                    map.isClickable = false
                    map.isFocusable = false
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun addPersonMarker(
    map: GoogleMap,
    position: LatLng,
    title: String,
    density: Float,
    ringColor: Int,
    photo: android.graphics.Bitmap?,
    tag: String?,
    zIndex: Float,
) {
    val icon = BitmapDescriptorFactory.fromBitmap(
        FriendMapMarkerBitmap.createPerson(density, title, ringColor, photo),
    )
    val marker = map.addMarker(
        MarkerOptions()
            .position(position)
            .title(title)
            .icon(icon)
            .anchor(0.5f, 1f)
            .zIndex(zIndex),
    )
    marker?.tag = tag
}
