package com.example.gyme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gyme.R
import com.example.gyme.data.Meal
import com.example.gyme.databinding.ItemMealBinding

class MealAdapter(
    private var list: List<Meal>,
    private val onDeleteClick: (Meal) -> Unit
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(val binding: ItemMealBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val item = list[position]

        // 1. Set Data Utama
        holder.binding.tvMealName.text = item.name
        holder.binding.tvMealCalories.text = item.calories.toString()
        holder.binding.tvProtein.text = "${item.protein}g"

        // 2. Set Icon Dinamis berdasarkan Waktu Makan
        val iconRes = when (item.type) {
            "Breakfast" -> R.drawable.ic_breakfast
            "Lunch" -> R.drawable.ic_lunch
            "Dinner" -> R.drawable.ic_dinner
            "Snack" -> R.drawable.ic_sneck
            else -> R.drawable.ic_breakfast
        }
        holder.binding.imgMealIcon.setImageResource(iconRes)

        // 3. Logic Header (Sama seperti sebelumnya)
        val isHeaderVisible = if (position == 0) {
            true
        } else {
            val prevItem = list[position - 1]
            item.type != prevItem.type
        }

        if (isHeaderVisible) {
            holder.binding.layoutSectionHeader.visibility = View.VISIBLE
            holder.binding.tvSectionHeader.text = item.type.uppercase()
        } else {
            holder.binding.layoutSectionHeader.visibility = View.GONE
        }

        // Tombol Hapus
        holder.binding.btnDeleteMeal.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Meal>) {
        list = newList
        notifyDataSetChanged()
    }
}