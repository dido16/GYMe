package com.example.gyme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Cek Data User di Memori
            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isSetupDone = sharedPref.getBoolean("is_setup_done", false)

            // 2. Tentukan Tujuan
            val targetActivity = if (isSetupDone) {
                MainActivity::class.java // Kalau user lama -> Ke Home
            } else {
                OnboardingActivity::class.java // Kalau user baru -> Ke Input Data
            }

            // 3. Gas!
            val intent = Intent(this, targetActivity)
            startActivity(intent)
            finish()

        }, 2000) // Delay 2 detik
    }
}