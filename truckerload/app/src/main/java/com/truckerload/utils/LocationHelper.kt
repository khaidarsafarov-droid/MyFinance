package com.truckerload.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.truckerload.domain.friends.LatLngPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class LocationHelper(private val context: Context) {

    private val geocodeCache = ConcurrentHashMap<String, LatLngPoint?>()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null
        try {
            val location = fusedLocationClient.lastLocation.await()
                ?: fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            location ?: return@withContext null
            val geo = reverseGeocode(location.latitude, location.longitude)
            LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                city = geo?.first ?: "",
                state = geo?.second ?: "",
                zipCode = geo?.third ?: "",
            )
        } catch (e: Exception) {
            android.util.Log.w("LocationHelper", "getCurrentLocation failed", e)
            null
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double): Triple<String, String, String>? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() ?: return null
            Triple(
                address.locality.orEmpty().ifBlank { address.subAdminArea.orEmpty() },
                address.adminArea.orEmpty(),
                address.postalCode.orEmpty(),
            )
        } catch (e: Exception) {
            android.util.Log.w("LocationHelper", "reverseGeocode failed", e)
            null
        }
    }

    /** Forward-geocode a city/state or full address for map routing (cached). */
    suspend fun geocodeAddress(query: String): LatLngPoint? = withContext(Dispatchers.IO) {
        val key = query.trim()
        if (key.isBlank()) return@withContext null
        if (geocodeCache.containsKey(key)) return@withContext geocodeCache[key]
        if (!Geocoder.isPresent()) {
            geocodeCache[key] = null
            return@withContext null
        }
        val point = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocationName(key, 1)?.firstOrNull()
            if (address != null) LatLngPoint(address.latitude, address.longitude) else null
        } catch (e: Exception) {
            android.util.Log.w("LocationHelper", "geocodeAddress failed for '$key'", e)
            null
        }
        geocodeCache[key] = point
        point
    }
}
