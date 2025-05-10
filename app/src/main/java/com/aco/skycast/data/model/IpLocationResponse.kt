package com.aco.skycast.data.model

data class IpLocationResponse(
    val status: String,
    val country: String,
    val regionName: String,
    val city: String,
    val timezone: String,
    val lat: Double,
    val lon: Double
)