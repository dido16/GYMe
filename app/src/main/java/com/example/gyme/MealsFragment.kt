package com.example.gyme

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gyme.adapter.MealAdapter
import com.example.gyme.data.AppDatabase
import com.example.gyme.databinding.FragmentMealsBinding
import com.example.gyme.utils.FoodRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class MealsFragment : Fragment() {

    private var _binding: FragmentMealsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var adapter: MealAdapter

    // Target User
    private var userGoal = "Maintenance"
    private var targetCalories = 2500 // Default

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        setupRecyclerView()
        calculateTarget()
        lifecycleScope.launch {
            kotlinx.coroutines.delay(100)
            loadMeals()
        }

        // TOMBOL GENERATE REKOMENDASI
        binding.btnGeneratePlan.setOnClickListener {
            lifecycleScope.launch {
                // 1. Hapus menu hari ini dulu (Reset)
                val todayStart = getStartOfDay()
                val todayEnd = getEndOfDay()
                database.mealDao().clearMealsByDate(todayStart, todayEnd)

                // 2. Minta "Otak" (Repository) buatkan menu baru
                val recommendedMeals = FoodRepository.generateMealPlan(targetCalories, userGoal)

                // 3. Simpan ke Database
                database.mealDao().insertAll(recommendedMeals)

                // 4. Refresh Tampilan
                loadMeals()
                Toast.makeText(context, "Menu $userGoal Siap! 🍱", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateTarget() {
        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val weight = sharedPref.getFloat("weight", 60f)
        val height = sharedPref.getFloat("height", 170f)
        val age = sharedPref.getInt("age", 20)
        val isMale = sharedPref.getBoolean("is_male", true)

        // 1. Hitung BMI & Tentukan Goal Otomatis
        val heightM = height / 100
        val bmi = weight / (heightM * heightM)

        userGoal = if (bmi < 18.5) "Bulking"
        else if (bmi > 25.0) "Cutting"
        else "Maintenance"

        // Logic tambahan: Cowok muda biasanya mau berotot (Bulking) meski BMI normal
        if (bmi < 22 && isMale) userGoal = "Bulking"

        // 2. Hitung BMR (Rumus Mifflin-St Jeor)
        val bmr = if (isMale) (10 * weight) + (6.25 * height) - (5 * age) + 5
        else (10 * weight) + (6.25 * height) - (5 * age) - 161

        // TDEE (Total Daily Energy Expenditure) - Activity Level Sedang (1.55)
        val tdee = (bmr * 1.55).toInt()

        // 3. Set Target Kalori Akhir
        targetCalories = when (userGoal) {
            "Bulking" -> tdee + 500  // Surplus
            "Cutting" -> tdee - 500  // Defisit
            else -> tdee             // Jaga Berat Badan
        }

        binding.tvTargetInfo.text = "Goal: $userGoal ($targetCalories kcal)"
        binding.btnGeneratePlan.text = "GENERATE $userGoal MENU"
    }

    private fun loadMeals() {
        // Ganti launch biasa dengan launch(Dispatchers.IO) biar di background
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {

            val start = getStartOfDay()
            val end = getEndOfDay()
            val meals = database.mealDao().getMealsByDate(start, end)

            // Sorting (Tetap di background biar UI gak macet)
            val sortedMeals = meals.sortedBy { meal ->
                when (meal.type) {
                    "Breakfast" -> 1
                    "Lunch" -> 2
                    "Dinner" -> 3
                    "Snack" -> 4
                    else -> 5
                }
            }

            // --- UPDATE UI WAJIB KEMBALI KE MAIN THREAD ---
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                adapter.updateData(sortedMeals)

                val totalEaten = meals.sumOf { it.calories }
                val caloriesLeft = targetCalories - totalEaten

                binding.tvCaloriesLeft.text = "$caloriesLeft"
                binding.progressCalories.max = targetCalories

                // Kasih animasi dikit pas progress bar gerak
                binding.progressCalories.setProgressCompat(totalEaten, true)

                if (caloriesLeft < 0) {
                    binding.tvCaloriesLeft.setTextColor(android.graphics.Color.RED)
                    binding.tvCaloriesLeft.text = "OVER!"
                } else {
                    binding.tvCaloriesLeft.setTextColor(android.graphics.Color.WHITE)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MealAdapter(emptyList()) { mealToDelete ->
            // Logic Hapus Item
            lifecycleScope.launch {
                database.mealDao().deleteMeal(mealToDelete)
                loadMeals() // Refresh setelah hapus
            }
        }
        binding.rvMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeals.adapter = adapter
    }

    // --- Helper Waktu (Awal & Akhir Hari) ---
    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}