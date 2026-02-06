package com.example.gyme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.gyme.databinding.FragmentTrackerBinding
import com.google.android.gms.location.*

class TrackerFragment : Fragment() {

    private lateinit var binding: FragmentTrackerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isTracking = false
    private var totalDistance = 0.0
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Setup Callback GPS
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (lastLocation != null) {
                        // Hitung jarak dari titik terakhir ke titik sekarang
                        val distanceInMeters = lastLocation!!.distanceTo(location)
                        totalDistance += distanceInMeters / 1000.0 // Konversi ke KM
                    }
                    lastLocation = location
                    updateUI()
                }
            }
        }

        binding.btnStartRun.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                checkPermissionAndStart()
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        startTracking()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startTracking() else Toast.makeText(context, "Butuh izin lokasi!", Toast.LENGTH_SHORT).show()
    }

    private fun startTracking() {
        isTracking = true
        totalDistance = 0.0
        lastLocation = null
        binding.btnStartRun.text = "STOP LARI"
        binding.btnStartRun.backgroundTintList = requireContext().getColorStateList(android.R.color.holo_red_dark)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Toast.makeText(context, "GPS Tracking Dimulai...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopTracking() {
        isTracking = false
        binding.btnStartRun.text = "MULAI LARI"
        binding.btnStartRun.backgroundTintList = requireContext().getColorStateList(R.color.gym_accent)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Toast.makeText(context, "Lari Selesai! Total: ${"%.2f".format(totalDistance)} km", Toast.LENGTH_LONG).show()
    }

    private fun updateUI() {
        binding.tvDistance.text = "%.2f KM".format(totalDistance)
    }
}