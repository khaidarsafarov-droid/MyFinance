package com.truckerload.presentation.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle
import com.truckerload.R
import com.truckerload.presentation.components.StateRating
import com.truckerload.presentation.components.getStateDisplayName
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppTypography
import org.json.JSONObject
import java.util.Locale

private val STATE_NAME_TO_ABBR = mapOf(
    "Alabama" to "AL", "Alaska" to "AK", "Arizona" to "AZ", "Arkansas" to "AR",
    "California" to "CA", "Colorado" to "CO", "Connecticut" to "CT", "Delaware" to "DE",
    "District of Columbia" to "DC", "Florida" to "FL", "Georgia" to "GA", "Hawaii" to "HI",
    "Idaho" to "ID", "Illinois" to "IL", "Indiana" to "IN", "Iowa" to "IA", "Kansas" to "KS",
    "Kentucky" to "KY", "Louisiana" to "LA", "Maine" to "ME", "Maryland" to "MD",
    "Massachusetts" to "MA", "Michigan" to "MI", "Minnesota" to "MN", "Mississippi" to "MS",
    "Missouri" to "MO", "Montana" to "MT", "Nebraska" to "NE", "Nevada" to "NV",
    "New Hampshire" to "NH", "New Jersey" to "NJ", "New Mexico" to "NM", "New York" to "NY",
    "North Carolina" to "NC", "North Dakota" to "ND", "Ohio" to "OH", "Oklahoma" to "OK",
    "Oregon" to "OR", "Pennsylvania" to "PA", "Rhode Island" to "RI", "South Carolina" to "SC",
    "South Dakota" to "SD", "Tennessee" to "TN", "Texas" to "TX", "Utah" to "UT",
    "Vermont" to "VT", "Virginia" to "VA", "Washington" to "WA", "West Virginia" to "WV",
    "Wisconsin" to "WI", "Wyoming" to "WY", "Puerto Rico" to "PR"
)

private val COLOR_GOOD = 0xFF4ADE80.toInt()
private val COLOR_BAD = 0xFFF87171.toInt()
private val COLOR_NEUTRAL = 0xFFFDE047.toInt()
private val COLOR_NO_DATA = 0xFFE2E8F0.toInt()
private val COLOR_BORDER = 0xFF000000.toInt()
private val COLOR_SELECTED_SHADOW = 0xFF78838F.toInt()
private val STROKE_WIDTH_NORMAL = 2f
private val STROKE_WIDTH_SELECTED = 5f

private val MAP_STYLE = """[
  {"featureType":"poi","elementType":"labels","stylers":[{"visibility":"off"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"water","elementType":"geometry.fill","stylers":[{"color":"#d3d3d3"}]},
  {"featureType":"landscape","elementType":"geometry.fill","stylers":[{"color":"#f5f5f5"}]}
]"""

