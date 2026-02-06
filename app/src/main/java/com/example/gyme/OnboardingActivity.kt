package com.example.gyme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gyme.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek status (Opsional: sebagai backup jika Splash Screen error)
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        // PENTING: Gunakan key "is_setup_done" agar cocok dengan SplashActivity
        val isSetupDone = sharedPref.getBoolean("is_setup_done", false)

        if (isSetupDone) {
            goToMain()
        }

        binding.btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun saveUserData() {
        val weightStr = binding.etWeight.text.toString()
        val heightStr = binding.etHeight.text.toString()
        val ageStr = binding.etAge.text.toString()

        if (weightStr.isEmpty() || heightStr.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val isMale = binding.rbMale.isChecked
        val weight = weightStr.toFloat()
        val height = heightStr.toFloat()
        val age = ageStr.toInt()

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putFloat("weight", weight)
        editor.putFloat("height", height)
        editor.putInt("age", age)
        editor.putBoolean("is_male", isMale)

        editor.putBoolean("is_setup_done", true)
        editor.apply()

        Toast.makeText(this, "Data tersimpan! Selamat Latihan.", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}