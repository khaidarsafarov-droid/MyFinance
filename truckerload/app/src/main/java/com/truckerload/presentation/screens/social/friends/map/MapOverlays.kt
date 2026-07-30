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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

internal val COLOR_PAST = 0xFF9CA3AF.toInt()
internal val COLOR_REMAINING = 0xFF2563EB.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenFriendsMapDialog(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint>,
    myPathRemaining: List<LatLngPoint>,
    selectedFriendId: String?,
    myLocation: LatLng?,
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
                    text = stringResource(R.string.friends_map_legend),
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
