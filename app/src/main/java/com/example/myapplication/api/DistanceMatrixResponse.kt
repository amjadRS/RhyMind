package com.example.myapplication.api

import com.google.android.gms.maps.model.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar


data class DistanceMatrixResponse(
    val rows: List<Row>
)

data class Row(
    val elements: List<Element>
)

data class Element(
    val distance: Value,
    val duration: Value,
    val duration_in_traffic: Value
)

data class Value(
    val text: String,
    val value: Int
)


suspend fun fetchTravelTime(
    currentLocation: LatLng,
    destination: LatLng,
    apiKey: String
): Pair<Int, Int>? {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(GoogleMapsService::class.java)
    val calendar = Calendar.getInstance()

// Set the date to January 15, 2025
    calendar.set(Calendar.YEAR, 2025)
    calendar.set(Calendar.MONTH, Calendar.JANUARY) // Months are 0-based in Calendar (January is 0)
    calendar.set(Calendar.DAY_OF_MONTH, 15)

// Set the time to 3:00 PM
    calendar.set(Calendar.HOUR_OF_DAY, 15)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val response = service.getDistanceMatrix(
        origins = "${currentLocation.latitude},${currentLocation.longitude}",
        destinations = "${destination.latitude},${destination.longitude}",
        departureTime = System.currentTimeMillis() / 1000, // Current time in seconds
        trafficModel = "best_guess",       //best_guess - optimistic - pessimistic
        apiKey = apiKey
    )

    if (response.isSuccessful) {
        val elements = response.body()?.rows?.firstOrNull()?.elements?.firstOrNull()
        val durationInTraffic = elements?.duration_in_traffic?.value // Time in seconds
        val distance = elements?.distance?.value // Distance in meters
        return Pair(durationInTraffic ?: 0, distance ?: 0)
    }
    return null
}