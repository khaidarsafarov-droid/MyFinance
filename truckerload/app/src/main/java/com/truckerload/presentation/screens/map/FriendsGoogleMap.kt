package com.truckerload.presentation.screens.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.truckerload.R
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

private val COLOR_PAST = 0xFF9CA3AF.toInt()
private val COLOR_REMAINING = 0xFF2563EB.toInt()

@Composable
internal fun FriendsMapPreviewCard(
    mapExpanded: Boolean,
    myPathPast: List<LatLngPoint>,
    myPathRemaining: List<LatLngPoint>,
    myLocation: LatLng?,
    showMyLocationLayer: Boolean,
    isLoading: Boolean,
    myRouteSummary: String?,
    friendsCount: Int,
    onOpenMap: () -> Unit,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                // Avoid two live MapViews at once (preview + fullscreen).
                if (!mapExpanded) {
                    FriendsGoogleMap(
                        overlays = emptyList(),
                        myPathPast = myPathPast,
                        myPathRemaining = myPathRemaining,
                        selectedFriendId = null,
                        myLocation = myLocation,
                        showMyLocationLayer = showMyLocationLayer,
                        centerOnMeNonce = 0,
                        interactive = false,
                        onMarkerClick = {},
                    )
                }
                // Overlay captures the tap; MapView would otherwise eat it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onOpenMap),
                )
                if (isLoading && myLocation == null && !mapExpanded) {
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
            myRouteSummary?.let { summary ->
                Text(
                    text = stringResource(R.string.friends_my_route_label, summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.AccentPrimary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (friendsCount > 0) {
                Text(
                    text = stringResource(R.string.friends_online_count, friendsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun FriendsGoogleMap(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint> = emptyList(),
    myPathRemaining: List<LatLngPoint> = emptyList(),
    selectedFriendId: String?,
    myLocation: LatLng?,
    showMyLocationLayer: Boolean,
    centerOnMeNonce: Int,
    onMarkerClick: (String) -> Unit,
    interactive: Boolean = true,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val meLabel = stringResource(R.string.friends_me_marker)
    val destLabel = stringResource(R.string.friends_my_destination_marker)
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
        showMyLocationLayer,
        interactive,
        mapView,
    ) {
        val map = mapView ?: return@LaunchedEffect
        map.getMapAsync { googleMap ->
            googleMap.clear()
            googleMap.uiSettings.isZoomControlsEnabled = interactive
            googleMap.uiSettings.isScrollGesturesEnabled = interactive
            googleMap.uiSettings.isZoomGesturesEnabled = interactive
            googleMap.uiSettings.isRotateGesturesEnabled = interactive
            googleMap.uiSettings.isTiltGesturesEnabled = interactive
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            runCatching {
                googleMap.isMyLocationEnabled = showMyLocationLayer && interactive
            }
            myLocation?.let { me ->
                googleMap.addMarker(
                    MarkerOptions()
                        .position(me)
                        .title(meLabel)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
                )
            }
            // Own load corridor: gray = driven / blue = remaining (same colors as friends).
            if (myPathPast.size >= 2) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(myPathPast.map { LatLng(it.lat, it.lng) })
                        .color(COLOR_PAST)
                        .width(12f),
                )
            }
            if (myPathRemaining.size >= 2) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(myPathRemaining.map { LatLng(it.lat, it.lng) })
                        .color(COLOR_REMAINING)
                        .width(12f),
                )
                myPathRemaining.lastOrNull()?.let { dest ->
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(dest.lat, dest.lng))
                            .title(destLabel)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
                    )
                }
            }
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
                if (!interactive) return@setOnMarkerClickListener true
                (marker.tag as? String)?.let(onMarkerClick)
                false
            }
            val selected = overlays.firstOrNull { it.presence.userId == selectedFriendId }
            val routePoints = (myPathPast + myPathRemaining).map { LatLng(it.lat, it.lng) }
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
                        myLocation?.let(b::include)
                    }.build()
                    runCatching {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                    }.onFailure {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 7f))
                    }
                }
                myLocation != null && centerOnMeNonce == 0 -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12f))
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
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 14f))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { mapView = it }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
