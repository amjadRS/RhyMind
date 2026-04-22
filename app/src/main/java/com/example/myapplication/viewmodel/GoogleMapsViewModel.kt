package com.example.myapplication.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.GoogleMapsRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class GoogleMapsViewModel(private val repository: GoogleMapsRepository) : ViewModel() {

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> get() = _currentLocation

    private val fusedLocationClient = repository.fusedLocationClient

    fun fetchCurrentLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location == null) {
                    // Handle null location (e.g., location services may be off)
                    // You may want to request location updates here.
                } else {
                    withContext(Dispatchers.Main) {
                        _currentLocation.value = location
                    }
                }
            } catch (e: SecurityException) {
                _currentLocation.value = null
                // Optionally, inform the UI that location permission was not granted
            }
        }
    }
}