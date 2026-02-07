package com.example.gyme

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gyme.adapter.RunHistoryAdapter
import com.example.gyme.data.AppDatabase
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: RunHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        database = AppDatabase.getDatabase(this)

        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        rvHistory.layoutManager = LinearLayoutManager(this)

        // Load Data
        lifecycleScope.launch {
            val historyList = database.runHistoryDao().getAllRuns()

            if (historyList.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE

                adapter = RunHistoryAdapter(
                    list = historyList,
                    onClick = { selectedRun ->
                        // KLIK ITEM -> BUKA DETAIL
                        val intent = Intent(this@HistoryActivity, RunDetailActivity::class.java)
                        intent.putExtra("DIST", selectedRun.distanceKm)
                        intent.putExtra("PACE", selectedRun.avgPace)
                        intent.putExtra("CAL", selectedRun.caloriesBurned)
                        intent.putExtra("DATE", selectedRun.dateInMillis)
                        intent.putExtra("PATH", selectedRun.pathData)
                        startActivity(intent)
                    },
                    onDeleteClick = { itemToDelete ->
                        // KLIK HAPUS -> TAMPILKAN CUSTOM ALERT DULU
                        showDeleteConfirmation(itemToDelete)
                    }
                )
                rvHistory.adapter = adapter
            }
        }
    }

    // --- CUSTOM ALERT DIALOG ---
    private fun showDeleteConfirmation(itemToDelete: com.example.gyme.data.RunHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_alert, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnPositive = dialogView.findViewById<Button>(R.id.btnDialogPositive)
        val btnNegative = dialogView.findViewById<Button>(R.id.btnDialogNegative)

        tvTitle.text = "Hapus Data?"
        tvMessage.text = "Data lari ini akan dihapus permanen. Yakin?"
        btnPositive.text = "YA, HAPUS"
        btnNegative.text = "BATAL"

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnPositive.setOnClickListener {
            // EKSEKUSI HAPUS
            lifecycleScope.launch {
                database.runHistoryDao().deleteRun(itemToDelete)

                // Refresh List
                val updatedList = database.runHistoryDao().getAllRuns()
                adapter.updateData(updatedList)

                if (updatedList.isEmpty()) {
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                }
                Toast.makeText(this@HistoryActivity, "Data dihapus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}