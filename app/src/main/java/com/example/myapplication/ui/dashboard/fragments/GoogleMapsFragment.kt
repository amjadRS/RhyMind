package com.example.myapplication.ui.dashboard.fragments

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentGoogleMapsBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleMapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var address: String

    private var _binding: FragmentGoogleMapsBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_LOCATION_PERMISSION = 1

    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        _binding = FragmentGoogleMapsBinding.inflate(inflater, container, false)

        val apiKey = BuildConfig.PLACES_API_KEY
        if(!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, apiKey)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autocompleteFragment = AutocompleteSupportFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.autoComplete, autocompleteFragment)
            .commit()

        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
        autocompleteFragment.setHint("Search for a place")
        autocompleteFragment.setActivityMode(AutocompleteActivityMode.OVERLAY)


        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener{
            override fun onPlaceSelected(place: Place) {
                val location = place.latLng!!
                //val name = place.displayName
                zoomOnPlace(location)
            }

            override fun onError(p0: Status) {
                Toast.makeText(requireContext(),"Error in search",Toast.LENGTH_SHORT).show()
            }

        })


        val mapFragment = SupportMapFragment.newInstance()

        childFragmentManager.beginTransaction()
            .replace(R.id.map, mapFragment)
            .commit()

        binding.setLocationButton.setOnClickListener{
            val source = arguments?.getString("source")
            if (source == "activity") {
                parentFragmentManager.setFragmentResult(
                    "locationRequestKey",
                    Bundle().apply { putString("location", address) }
                )
                requireActivity().supportFragmentManager.popBackStack()
            }
            else {
                val action =
                    GoogleMapsFragmentDirections.actionGoogleMapsFragmentToReminderFragment(address = address)
                findNavController().navigate(action)
            }
        }
        mapFragment.getMapAsync(this)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, enable location on the map
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isScrollGesturesEnabled = true
            }

        } else {

            requestLocationPermission()
        }
        googleMap.setOnMapClickListener { latLng ->
            googleMap.clear()  // Clear existing markers
            googleMap.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(latLng)
                    .title("Marked Location")
            )
            binding.setLocationButton.visibility = View.VISIBLE
            binding.hint.visibility = View.GONE
            val currentZoomLevel = googleMap.cameraPosition.zoom


            if (currentZoomLevel < 15f) {

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }


            getAddressFromLatLngAsync(latLng)

        }
    }

    private fun zoomOnPlace(location: LatLng){
        val newLocationZoom = CameraUpdateFactory.newLatLngZoom(location,16f)
        googleMap.animateCamera(newLocationZoom)
        googleMap.addMarker(
            com.google.android.gms.maps.model.MarkerOptions()
                .position(location)
                .title("Marked Location")
        )
        binding.hint.visibility = View.GONE
        binding.setLocationButton.visibility = View.VISIBLE

        getAddressFromLatLngAsync(location)
    }

    // Request location permissions
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    // Handle permission request result
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location on the map
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                }
            } else {
                Toast.makeText(requireContext(), "Permissions request has been denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAddressFromLatLngAsync(latLng: LatLng) {
        // Run Geocoder operation on background thread
        lifecycleScope.launch {
            val fetchedAddress = withContext(Dispatchers.IO) {
                getAddressFromLatLng(latLng) // Perform Geocoder operation on a background thread
            }


            address = "${latLng.latitude},${latLng.longitude}"
            Toast.makeText(requireContext(), "Address: $address", Toast.LENGTH_SHORT).show()
        }
    }

    // Get the address from latitude and longitude
    private fun getAddressFromLatLng(latLng: LatLng): String {
        val geocoder = Geocoder(requireContext())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        return addresses?.get(0)?.getAddressLine(0) ?: "Unknown Address"
    }
}
