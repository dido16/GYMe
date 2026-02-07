package com.example.gyme

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gyme.data.AppDatabase
import com.example.gyme.data.WeightHistory
import com.example.gyme.databinding.ActivityProfileBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        loadAndDisplayData()
        setupChartStyle() // Setup tampilan awal chart
        loadChartData()   // Load data grafik dari DB

        binding.btnBack.setOnClickListener { finish() }

        binding.btnEditProfile.setOnClickListener {
            showEditDialog()
        }
    }

    // --- FUNGSI CHART ---
    private fun setupChartStyle() {
        val chart = binding.weightChart

        // Matikan interaksi ribet biar bersih
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)

        // Hilangkan Grid & Border
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = Color.parseColor("#666666")
        chart.xAxis.textColor = Color.parseColor("#666666")
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        // Format tanggal di bawah (Sumbu X)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }
    }

    private fun loadChartData() {
        lifecycleScope.launch {
            val historyList = database.weightDao().getAllWeights()

            if (historyList.isNotEmpty()) {
                val entries = historyList.map {
                    Entry(it.dateInMillis.toFloat(), it.weightKg)
                }

                val dataSet = LineDataSet(entries, "Berat Badan")

                // STYLING GARIS
                dataSet.color = ContextCompat.getColor(this@ProfileActivity, R.color.gym_accent)
                dataSet.lineWidth = 3f
                dataSet.setCircleColor(Color.WHITE)
                dataSet.circleRadius = 4f
                dataSet.setDrawCircleHole(false)
                dataSet.valueTextColor = Color.WHITE
                dataSet.valueTextSize = 10f
                dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

                // Efek Fill
                dataSet.setDrawFilled(true)
                val fillDrawable = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.gradient_chart_fill)
                dataSet.fillDrawable = fillDrawable

                val lineData = LineData(dataSet)
                binding.weightChart.data = lineData
                binding.weightChart.invalidate()
            } else {
                binding.weightChart.clear()
            }
        }
    }

    // --- FUNGSI LOAD DATA ---
    private fun loadAndDisplayData() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val weight = sharedPref.getFloat("weight", 60f)
        val height = sharedPref.getFloat("height", 170f)
        val age = sharedPref.getInt("age", 20)
        val isMale = sharedPref.getBoolean("is_male", true)

        binding.tvWeight.text = weight.toInt().toString()
        binding.tvHeight.text = height.toInt().toString()
        binding.tvAge.text = age.toString()

        val heightInMeter = height / 100
        val bmi = weight / (heightInMeter * heightInMeter)

        binding.tvBmiScore.text = "%.1f".format(bmi)
        binding.pbBmi.progress = bmi.toInt()

        val bmiStatus = when {
            bmi < 18.5 -> "Underweight"
            bmi < 24.9 -> "Normal Weight"
            bmi < 29.9 -> "Overweight"
            else -> "Obesity"
        }
        binding.tvBmiStatus.text = bmiStatus

        val statusColor = when {
            bmi < 18.5 -> Color.parseColor("#FFC107")
            bmi < 24.9 -> Color.parseColor("#4CAF50")
            else -> Color.parseColor("#FF5722")
        }
        binding.tvBmiStatus.setTextColor(statusColor)

        val bmr = if (isMale) (10 * weight) + (6.25 * height) - (5 * age) + 5 else (10 * weight) + (6.25 * height) - (5 * age) - 161
        val tdee = (bmr * 1.55)

        binding.tvBmr.text = bmr.toInt().toString()
        binding.tvTdee.text = tdee.toInt().toString()
    }

    // --- DIALOG EDIT (UPDATE) ---
    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)

        val etWeight = dialogView.findViewById<EditText>(R.id.etEditWeight)
        val etHeight = dialogView.findViewById<EditText>(R.id.etEditHeight)
        val etAge = dialogView.findViewById<EditText>(R.id.etEditAge)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel) // Pastikan ID ini ada di XML

        // Pre-fill data lama
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        etWeight.setText(sharedPref.getFloat("weight", 0f).toString())
        etHeight.setText(sharedPref.getFloat("height", 0f).toString())
        etAge.setText(sharedPref.getInt("age", 0).toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Logic Tombol Batal
        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        // Logic Tombol Simpan
        btnSave.setOnClickListener {
            val wStr = etWeight.text.toString()
            val hStr = etHeight.text.toString()
            val aStr = etAge.text.toString()

            if (wStr.isNotEmpty() && hStr.isNotEmpty() && aStr.isNotEmpty()) {
                val newWeight = wStr.toFloat()

                // 1. Simpan ke SharedPreferences
                val editor = sharedPref.edit()
                editor.putFloat("weight", newWeight)
                editor.putFloat("height", hStr.toFloat())
                editor.putInt("age", aStr.toInt())
                editor.apply()

                // 2. SIMPAN KE DATABASE (Untuk Grafik)
                lifecycleScope.launch {
                    val weightEntry = WeightHistory(
                        dateInMillis = System.currentTimeMillis(),
                        weightKg = newWeight
                    )
                    database.weightDao().insert(weightEntry)

                    // Refresh data
                    loadAndDisplayData()
                    loadChartData() // Refresh grafik
                }

                Toast.makeText(this, "Profil Diupdate!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Isi semua data ya!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}