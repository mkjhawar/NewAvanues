package com.augmentalis.photoavanue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.augmentalis.photoavanue.model.GpsMetadata

/**
 * Simplified dual-provider (GPS + Network) location provider for photo/video EXIF tagging.
 *
 * Ported from Avenue-Redux LocationService. Updates every 60 seconds.
 * Converts Android [Location] to KMP [GpsMetadata] for cross-platform consumption.
 */
class AndroidLocationProvider(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** Current GPS metadata, or null if no location fix yet. */
    var currentMetadata: GpsMetadata? = null
        private set

    /** Raw Android Location for CameraX metadata injection. */
    var currentLocation: Location? = null
        private set

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val networkListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    /**
     * Start location updates from GPS and Network providers.
     * @return true if at least one provider is available, false if no permission or no providers.
     */
    fun start(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = locationManager ?: return false
        var started = false

        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, 0f, gpsListener
                )
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { updateLocation(it) }
                started = true
            }
        } catch (_: SecurityException) { /* permission not granted */ }

        try {
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, 0f, networkListener
                )
                if (currentLocation == null) {
                    manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { updateLocation(it) }
                }
                started = true
            }
        } catch (_: SecurityException) { /* permission not granted */ }

        return started
    }

    /** Stop all location updates and release listeners. */
    fun stop() {
        locationManager?.removeUpdates(gpsListener)
        locationManager?.removeUpdates(networkListener)
    }

    private fun updateLocation(location: Location) {
        currentLocation = location
        currentMetadata = GpsMetadata(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timestamp = location.time
        )
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 60_000L // 60 seconds
    }
}
