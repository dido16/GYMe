package com.example.gyme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.gyme.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load Fragment Default (Gym) saat aplikasi dibuka
        if (savedInstanceState == null) {
            loadFragment(GymFragment())
        }

        // 2. Setup Logika Pindah Menu (Gym - Meals - Tracker)
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_gym -> {
                    loadFragment(GymFragment())
                    true
                }
                R.id.nav_meals -> {
                    loadFragment(MealsFragment())
                    true
                }
                R.id.nav_tracker -> {
                    loadFragment(TrackerFragment())
                    true
                }
                else -> false
            }
        }

        // Matikan efek re-selection (biar gak reload kalau diklik lagi)
        binding.bottomNav.setOnItemReselectedListener {
            // Do nothing
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}