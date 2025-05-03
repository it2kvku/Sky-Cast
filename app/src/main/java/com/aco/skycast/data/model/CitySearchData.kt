package com.aco.skycast.data.model

data class CitySearchData(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val temperature: Double = 0.0
)