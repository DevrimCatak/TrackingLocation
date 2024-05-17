package com.devca.trackinglocation.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.devca.trackinglocation.models.LocationStorage
import com.devca.trackinglocation.models.ServiceManager
import com.devca.trackinglocation.services.LocationForegroundService
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
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        sharedPreferences = requireContext().getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        geocoder = Geocoder(requireActivity())

        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations) {
                    lastKnownLocation = LatLng(location.latitude, location.longitude)
                    LocationStorage.saveLocation(context!!, location.latitude, location.longitude)
                    updateLocationUI()
                    addMarkerToMap(lastKnownLocation!!)
                }
            }
        }

        buttonClicks()

        if (LocationStorage.isRouteTrackingEnabled(requireContext())) {
            startLocationUpdates()
        }

        return binding.root
    }

    private fun buttonClicks() {
        binding.stopTrackingButton.setOnClickListener {
            stopLocationUpdates()
            if (ServiceManager.isLocationServiceRunning) {
                val serviceIntent = Intent(requireContext(), LocationForegroundService::class.java)
                requireActivity().stopService(serviceIntent)
            }
            LocationStorage.setRouteTrackingEnabled(requireContext(), false)
        }

        binding.resetRouteButton.setOnClickListener {
            resetRoute()
        }

        binding.startTrackingButton.setOnClickListener {
            startLocationUpdates()
            if (!ServiceManager.isLocationServiceRunning) {
                val serviceIntent = Intent(requireContext(), LocationForegroundService::class.java)
                requireActivity().startService(serviceIntent)
            }
            LocationStorage.setRouteTrackingEnabled(requireContext(), true)
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
        restoreMarkers()
    }

    private fun restoreMarkers() {
        val locationList = LocationStorage.getLocationList(requireContext())
        for (locationString in locationList) {
            val (latitude, longitude) = locationString.split(",").map { it.toDouble() }
            val newLocation = LatLng(latitude, longitude)
            mMap.addMarker(MarkerOptions().position(newLocation))
        }
        updateLocationUI()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 100f
        }
    }

    private fun startLocationUpdates() {
        val foregroundServiceLocationPermission = Manifest.permission.FOREGROUND_SERVICE_LOCATION
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        if (ContextCompat.checkSelfPermission(requireContext(), foregroundServiceLocationPermission) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), fineLocationPermission) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), coarseLocationPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(foregroundServiceLocationPermission, fineLocationPermission, coarseLocationPermission),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
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
        LocationStorage.clearLocationList(requireContext())
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
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