package com.aco.skycast.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Reverse geocodes coordinates to get a location name
 */
class LocationUtils {
    companion object {
        private const val TAG = "LocationUtils"

        /**
         * Converts coordinates into a location name (e.g., city, province/state, country)
         */
        suspend fun getLocationNameFromCoordinates(
            context: Context,
            latitude: Double,
            longitude: Double
        ): String {
            return withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())

                    // For Android 13+ (Tiramisu)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { continuation ->
                            try {
                                geocoder.getFromLocation(latitude, longitude, 3) { addresses ->
                                    if (addresses.isNotEmpty()) {
                                        val address = addresses[0]
                                        Log.d(TAG, "Found address: $address")
                                        continuation.resume(buildLocationString(address))
                                    } else {
                                        Log.d(TAG, "No addresses found for $latitude, $longitude")
                                        continuation.resume("Current Location")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Geocoding failed", e)
                                continuation.resume("Current Location")
                            }
                        }
                    }
                    // For older Android versions
                    else {
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(latitude, longitude, 3)
                            if (!addresses.isNullOrEmpty()) {
                                Log.d(TAG, "Found address: ${addresses[0]}")
                                buildLocationString(addresses[0])
                            } else {
                                Log.d(TAG, "No addresses found for $latitude, $longitude")
                                "Current Location"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Geocoding failed", e)
                            "Current Location"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in getLocationNameFromCoordinates", e)
                    "Current Location"
                }
            }
        }

        private fun buildLocationString(address: Address): String {
            // Log all available address components for debugging
            Log.d(TAG, "Address components: locality=${address.locality}, " +
                    "subAdminArea=${address.subAdminArea}, " +
                    "adminArea=${address.adminArea}, " +
                    "countryName=${address.countryName}")

            return when {
                // City + Country
                !address.locality.isNullOrEmpty() -> {
                    if (!address.countryName.isNullOrEmpty())
                        "${address.locality}, ${address.countryName}"
                    else
                        address.locality
                }
                // SubAdmin area (county) + Country
                !address.subAdminArea.isNullOrEmpty() -> {
                    if (!address.countryName.isNullOrEmpty())
                        "${address.subAdminArea}, ${address.countryName}"
                    else
                        address.subAdminArea
                }
                // Admin area (state/province) + Country
                !address.adminArea.isNullOrEmpty() -> {
                    if (!address.countryName.isNullOrEmpty())
                        "${address.adminArea}, ${address.countryName}"
                    else
                        address.adminArea
                }
                // Just country
                !address.countryName.isNullOrEmpty() -> address.countryName
                else -> "Current Location"
            }
        }
    }
}