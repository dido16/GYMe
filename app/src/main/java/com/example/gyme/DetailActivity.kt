package com.example.gyme

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.gyme.data.AppDatabase
import com.example.gyme.databinding.ActivityDetailBinding
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

        // 1. Ambil Data dari Intent
        val id = intent.getIntExtra("EXTRA_ID", 0)
        val weight = intent.getDoubleExtra("EXTRA_WEIGHT", 0.0)
        val name = intent.getStringExtra("EXTRA_NAME") ?: "Latihan"
        val muscle = intent.getStringExtra("EXTRA_MUSCLE") ?: "-"
        val sets = intent.getStringExtra("EXTRA_SETS") ?: "0"
        val reps = intent.getStringExtra("EXTRA_REPS") ?: "0"
        val instructions = intent.getStringExtra("EXTRA_INSTRUCT") ?: "Tidak ada instruksi."
        val imageUrl = intent.getStringExtra("EXTRA_IMAGE") ?: ""

        // 2. Tampilkan UI (Update sesuai Layout Aesthetic Baru)
        binding.tvDetailName.text = name
        binding.tvDetailMuscle.text = muscle

        // Gabungkan Sets & Reps di satu TextView (sesuai layout baru)
        binding.tvDetailSetsReps.text = "$sets • $reps"

        binding.tvInstructions.text = instructions

        // Cek jika ada beban tersimpan
        if (weight > 0) {
            binding.etWeight.setText(weight.toString()) // ID berubah jadi etWeight
        }

        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(binding.imgDetail)

        // Tombol Back
        binding.btnBack.setOnClickListener { finish() }

        // Logika Simpan Beban
        binding.btnSaveWeight.setOnClickListener {
            val inputWeight = binding.etWeight.text.toString().toDoubleOrNull()

            if (id != 0 && inputWeight != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    database.workoutDao().updateWeight(id, inputWeight)
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

                playNotificationSound()

                // --- GANTI TOAST DENGAN DIALOG KEREN ---
                showCustomAlert(
                    title = "Waktu Habis! ⏰",
                    message = "Istirahat selesai. Tarik napas, fokus, dan angkat bebanmu sekarang!",
                    positiveText = "GAS LATIHAN!",
                    onPositiveClick = {
                        // User klik tombol Gas, timer berhenti (dialog otomatis tutup)
                    }
                )
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
        binding.btnTimer.text = "Mulai Istirahat (60s)"
        binding.btnTimer.backgroundTintList = getColorStateList(R.color.gym_accent)
    }

    // --- FUNGSI CUSTOM DIALOG YANG KEREN ---
    private fun showCustomAlert(
        title: String,
        message: String,
        positiveText: String = "Oke",
        negativeText: String? = null, // Kalau null, tombol batal disembunyikan
        onPositiveClick: () -> Unit
    ) {
        // Inflate Layout Custom
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_alert, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnPositive = dialogView.findViewById<Button>(R.id.btnDialogPositive)
        val btnNegative = dialogView.findViewById<Button>(R.id.btnDialogNegative)
        val imgIcon = dialogView.findViewById<ImageView>(R.id.imgDialogIcon)

        // Set Data
        tvTitle.text = title
        tvMessage.text = message
        btnPositive.text = positiveText

        // Atur Tombol Negative (Sembunyikan kalau tidak butuh)
        if (negativeText == null) {
            btnNegative.visibility = android.view.View.GONE
        } else {
            btnNegative.text = negativeText
        }

        // Bikin Dialog
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()

        // PENTING: Bikin background dialog jadi transparan biar rounded corner-nya kelihatan
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Klik Listener
        btnPositive.setOnClickListener {
            onPositiveClick()
            dialog.dismiss()
        }

        btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun playNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
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
        mediaPlayer?.release()
    }
}