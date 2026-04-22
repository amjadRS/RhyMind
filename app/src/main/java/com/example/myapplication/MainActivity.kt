package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var travelTimeTextView: TextView
    private lateinit var distanceTextView: TextView
    private val apiKey = BuildConfig.MAPS_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize TextViews
        travelTimeTextView = findViewById(R.id.travelTimeTextView)
        distanceTextView = findViewById(R.id.distanceTextView)

        // Example locations
        val currentLocation = LatLng(31.9909945, 35.9216744) // Example: home
        val destination = LatLng(32.0163183, 35.811578) // Example: applied science university

        // Fetch travel time and distance
        fetchAndDisplayTravelData(currentLocation, destination)
    }

    private fun fetchAndDisplayTravelData(currentLocation: LatLng, destination: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            val travelData = fetchTravelTime(currentLocation, destination, apiKey)

            if (travelData != null) {
                val (travelTime, distance) = travelData

                
                runOnUiThread {
                    travelTimeTextView.text = "Travel Time: ${travelTime / 60} min"
                    distanceTextView.text = "Distance: ${distance / 1000} km"
                }
            } else {

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Unable to fetch travel data.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun fetchTravelTime(currentLocation: LatLng, destination: LatLng, apiKey: String): Pair<Int, Float>? {
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${currentLocation.latitude},${currentLocation.longitude}&destinations=${destination.latitude},${destination.longitude}&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            Log.d("API", "Making API request to fetch travel time... URL: $url")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.d("API", "API Response: $responseData")

                // Parse the response
                val jsonObject = responseData?.let { JSONObject(it) }
                val status = jsonObject?.getString("status")
                if (status != "OK") {
                    Log.e("API", "API Request failed with status: $status")
                    return null
                }

                val rows = jsonObject.getJSONArray("rows")
                val elements = rows.getJSONObject(0).getJSONArray("elements")
                val duration = elements.getJSONObject(0).getJSONObject("duration")
                val distance = elements.getJSONObject(0).getJSONObject("distance")

                val travelTime = duration.getInt("value") // In seconds
                val dist = distance.getInt("value") // In meters

                Log.d("API", "Travel Time: $travelTime, Distance: $dist")
                return Pair(travelTime, dist.toFloat())
            } else {
                Log.e("API", "Failed to fetch data: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e("API", "Error during API call: ${e.message}", e)
            return null
        }
    }
}
