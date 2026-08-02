package com.truckerload.presentation.screens.social.friends.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
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
import com.truckerload.R
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

internal val COLOR_PAST = 0xFF9CA3AF.toInt()
internal val COLOR_REMAINING = 0xFF2563EB.toInt()
private val COLOR_ME = 0xFF22C55E.toInt()
private val COLOR_FRIEND = 0xFFF97316.toInt()
private val COLOR_SELECTED = 0xFF3B82F6.toInt()
private val COLOR_DEST = 0xFF2563EB.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenFriendsMapDialog(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint>,
    myPathRemaining: List<LatLngPoint>,
    selectedFriendId: String?,
    myLocation: LatLngPoint?,
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
                FriendsOsmMap(
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
fun FriendsOsmMap(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint> = emptyList(),
    myPathRemaining: List<LatLngPoint> = emptyList(),
    selectedFriendId: String?,
    myLocation: LatLngPoint?,
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
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
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
        map.overlays.clear()
        map.setMultiTouchControls(interactive)
        map.isClickable = interactive
        map.isFocusable = interactive

        if (showMyLocationLayer) {
            myLocation?.let { me ->
                map.overlays.add(
                    createMarker(
                        map = map,
                        point = me,
                        title = meLabel,
                        color = COLOR_ME,
                        tag = null,
                        onMarkerClick = onMarkerClick,
                        interactive = interactive,
                    ),
                )
            }
        }
        if (myPathPast.size >= 2) {
            map.overlays.add(createPolyline(myPathPast, COLOR_PAST, 12f))
        }
        if (myPathRemaining.size >= 2) {
            map.overlays.add(createPolyline(myPathRemaining, COLOR_REMAINING, 12f))
            myPathRemaining.lastOrNull()?.let { dest ->
                map.overlays.add(
                    createMarker(
                        map = map,
                        point = dest,
                        title = destLabel,
                        color = COLOR_DEST,
                        tag = null,
                        onMarkerClick = onMarkerClick,
                        interactive = interactive,
                    ),
                )
            }
        }
        overlays.forEach { friend ->
            val marker = createMarker(
                map = map,
                point = LatLngPoint(friend.presence.latitude, friend.presence.longitude),
                title = friend.presence.displayName,
                color = if (friend.presence.userId == selectedFriendId) COLOR_SELECTED else COLOR_FRIEND,
                tag = friend.presence.userId,
                onMarkerClick = onMarkerClick,
                interactive = interactive,
            )
            map.overlays.add(marker)
            if (friend.showPath) {
                if (friend.past.size >= 2) {
                    map.overlays.add(createPolyline(friend.past, COLOR_PAST, 10f))
                }
                if (friend.remaining.size >= 2) {
                    map.overlays.add(createPolyline(friend.remaining, COLOR_REMAINING, 10f))
                }
            }
        }

        val selected = overlays.firstOrNull { it.presence.userId == selectedFriendId }
        val routePoints = (myPathPast + myPathRemaining).map { GeoPoint(it.lat, it.lng) }
        when {
            selected != null -> {
                map.controller.animateTo(GeoPoint(selected.presence.latitude, selected.presence.longitude))
                map.controller.setZoom(8.0)
            }
            routePoints.size >= 2 && centerOnMeNonce == 0 -> {
                val bounds = BoundingBox.fromGeoPoints(routePoints)
                map.post { map.zoomToBoundingBox(bounds, true, 80) }
            }
            myLocation != null && centerOnMeNonce == 0 -> {
                map.controller.animateTo(GeoPoint(myLocation.lat, myLocation.lng))
                map.controller.setZoom(12.0)
            }
            overlays.isNotEmpty() && centerOnMeNonce == 0 -> {
                val f = overlays.first()
                map.controller.animateTo(GeoPoint(f.presence.latitude, f.presence.longitude))
                map.controller.setZoom(6.0)
            }
        }
        map.invalidate()
    }

    LaunchedEffect(centerOnMeNonce, myLocation, mapView) {
        if (centerOnMeNonce == 0) return@LaunchedEffect
        val target = myLocation ?: return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        map.controller.animateTo(GeoPoint(target.lat, target.lng))
        map.controller.setZoom(14.0)
        map.invalidate()
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(interactive)
                controller.setZoom(6.0)
                mapView = this
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun createPolyline(points: List<LatLngPoint>, color: Int, width: Float): Polyline =
    Polyline().apply {
        setPoints(points.map { GeoPoint(it.lat, it.lng) })
        outlinePaint.color = color
        outlinePaint.strokeWidth = width
        outlinePaint.isAntiAlias = true
    }

private fun createMarker(
    map: MapView,
    point: LatLngPoint,
    title: String,
    color: Int,
    tag: String?,
    onMarkerClick: (String) -> Unit,
    interactive: Boolean,
): Marker = Marker(map).apply {
    position = GeoPoint(point.lat, point.lng)
    this.title = title
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    icon = dotIcon(map.context, color)
    relatedObject = tag
    setOnMarkerClickListener { marker, _ ->
        if (!interactive) return@setOnMarkerClickListener true
        (marker.relatedObject as? String)?.let(onMarkerClick)
        true
    }
}

private fun dotIcon(context: android.content.Context, color: Int): BitmapDrawable {
    val sizePx = (18 * context.resources.displayMetrics.density).toInt().coerceAtLeast(18)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, paint)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, ring)
    return BitmapDrawable(context.resources, bitmap)
}
