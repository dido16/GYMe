package com.example.gyme

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gyme.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAndDisplayData()

        // Tombol Kembali
        binding.btnBack.setOnClickListener { finish() }

        // Tombol Edit
        binding.btnEditProfile.setOnClickListener {
            showEditDialog()
        }
    }

    private fun loadAndDisplayData() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val weight = sharedPref.getFloat("weight", 60f)
        val height = sharedPref.getFloat("height", 170f)
        val age = sharedPref.getInt("age", 20)
        val isMale = sharedPref.getBoolean("is_male", true)

        // 1. Set Basic Data
        binding.tvWeight.text = weight.toInt().toString() // Tampilkan bulat biar rapi
        binding.tvHeight.text = height.toInt().toString()
        binding.tvAge.text = age.toString()

        // 2. Hitung BMI
        val heightInMeter = height / 100
        val bmi = weight / (heightInMeter * heightInMeter)

        binding.tvBmiScore.text = "%.1f".format(bmi)
        binding.pbBmi.progress = bmi.toInt() // Update visual bar

        val bmiStatus = when {
            bmi < 18.5 -> "Underweight"
            bmi < 24.9 -> "Normal Weight"
            bmi < 29.9 -> "Overweight"
            else -> "Obesity"
        }
        binding.tvBmiStatus.text = bmiStatus

        // Ganti warna teks status berdasarkan kondisi
        val statusColor = when {
            bmi < 18.5 -> Color.parseColor("#FFC107") // Kuning
            bmi < 24.9 -> Color.parseColor("#4CAF50") // Hijau
            else -> Color.parseColor("#FF5722") // Oranye/Merah
        }
        binding.tvBmiStatus.setTextColor(statusColor)


        // 3. Hitung BMR (Mifflin-St Jeor)
        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }

        // 4. Hitung TDEE (Asumsi Moderate Activity x1.55)
        val tdee = (bmr * 1.55)

        binding.tvBmr.text = bmr.toInt().toString()
        binding.tvTdee.text = tdee.toInt().toString()
    }

    // Dialog Edit Data (Re-use konsep Custom Dialog)
    private fun showEditDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_workout, null)
        // Note: Kita pakai layout 'dialog_add_workout' sementara karena isinya mirip (input fields)
        // Tapi idealnya bikin layout khusus 'dialog_edit_profile.xml'
        // Untuk sekarang kita modif via code aja biar cepet

        val et1 = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val et2 = dialogView.findViewById<EditText>(R.id.etMuscleGroup)
        val et3 = dialogView.findViewById<EditText>(R.id.etSets)
        val et4 = dialogView.findViewById<EditText>(R.id.etReps)
        val et5 = dialogView.findViewById<EditText>(R.id.etImageUrl)
        val btnSave = dialogView.findViewById<Button>(R.id.btnAdd)

        // Ubah Hint agar sesuai konteks Profile
        et1.hint = "Berat Badan (kg)"
        et1.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        et2.hint = "Tinggi Badan (cm)"
        et2.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        et3.hint = "Umur (thn)"
        et3.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        // Sembunyikan field yang tidak perlu
        et4.visibility = android.view.View.GONE
        et5.visibility = android.view.View.GONE

        btnSave.text = "SIMPAN PERUBAHAN"

        // Pre-fill data lama
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        et1.setText(sharedPref.getFloat("weight", 0f).toString())
        et2.setText(sharedPref.getFloat("height", 0f).toString())
        et3.setText(sharedPref.getInt("age", 0).toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val wStr = et1.text.toString()
            val hStr = et2.text.toString()
            val aStr = et3.text.toString()

            if (wStr.isNotEmpty() && hStr.isNotEmpty() && aStr.isNotEmpty()) {
                val editor = sharedPref.edit()
                editor.putFloat("weight", wStr.toFloat())
                editor.putFloat("height", hStr.toFloat())
                editor.putInt("age", aStr.toInt())
                editor.apply()

                loadAndDisplayData() // Refresh Tampilan
                Toast.makeText(this, "Profil Diupdate!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Isi semua data ya!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}