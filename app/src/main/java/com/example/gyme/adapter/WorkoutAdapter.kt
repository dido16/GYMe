package com.example.gyme.adapter

import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gyme.DetailActivity
import com.example.gyme.data.Workout
import com.example.gyme.databinding.ItemWorkoutBinding

class WorkoutAdapter(
    private var list: List<Workout>,
    private val onCheckChanged: (Workout) -> Unit,
    private val onDeleteRequest: (Workout) -> Unit // <--- Callback Baru untuk Hapus
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    class WorkoutViewHolder(val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val data = list[position]
        holder.binding.apply {
            tvExerciseName.text = data.exerciseName
            tvMuscleGroup.text = "Target: ${data.muscleGroup}"
            tvSets.text = data.sets

            // Tampilkan beban jika ada
            val weightText = if (data.weight > 0) " • ${data.weight}kg" else ""
            tvReps.text = "${data.reps}$weightText"

            // Logic Checkbox (Hindari trigger listener saat binding)
            cbDone.setOnCheckedChangeListener(null)
            cbDone.isChecked = data.isCompleted

            // Logic Coret Teks
            if (data.isCompleted) {
                tvExerciseName.paintFlags = tvExerciseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                root.alpha = 0.5f
            } else {
                tvExerciseName.paintFlags = tvExerciseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                root.alpha = 1.0f
            }

            Glide.with(holder.itemView.context)
                .load(data.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgExercise)

            // 1. KLIK CHECKBOX: Simpan status
            cbDone.setOnCheckedChangeListener { _, isChecked ->
                data.isCompleted = isChecked
                onCheckChanged(data)
                notifyItemChanged(position)
            }

            // 2. KLIK KARTU: Buka Detail
            root.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra("EXTRA_ID", data.id)
                    putExtra("EXTRA_NAME", data.exerciseName)
                    putExtra("EXTRA_MUSCLE", data.muscleGroup)
                    putExtra("EXTRA_SETS", data.sets)
                    putExtra("EXTRA_REPS", data.reps)
                    putExtra("EXTRA_INSTRUCT", data.instructions)
                    putExtra("EXTRA_IMAGE", data.imageUrl)
                    putExtra("EXTRA_WEIGHT", data.weight)
                }
                context.startActivity(intent)
            }

            // 3. TEKAN TAHAN (LONG PRESS): Hapus Latihan
            root.setOnLongClickListener {
                onDeleteRequest(data) // Panggil fungsi hapus di MainActivity
                true // true artinya event sudah ditangani
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Workout>) {
        list = newList
        notifyDataSetChanged()
    }
}