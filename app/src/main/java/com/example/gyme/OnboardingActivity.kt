package com.example.gyme

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gyme.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var isMaleSelected = true // Default Male

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek apakah user sudah pernah login (Kalau sudah, langsung ke MainActivity)
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("is_setup_done", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup Tampilan Awal Gender (Male aktif)
        updateGenderUI()

        // Klik Male
        binding.cardMale.setOnClickListener {
            isMaleSelected = true
            updateGenderUI()
        }

        // Klik Female
        binding.cardFemale.setOnClickListener {
            isMaleSelected = false
            updateGenderUI()
        }

        binding.btnFinishOnboarding.setOnClickListener {
            saveDataAndProceed()
        }
    }

    private fun updateGenderUI() {
        val activeColor = ContextCompat.getColor(this, R.color.gym_accent)
        val inactiveBg = Color.parseColor("#1E1E1E")
        val activeBg = Color.parseColor("#333333") // Sedikit lebih terang
        val inactiveText = Color.parseColor("#888888")
        val activeText = Color.WHITE

        if (isMaleSelected) {
            // Male Aktif
            binding.cardMale.setCardBackgroundColor(activeBg)
            binding.imgMale.imageTintList = ColorStateList.valueOf(activeColor)
            binding.tvMale.setTextColor(activeText)


            // Female Non-Aktif
            binding.cardFemale.setCardBackgroundColor(inactiveBg)
            binding.imgFemale.imageTintList = ColorStateList.valueOf(inactiveText)
            binding.tvFemale.setTextColor(inactiveText)
        } else {
            // Female Aktif
            binding.cardFemale.setCardBackgroundColor(activeBg)
            binding.imgFemale.imageTintList = ColorStateList.valueOf(activeColor)
            binding.tvFemale.setTextColor(activeText)

            // Male Non-Aktif
            binding.cardMale.setCardBackgroundColor(inactiveBg)
            binding.imgMale.imageTintList = ColorStateList.valueOf(inactiveText)
            binding.tvMale.setTextColor(inactiveText)
        }
    }

    private fun saveDataAndProceed() {
        val weightStr = binding.etWeight.text.toString()
        val heightStr = binding.etHeight.text.toString()
        val ageStr = binding.etAge.text.toString()

        if (weightStr.isNotEmpty() && heightStr.isNotEmpty() && ageStr.isNotEmpty()) {
            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.putFloat("weight", weightStr.toFloat())
            editor.putFloat("height", heightStr.toFloat())
            editor.putInt("age", ageStr.toInt())
            editor.putBoolean("is_male", isMaleSelected)
            editor.putBoolean("is_setup_done", true) // Tandai sudah selesai
            editor.apply()

            Toast.makeText(this, "Profile Created! Welcome! 🚀", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Please fill all fields to continue.", Toast.LENGTH_SHORT).show()
        }
    }
}