package com.example.gyme

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gyme.adapter.RunHistoryAdapter
import com.example.gyme.data.AppDatabase
import com.example.gyme.data.RunHistory
import com.example.gyme.databinding.FragmentTrackerBinding
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class TrackerFragment : Fragment() {

    private lateinit var binding: FragmentTrackerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: AppDatabase

    // Variabel Tracking
    private var isTracking = false
    private var totalDistance = 0.0
    private var caloriesBurned = 0 // Variabel Kalori
    private var userWeight = 60f   // Default berat badan

    private var pathPoints = mutableListOf<GeoPoint>()
    private lateinit var locationCallback: LocationCallback
    private var runPolyline: Polyline? = null

    // Variabel Timer
    private var startTime = 0L
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60
            binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        setupMap()

        // AMBIL BERAT BADAN USER
        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userWeight = sharedPref.getFloat("weight", 60f) // Ambil berat, default 60kg

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()

        binding.btnStartRun.setOnClickListener {
            if (isTracking) stopTracking() else checkPermissionAndStart()
        }

        binding.btnHistory.setOnClickListener { showHistoryDialog() }
    }

    // ... (setupMap, setupLocationCallback SAMA) ...
    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(18.0)
        binding.mapView.isHorizontalMapRepetitionEnabled = true
        binding.mapView.isVerticalMapRepetitionEnabled = false
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isTracking) return
                for (location in locationResult.locations) updateTrack(location)
            }
        }
    }

    private fun updateTrack(location: Location) {
        if (!location.hasAccuracy() || location.accuracy > 30) return

        val newPoint = GeoPoint(location.latitude, location.longitude)

        if (pathPoints.isNotEmpty()) {
            val lastPoint = pathPoints.last()
            val result = FloatArray(1)
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, newPoint.latitude, newPoint.longitude, result)

            val distanceInMeters = result[0]
            if (distanceInMeters < 3) return

            totalDistance += distanceInMeters / 1000.0

            // --- HITUNG KALORI REAL-TIME ---
            // Rumus: Jarak (km) * Berat (kg) * 1.036
            val kcal = totalDistance * userWeight * 1.036
            caloriesBurned = kcal.toInt()
        }

        pathPoints.add(newPoint)

        if (runPolyline == null) {
            runPolyline = Polyline().apply {
                outlinePaint.color = Color.RED
                outlinePaint.strokeWidth = 15f
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            }
            binding.mapView.overlays.add(runPolyline)
        }
        runPolyline?.setPoints(pathPoints)
        binding.mapView.controller.animateTo(newPoint)
        binding.mapView.invalidate()

        // Update UI
        binding.tvDistance.text = "%.2f".format(totalDistance)
        binding.tvCalories.text = "$caloriesBurned" // Tampilkan Kalori
    }

    // ... (checkPermissionAndStart, requestPermissionLauncher SAMA) ...
    private fun checkPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        startTracking()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) startTracking() else Toast.makeText(context, "Izin Ditolak!", Toast.LENGTH_SHORT).show()
    }


    private fun startTracking() {
        isTracking = true
        pathPoints.clear()
        totalDistance = 0.0
        caloriesBurned = 0 // Reset Kalori

        if (runPolyline != null) {
            binding.mapView.overlays.remove(runPolyline)
            runPolyline = null
        }
        binding.mapView.invalidate()

        binding.btnStartRun.text = "STOP TRACKING"
        binding.btnStartRun.backgroundTintList = requireContext().getColorStateList(android.R.color.holo_red_dark)

        startTime = System.currentTimeMillis()
        timerHandler.postDelayed(timerRunnable, 0)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(5f)
            .build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Toast.makeText(context, "Mulai Lari!", Toast.LENGTH_SHORT).show()
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
            myLocationOverlay.enableMyLocation()
            binding.mapView.overlays.add(myLocationOverlay)
        }
    }

    private fun stopTracking() {
        isTracking = false
        binding.btnStartRun.text = "MULAI TRACKING"
        binding.btnStartRun.backgroundTintList = requireContext().getColorStateList(R.color.gym_accent)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        timerHandler.removeCallbacks(timerRunnable)
        val finalDuration = System.currentTimeMillis() - startTime

        if (totalDistance > 0.05) {
            saveRunToHistory(totalDistance, finalDuration, caloriesBurned)
            Toast.makeText(context, "Disimpan: $caloriesBurned Kcal terbakar! 🔥", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Jarak terlalu pendek.", Toast.LENGTH_SHORT).show()
        }

        binding.tvDistance.text = "0.00"
        binding.tvCalories.text = "0"
        binding.tvTimer.text = "00:00:00"
    }

    private fun saveRunToHistory(distance: Double, duration: Long, calories: Int) {
        lifecycleScope.launch {
            val history = RunHistory(
                dateInMillis = System.currentTimeMillis(),
                distanceKm = distance,
                durationInMillis = duration,
                caloriesBurned = calories // Simpan Kalori
            )
            database.runHistoryDao().insertRun(history)
        }
    }

    // ... (showHistoryDialog SAMA, tapi Adapter-nya perlu update dikit nanti) ...
    private fun showHistoryDialog() {
        val recyclerView = RecyclerView(requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setPadding(32, 32, 32, 32)
        val dialog = AlertDialog.Builder(requireContext()).setTitle("Riwayat Lari").setView(recyclerView).setPositiveButton("Tutup", null).create()
        lifecycleScope.launch {
            val historyList = database.runHistoryDao().getAllRuns()
            val adapter = RunHistoryAdapter(historyList) { itemToDelete ->
                lifecycleScope.launch {
                    database.runHistoryDao().deleteRun(itemToDelete)
                    dialog.dismiss()
                    showHistoryDialog()
                }
            }
            recyclerView.adapter = adapter
        }
        dialog.show()
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
}