package com.aco.skycast.data.remote
import com.aco.skycast.data.api.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApi {
    @GET("{latitude},{longitude}")
    suspend fun getWeatherByCoordinates(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("key") apiKey: String,
        @Query("unitGroup") unitGroup: String = "metric",
        @Query("include") include: String = "current,days,hours",
        @Query("contentType") contentType: String = "json"

    ): WeatherResponse

    @GET("{location}")
    suspend fun getWeather(
        @Path("location") location: String,
        @Query("key") key: String,
        @Query("unitGroup") unitGroup: String = "metric",
        @Query("include") include: String = "current,days,hours",
        @Query("contentType") contentType: String = "json"
    ): WeatherResponse
}