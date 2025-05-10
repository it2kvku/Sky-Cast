package com.aco.skycast.data.repository

import com.aco.skycast.data.api.IpApiService
import com.aco.skycast.data.model.IpLocationResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class IpLocationRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://ip-api.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val ipApiService = retrofit.create(IpApiService::class.java)

    suspend fun getIpLocation(): Result<IpLocationResponse> {
        return try {
            val response = ipApiService.getIpLocation()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API returned status: ${body.status}"))
                }
            } else {
                Result.failure(Exception("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}