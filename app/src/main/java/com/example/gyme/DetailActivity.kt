package com.example.gyme

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gyme.databinding.ActivityDetailBinding
import com.example.gyme.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        // 1. Ambil Data
        val id = intent.getIntExtra("EXTRA_ID", 0)
        val weight = intent.getDoubleExtra("EXTRA_WEIGHT", 0.0)
        val name = intent.getStringExtra("EXTRA_NAME") ?: "Latihan"
        val muscle = intent.getStringExtra("EXTRA_MUSCLE") ?: "-"
        val sets = intent.getStringExtra("EXTRA_SETS") ?: "0"
        val reps = intent.getStringExtra("EXTRA_REPS") ?: "0"
        val instructions = intent.getStringExtra("EXTRA_INSTRUCT") ?: "Tidak ada instruksi."
        val imageUrl = intent.getStringExtra("EXTRA_IMAGE") ?: ""

        // 2. Tampilkan UI
        binding.tvDetailName.text = name
        binding.tvDetailMuscle.text = "Target: $muscle"
        binding.tvDetailSets.text = sets.replace(" Sets", "")
        binding.tvDetailReps.text = reps.replace(" Reps", "")
        binding.tvInstructions.text = instructions

        if (weight > 0) {
            binding.etWeightRecord.setText(weight.toString())
        }

        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.imgDetail)

        binding.btnBack.setOnClickListener { finish() }

        // Logika Simpan Beban
        binding.btnSaveWeight.setOnClickListener {
            val inputWeight = binding.etWeightRecord.text.toString().toDoubleOrNull()

            if (id != 0 && inputWeight != null) {
                // Simpan ke Database di Background Thread
                CoroutineScope(Dispatchers.IO).launch {
                    database.workoutDao().updateWeight(id, inputWeight)

                    // Balik ke UI Thread untuk Toast
                    runOnUiThread {
                        Toast.makeText(this@DetailActivity, "Beban $inputWeight kg Disimpan!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Masukkan angka yang valid", Toast.LENGTH_SHORT).show()
            }
        }


        // 3. Logika Timer
        binding.btnTimer.setOnClickListener {
            if (isTimerRunning) {
                cancelTimer()
            } else {
                startTimer(60000) // 60 Detik
            }
        }
    }

    private fun startTimer(duration: Long) {
        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.tvTimer.text = String.format("00:%02d", secondsRemaining)
            }

            override fun onFinish() {
                binding.tvTimer.text = "00:00"
                binding.btnTimer.text = "Selesai! Lanjut Set"
                binding.btnTimer.backgroundTintList = getColorStateList(R.color.gym_accent)
                isTimerRunning = false

                // --- FITUR BARU: MAINKAN SUARA ---
                playNotificationSound()

                Toast.makeText(this@DetailActivity, "Waktu Habis! Gas Latihan!", Toast.LENGTH_SHORT).show()
            }
        }.start()

        isTimerRunning = true
        binding.btnTimer.text = "Stop Timer"
        binding.btnTimer.backgroundTintList = getColorStateList(android.R.color.holo_red_dark)
    }

    private fun cancelTimer() {
        timer?.cancel()
        isTimerRunning = false
        binding.tvTimer.text = "01:00"
        binding.btnTimer.text = "Mulai Istirahat"
        binding.btnTimer.backgroundTintList = getColorStateList(R.color.gym_accent)
    }

    // Fungsi untuk memutar suara notifikasi default HP
    private fun playNotificationSound() {
        try {
            // Ambil URI suara notifikasi default
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Siapkan MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare() // Siapkan buffer
                start()   // Mainkan

                // Release memory setelah selesai main
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        mediaPlayer?.release() // Bersihkan player saat keluar activity agar tidak bocor memory
    }
}