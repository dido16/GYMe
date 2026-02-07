package com.example.gyme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
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
import androidx.core.content.ContextCompat // Untuk ambil warna resource
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gyme.data.AppDatabase
import com.example.gyme.data.RunHistory
import com.example.gyme.databinding.FragmentTrackerBinding
import com.google.android.gms.location.*
import com.google.gson.Gson
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

    private var isTracking = false
    private var totalDistance = 0.0
    private var caloriesBurned = 0
    private var currentPace = "0:00"
    private var userWeight = 60f

    private var pathPoints = mutableListOf<GeoPoint>()
    private lateinit var locationCallback: LocationCallback
    private var runPolyline: Polyline? = null

    // Timer
    private var startTime = 0L
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60
            binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)

            // --- HITUNG PACE (Menit per KM) ---
            if (totalDistance > 0.05) { // Hitung setelah 50 meter biar stabil
                val totalMinutes = millis / 1000.0 / 60.0
                val paceVal = totalMinutes / totalDistance

                val paceMin = paceVal.toInt()
                val paceSec = ((paceVal - paceMin) * 60).toInt()
                currentPace = "%d:%02d".format(paceMin, paceSec)
                binding.tvPace.text = currentPace
            }

            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(requireContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        setupMap()

        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userWeight = sharedPref.getFloat("weight", 60f)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()

        binding.btnStartRun.setOnClickListener {
            if (isTracking) stopTracking() else checkPermissionAndStart()
        }

        binding.btnHistory.setOnClickListener {
            // Pindah ke Activity History
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

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
            val kcal = totalDistance * userWeight * 1.036
            caloriesBurned = kcal.toInt()
        }

        pathPoints.add(newPoint)

        if (runPolyline == null) {
            runPolyline = Polyline().apply {
                // --- UPDATE WARNA KE ACCENT NEON ---
                val neonColor = ContextCompat.getColor(requireContext(), R.color.gym_accent)
                outlinePaint.color = neonColor // Pakai warna neon
                outlinePaint.strokeWidth = 15f

                // Anti-Alias biar halus
                outlinePaint.isAntiAlias = true

                // Sambungan & Ujung Bulat
                outlinePaint.strokeJoin = Paint.Join.ROUND
                outlinePaint.strokeCap = Paint.Cap.ROUND

                // Efek Sudut Melengkung (Radius 30f)
                outlinePaint.pathEffect = CornerPathEffect(30f)
            }
            binding.mapView.overlays.add(runPolyline)
        }
        runPolyline?.setPoints(pathPoints)
        binding.mapView.controller.animateTo(newPoint)
        binding.mapView.invalidate()

        binding.tvDistance.text = "%.2f".format(totalDistance)
        binding.tvCalories.text = "$caloriesBurned"
    }

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
        caloriesBurned = 0
        currentPace = "0:00"

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
            saveRunToHistory(totalDistance, finalDuration, caloriesBurned, currentPace)
            Toast.makeText(context, "Disimpan!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Jarak terlalu pendek.", Toast.LENGTH_SHORT).show()
        }

        binding.tvDistance.text = "0.00"
        binding.tvCalories.text = "0"
        binding.tvTimer.text = "00:00:00"
        binding.tvPace.text = "0:00"
    }

    private fun saveRunToHistory(distance: Double, duration: Long, calories: Int, pace: String) {
        // KONVERSI PATH POINTS KE JSON STRING
        val simplePoints = pathPoints.map { listOf(it.latitude, it.longitude) }
        val jsonPath = Gson().toJson(simplePoints)

        lifecycleScope.launch {
            val history = RunHistory(
                dateInMillis = System.currentTimeMillis(),
                distanceKm = distance,
                durationInMillis = duration,
                caloriesBurned = calories,
                avgPace = pace,
                pathData = jsonPath // SIMPAN DATA JALUR
            )
            database.runHistoryDao().insertRun(history)
        }
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
}