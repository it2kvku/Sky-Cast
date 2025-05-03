package com.aco.skycast.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class GeocodingHelper(private val context: Context) {
    
    /**
     * Get a readable address from coordinates
     * @return The name of the location or null if geocoding fails
     */
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // For Android 13+ use the new callback API
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                // Try to get locality (city) first, then subAdminArea (county/district), 
                                // then adminArea (state), then country
                                val locationName = addresses[0].locality 
                                    ?: addresses[0].subAdminArea
                                    ?: addresses[0].adminArea
                                    ?: addresses[0].countryName
                                    ?: "$latitude,$longitude" // Fall back to coordinates
                                
                                continuation.resume(locationName)
                            } else {
                                continuation.resume(null)
                            }
                        }
                    }
                } else {
                    // For Android 12 and below
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    
                    if (addresses != null && addresses.isNotEmpty()) {
                        addresses[0].locality
                            ?: addresses[0].subAdminArea
                            ?: addresses[0].adminArea
                            ?: addresses[0].countryName
                            ?: "$latitude,$longitude" // Fall back to coordinates
                    } else {
                        null
                    }
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}
