package com.example.gyme

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gyme.data.AppDatabase
import com.example.gyme.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        // 1. Load Data Lama
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        binding.etWeight.setText(sharedPref.getFloat("weight", 0f).toString())
        binding.etHeight.setText(sharedPref.getFloat("height", 0f).toString())
        binding.etAge.setText(sharedPref.getInt("age", 0).toString())

        // 2. Tombol Simpan
        binding.btnSave.setOnClickListener {
            val newWeight = binding.etWeight.text.toString().toFloatOrNull()
            val newHeight = binding.etHeight.text.toString().toFloatOrNull()
            val newAge = binding.etAge.text.toString().toIntOrNull()

            if (newWeight != null && newHeight != null && newAge != null) {
                val editor = sharedPref.edit()
                editor.putFloat("weight", newWeight)
                editor.putFloat("height", newHeight)
                editor.putInt("age", newAge)
                editor.apply()

                Toast.makeText(this, "Profil Diupdate! Target Kalori disesuaikan.", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke Main
            } else {
                Toast.makeText(this, "Mohon isi angka yang valid", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Tombol Reset Progress
        binding.btnReset.setOnClickListener {
            lifecycleScope.launch {
                database.workoutDao().resetAllWorkouts()
                Toast.makeText(this@ProfileActivity, "Minggu Baru Dimulai! Semangat!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke Main biar list kerefresh
            }
        }
    }
}