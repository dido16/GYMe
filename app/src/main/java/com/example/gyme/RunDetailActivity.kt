package com.example.gyme

import android.graphics.Color
import android.graphics.CornerPathEffect // PENTING
import android.graphics.Paint           // PENTING
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gyme.databinding.ActivityRunDetailBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

class RunDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRunDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this))
        binding = ActivityRunDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Terima Data dari Intent
        val distance = intent.getDoubleExtra("DIST", 0.0)
        val pace = intent.getStringExtra("PACE") ?: "-"
        val calories = intent.getIntExtra("CAL", 0)
        val date = intent.getLongExtra("DATE", 0L)
        val pathJson = intent.getStringExtra("PATH")

        // Set Text UI
        binding.tvDetailDistance.text = "%.2f".format(distance)
        binding.tvDetailPace.text = pace
        binding.tvDetailCal.text = calories.toString()
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        binding.tvDateDetail.text = sdf.format(Date(date))

        // SETUP PETA
        binding.mapDetail.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapDetail.setMultiTouchControls(true)

        // GAMBAR JALUR DARI JSON
        if (!pathJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<List<Double>>>() {}.type
                val pointList: List<List<Double>> = Gson().fromJson(pathJson, type)

                val geoPoints = pointList.map { GeoPoint(it[0], it[1]) }

                if (geoPoints.isNotEmpty()) {
                    val line = Polyline()
                    line.setPoints(geoPoints)

                    val neonColor = androidx.core.content.ContextCompat.getColor(this, R.color.gym_accent)
                    line.outlinePaint.color = neonColor
                    line.outlinePaint.strokeWidth = 15f
                    line.outlinePaint.isAntiAlias = true

                    line.outlinePaint.strokeJoin = Paint.Join.ROUND
                    line.outlinePaint.strokeCap = Paint.Cap.ROUND

                    line.outlinePaint.pathEffect = CornerPathEffect(30f)

                    binding.mapDetail.overlays.add(line)

                    // Zoom ke jalur & Center map
                    binding.mapDetail.addOnFirstLayoutListener { v, left, top, right, bottom ->
                        if (binding.mapDetail.width > 0 && binding.mapDetail.height > 0) {
                            binding.mapDetail.zoomToBoundingBox(line.bounds, true, 100)
                        }
                    }
                    // Fallback kalau listener gak jalan
                    binding.mapDetail.controller.setZoom(17.0)
                    binding.mapDetail.controller.setCenter(geoPoints.first())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}