@Composable
fun GoogleMapsHeatmapCard(
    metrics: List<USStateMetric>,
    selectedCode: String,
    refreshing: Boolean,
    onStateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val geoJson = remember { loadGeoJsonFromRaw(context) }
    val byCode = metrics.associateBy { it.code }
    val selected = if (selectedCode.isNotBlank()) byCode[selectedCode] else null

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                var mapView by remember { mutableStateOf<MapView?>(null) }
                var geoJsonLayer by remember { mutableStateOf<GeoJsonLayer?>(null) }
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(metrics, selectedCode, geoJsonLayer) {
                    geoJsonLayer?.let { layer ->
                        applyStateStyles(layer, metrics.associateBy { it.code }, selectedCode)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> mapView?.onStart()
                            Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                            Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                            Lifecycle.Event.ON_STOP -> mapView?.onStop()
                            Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            onCreate(null)
                            mapView = this
                            // MapView needs onStart/onResume to load tiles; call immediately if already started
                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                onStart()
                                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    onResume()
                                }
                            }
                            getMapAsync { googleMap ->
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.5, -98.35), 4f))
                                googleMap.setMapStyle(MapStyleOptions(MAP_STYLE))
                                geoJson?.let { json ->
                                    try {
                                        val layer = GeoJsonLayer(googleMap, json)
                                        layer.addLayerToMap()
                                        geoJsonLayer = layer
                                        applyStateStyles(layer, byCode, selectedCode)
                                        layer.setOnFeatureClickListener { feature ->
                                            val name = feature.getProperty("name") ?: return@setOnFeatureClickListener
                                            val code = STATE_NAME_TO_ABBR[name] ?: name
                                            onStateSelected(code)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.w("GoogleMapsHeatmap", "GeoJSON layer failed", e)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(COLOR_GOOD), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.map_rating_good),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(COLOR_BAD), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.map_rating_low),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(COLOR_NEUTRAL), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Нейтрально",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(COLOR_NO_DATA), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Нет данных",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCode.isBlank(),
                    onClick = { onStateSelected("") },
                    label = { Text(stringResource(R.string.stats_reset_filters)) },
                    colors = AppFilterChipDefaults.colors(),
                )
                metrics.sortedByDescending { it.revenue }.take(6).map { it.code }.forEach { code ->
                    FilterChip(
                        selected = code == selectedCode,
                        onClick = { onStateSelected(code) },
                        label = { Text(code) },
                        colors = AppFilterChipDefaults.stateColors(),
                    )
                }
            }
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 4 }
            ) {
                selected?.let { state ->
                    val rpmStr = if (state.trips > 0)
                        String.format(Locale.getDefault(), "%.2f", state.revenuePerMile) else "—"
                    val grossStr = String.format(Locale.getDefault(), "%,.0f", state.revenue)
                    val ratingLabel = when (state.rating) {
                        StateRating.GOOD -> stringResource(R.string.map_rating_good)
                        StateRating.BAD -> stringResource(R.string.map_rating_bad)
                        StateRating.NEUTRAL -> stringResource(R.string.map_rating_neutral)
                        StateRating.NO_DATA -> stringResource(R.string.map_rating_no_data)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.map_state_trips,
                                    getStateDisplayName(state.code),
                                    state.code,
                                    state.trips,
                                ),
                                style = AppTypography.CardTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.map_rpm_per_mile, rpmStr),
                                modifier = Modifier.padding(top = 4.dp),
                                style = AppTypography.Body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.map_gross_label, grossStr),
                                modifier = Modifier.padding(top = 2.dp),
                                style = AppTypography.Body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Рейтинг: $ratingLabel",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun loadGeoJsonFromRaw(context: Context): JSONObject? {
    return try {
        val json = context.resources.openRawResource(R.raw.us_states).bufferedReader().readText()
        JSONObject(json)
    } catch (_: Exception) {
        null
    }
}

private fun applyStateStyles(
    layer: GeoJsonLayer,
    byCode: Map<String, USStateMetric>,
    selectedCode: String
) {
    for (feature in layer.features) {
        val name = feature.getProperty("name") ?: continue
        val code = STATE_NAME_TO_ABBR[name] ?: name
        val metric = byCode[code]
        val rating = metric?.rating ?: StateRating.NO_DATA
        val isSelected = code == selectedCode
        val fillColor = when (rating) {
            StateRating.GOOD -> COLOR_GOOD
            StateRating.BAD -> COLOR_BAD
            StateRating.NEUTRAL -> COLOR_NEUTRAL
            StateRating.NO_DATA -> COLOR_NO_DATA
        }
        val style = GeoJsonPolygonStyle().apply {
            setFillColor(fillColor)
            setStrokeColor(if (isSelected) COLOR_SELECTED_SHADOW else COLOR_BORDER)
            setStrokeWidth(if (isSelected) STROKE_WIDTH_SELECTED else STROKE_WIDTH_NORMAL)
            if (isSelected) setZIndex(10f)
        }
        feature.setPolygonStyle(style)
    }
}
