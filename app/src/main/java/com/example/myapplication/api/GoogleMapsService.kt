package com.example.myapplication.api
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsService {
    @GET("distance matrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("departure_time") departureTime: Long,
        @Query("traffic_model") trafficModel: String,
        @Query("key") apiKey: String
    ): Response<DistanceMatrixResponse>
}