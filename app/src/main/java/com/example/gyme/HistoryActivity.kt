package com.example.gyme

import android.os.Bundle
import android.view.View
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

                adapter = RunHistoryAdapter(historyList) { itemToDelete ->
                    // Hapus Item
                    lifecycleScope.launch {
                        database.runHistoryDao().deleteRun(itemToDelete)
                        // Refresh Data setelah hapus
                        val updatedList = database.runHistoryDao().getAllRuns()
                        adapter.updateData(updatedList)

                        if (updatedList.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                        }
                        Toast.makeText(this@HistoryActivity, "Data dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
                rvHistory.adapter = adapter
            }
        }
    }
}