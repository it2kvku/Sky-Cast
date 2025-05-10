package com.aco.skycast.data.api

import com.aco.skycast.data.model.IpLocationResponse
import retrofit2.Response
import retrofit2.http.GET

interface IpApiService {
    @GET("json")
    suspend fun getIpLocation(): Response<IpLocationResponse>
}