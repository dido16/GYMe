package com.example.gyme

import android.Manifest
import android.app.AlertDialog
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
    private var pathPoints = mutableListOf<GeoPoint>()
    private lateinit var locationCallback: LocationCallback
    private var runPolyline: Polyline? = null

    // --- VARIABEL TIMER ---
    private var startTime = 0L
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            // Format 00:00:00
            binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)

            timerHandler.postDelayed(this, 1000) // Update setiap 1 detik
        }
    }
    // ---------------------

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()

        binding.btnStartRun.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                checkPermissionAndStart()
            }
        }

        binding.btnHistory.setOnClickListener {
            showHistoryDialog()
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
                for (location in locationResult.locations) {
                    updateTrack(location)
                }
            }
        }
    }

    private fun updateTrack(location: Location) {
        if (!location.hasAccuracy()) return
        if (location.accuracy > 30) return

        val newPoint = GeoPoint(location.latitude, location.longitude)

        if (pathPoints.isNotEmpty()) {
            val lastPoint = pathPoints.last()
            val result = FloatArray(1)
            Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                newPoint.latitude, newPoint.longitude,
                result
            )

            val distanceInMeters = result[0]
            if (distanceInMeters < 3) return

            totalDistance += distanceInMeters / 1000.0
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
        binding.tvDistance.text = "%.2f KM".format(totalDistance)
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
        if (isGranted) startTracking() else Toast.makeText(context, "Izin Ditolak!", Toast.LENGTH_SHORT).show()
    }

    private fun startTracking() {
        isTracking = true
        pathPoints.clear()
        totalDistance = 0.0

        if (runPolyline != null) {
            binding.mapView.overlays.remove(runPolyline)
            runPolyline = null
        }
        binding.mapView.invalidate()

        binding.btnStartRun.text = "STOP TRACKING"
        binding.btnStartRun.backgroundTintList = requireContext().getColorStateList(android.R.color.holo_red_dark)

        // --- MULAI TIMER ---
        startTime = System.currentTimeMillis()
        timerHandler.postDelayed(timerRunnable, 0)
        // -------------------

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

        // --- STOP TIMER ---
        timerHandler.removeCallbacks(timerRunnable)
        val finalDuration = System.currentTimeMillis() - startTime
        // ------------------

        if (totalDistance > 0.05) {
            saveRunToHistory(totalDistance, finalDuration) // Kirim Durasi juga
            Toast.makeText(context, "Lari disimpan ke Riwayat!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Jarak terlalu pendek, tidak disimpan.", Toast.LENGTH_SHORT).show()
        }

        binding.tvDistance.text = "0.00 KM"
        binding.tvTimer.text = "00:00:00" // Reset teks timer
    }

    private fun saveRunToHistory(distance: Double, duration: Long) {
        lifecycleScope.launch {
            val history = RunHistory(
                dateInMillis = System.currentTimeMillis(),
                distanceKm = distance,
                durationInMillis = duration // Simpan durasi
            )
            database.runHistoryDao().insertRun(history)
        }
    }

    private fun showHistoryDialog() {
        val recyclerView = RecyclerView(requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setPadding(32, 32, 32, 32)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Riwayat Lari")
            .setView(recyclerView)
            .setPositiveButton("Tutup", null)
            .create()

        lifecycleScope.launch {
            val historyList = database.runHistoryDao().getAllRuns()

            if (historyList.isEmpty()) {
                Toast.makeText(requireContext(), "Belum ada riwayat", Toast.LENGTH_SHORT).show()
            }

            val adapter = RunHistoryAdapter(historyList) { itemToDelete ->
                lifecycleScope.launch {
                    database.runHistoryDao().deleteRun(itemToDelete)
                    dialog.dismiss()
                    showHistoryDialog()
                    Toast.makeText(requireContext(), "Data dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            recyclerView.adapter = adapter
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}