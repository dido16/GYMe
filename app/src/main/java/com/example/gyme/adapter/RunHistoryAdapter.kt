package com.example.gyme.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gyme.data.RunHistory
import com.example.gyme.databinding.ItemRunHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RunHistoryAdapter(
    private var list: List<RunHistory>,
    private val onDeleteClick: (RunHistory) -> Unit
) : RecyclerView.Adapter<RunHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemRunHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemRunHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = list[position]

        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(item.dateInMillis))

        holder.binding.tvDate.text = dateString
        holder.binding.tvRunDistance.text = "%.2f km".format(item.distanceKm)

        val seconds = (item.durationInMillis / 1000) % 60
        val minutes = (item.durationInMillis / (1000 * 60)) % 60
        val hours = (item.durationInMillis / (1000 * 60 * 60))

        val durationText = if (hours > 0) {
            "⏱ %dja %02dm".format(hours, minutes)
        } else {
            "⏱ %02dm %02ds".format(minutes, seconds)
        }

        holder.binding.tvRunDuration.text = durationText

        holder.binding.btnDeleteRun.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<RunHistory>) {
        list = newList
        notifyDataSetChanged()
    }
}