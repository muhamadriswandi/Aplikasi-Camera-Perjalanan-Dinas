package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 999f,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = 0L,
    val isLocked: Boolean = false,
    val regency: String = "",
    val district: String = "",
    val village: String = "",
    val address: String = ""
) {
    fun toDmsString(): String {
        if (!isLocked) return "GPS Belum Terkunci"
        return "${formatDms(latitude, isLatitude = true)}, ${formatDms(longitude, isLatitude = false)}"
    }

    private fun formatDms(coordinate: Double, isLatitude: Boolean): String {
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        val absCoord = abs(coordinate)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60
        return String.format(Locale.US, "%d°%02d'%04.1f\"%s", degrees, minutes, seconds, direction)
    }
}

class GpsLocationHelper(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _gpsData = MutableStateFlow(GpsData())
    val gpsData: StateFlow<GpsData> = _gpsData.asStateFlow()

    private var isListening = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                processNewLocation(location)
            }
        }
    }

    private val fallbackListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            processNewLocation(location)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening) return
        isListening = true

        try {
            // Fused Location Request
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1f)
                .build()

            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())

            // Also get last known location immediately
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && !_gpsData.value.isLocked) {
                    processNewLocation(lastLoc)
                }
            }
        } catch (_: Exception) {
            // Fallback to LocationManager
            startFallbackLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFallbackLocationUpdates() {
        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000L,
                        1f,
                        fallbackListener,
                        Looper.getMainLooper()
                    )
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000L,
                        1f,
                        fallbackListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        try {
            fusedClient.removeLocationUpdates(fusedCallback)
            locationManager?.removeUpdates(fallbackListener)
        } catch (_: Exception) {
        }
    }

    private fun processNewLocation(location: Location) {
        val accuracy = location.accuracy
        val isLocked = location.latitude != 0.0 && location.longitude != 0.0 && accuracy < 100f

        val current = _gpsData.value
        _gpsData.value = current.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = accuracy,
            bearing = location.bearing,
            speed = location.speed,
            timestamp = location.time,
            isLocked = isLocked
        )

        // Try offline reverse geocoding if available
        resolveGeocodeAsync(location.latitude, location.longitude)
    }

    private fun resolveGeocodeAsync(lat: Double, lng: Double) {
        scope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val addr = addresses[0]
                                updateAddressDetails(
                                    regency = addr.subAdminArea ?: "",
                                    district = addr.locality ?: "",
                                    village = addr.subLocality ?: "",
                                    fullAddress = addr.getAddressLine(0) ?: ""
                                )
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            updateAddressDetails(
                                regency = addr.subAdminArea ?: "",
                                district = addr.locality ?: "",
                                village = addr.subLocality ?: "",
                                fullAddress = addr.getAddressLine(0) ?: ""
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback for offline environment
            }
        }
    }

    private fun updateAddressDetails(
        regency: String,
        district: String,
        village: String,
        fullAddress: String
    ) {
        _gpsData.value = _gpsData.value.copy(
            regency = regency,
            district = district,
            village = village,
            address = fullAddress
        )
    }
}
