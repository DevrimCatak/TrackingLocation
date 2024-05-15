package com.devca.trackinglocation.fragments

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devca.trackinglocation.R
import com.devca.trackinglocation.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback {
    lateinit var binding: FragmentMapBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder

    private var lastKnownLocation: LatLng? = null
    private val markers = mutableListOf<MarkerOptions>()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireActivity())

        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations) {
                    lastKnownLocation = LatLng(location.latitude, location.longitude)
                    updateLocationUI()
                    addMarkerToMap(lastKnownLocation!!)
                }
            }
        }

        buttonClicks()

        return binding.root
    }

    private fun buttonClicks() {
        binding.stopTrackingButton.setOnClickListener {
            stopLocationUpdates()
        }

        binding.resetRouteButton.setOnClickListener {
            resetRoute()
        }

        binding.startTrackingButton.setOnClickListener {
            startLocationUpdates()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener { marker ->
            val markerPosition = marker.position
            val address = getAddress(markerPosition.latitude, markerPosition.longitude)
            Toast.makeText(requireContext(), address, Toast.LENGTH_SHORT).show()
            true
        }
        updateLocationUI()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationUI() {
        if (lastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLocation!!, 15f))
        }
    }

    private fun addMarkerToMap(location: LatLng) {
        markers.add(MarkerOptions().position(location))
        mMap.addMarker(markers.last())
    }

    private fun resetRoute() {
        mMap.clear()
        markers.clear()
    }

    private fun getAddress(latitude: Double, longitude: Double): String {
        val addresses: List<Address>?
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address: String = addresses[0].getAddressLine(0)
                return address
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Adres bulunamadı"
    }

}