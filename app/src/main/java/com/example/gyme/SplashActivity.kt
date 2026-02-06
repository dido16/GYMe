package com.example.gyme

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen") // Biar ga di-warning Android 12
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Sembunyikan Action Bar biar Fullscreen Hitam
        supportActionBar?.hide()

        // ANIMASI LOGO
        val logoContainer = findViewById<View>(R.id.logoContainer)

        // Geser ke bawah sedikit (translationY), lalu animasikan ke posisi asli (0) sambil Fade In (alpha 1)
        logoContainer.translationY = 100f
        logoContainer.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000) // Durasi 1 detik
            .setStartDelay(200) // Tunggu 0.2 detik baru mulai
            .setInterpolator(DecelerateInterpolator()) // Efek rem (cepat di awal, pelan di akhir)
            .start()

        // LOGIKA PINDAH HALAMAN (Setelah 2.5 Detik)
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 2500)
    }

    private fun checkUserAndNavigate() {
        // Cek apakah user sudah pernah setup profil?
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isSetupDone = sharedPref.getBoolean("is_setup_done", false)

        if (isSetupDone) {
            // User Lama -> Langsung ke Menu Utama
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // User Baru -> Ke Halaman Onboarding (Setup Profil)
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        }

        // Tutup Splash biar user gak bisa back ke sini
        finish()
    }
}