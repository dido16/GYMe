package com.example.gyme.utils

import com.example.gyme.data.Meal
import kotlin.random.Random

object FoodRepository {

    // Struktur Data Internal untuk Bahan Mentah (Base Portion)
    // Base Amount adalah porsi standar manusia normal (Maintenance)
    data class RawFood(
        val name: String,
        val baseAmount: Int, // e.g. 100
        val unit: String,    // e.g. "gr", "butir", "buah"
        val baseCals: Int,   // Kalori per baseAmount
        val protein: Int     // Protein per baseAmount
    )

    // --- 1. SUMBER KARBO (Energi) ---
    private val carbs = listOf(
        RawFood("Nasi Putih", 100, "gr", 130, 2),
        RawFood("Nasi Merah", 100, "gr", 110, 3),
        RawFood("Jagung Rebus", 100, "gr", 96, 3),
        RawFood("Kentang Rebus", 150, "gr", 130, 3),
        RawFood("Ubi Cilembu", 150, "gr", 150, 2),
        RawFood("Oatmeal", 50, "gr", 190, 6),
        RawFood("Roti Gandum", 2, "lembar", 140, 6),
        RawFood("Pasta / Spaghetti", 80, "gr", 130, 5)
    )

    // --- 2. SUMBER PROTEIN (Otot) ---
    private val proteins = listOf(
        RawFood("Dada Ayam Bakar", 100, "gr", 165, 31),
        RawFood("Dada Ayam Rebus", 100, "gr", 150, 30),
        RawFood("Telur Rebus", 2, "butir", 155, 13),
        RawFood("Putih Telur", 3, "butir", 51, 11),
        RawFood("Ikan Kembung", 100, "gr", 160, 18),
        RawFood("Ikan Salmon", 100, "gr", 200, 20),
        RawFood("Tempe Bakar", 3, "potong", 160, 15),
        RawFood("Tahu Putih", 4, "kotak", 100, 10),
        RawFood("Sapi Lada Hitam (Lean)", 100, "gr", 200, 22),
        RawFood("Whey Protein", 1, "scoop", 120, 24)
    )

    // --- 3. SAYUR & SERAT (Vitamin) ---
    private val veggies = listOf(
        RawFood("Tumis Kangkung", 1, "piring kcl", 60, 2),
        RawFood("Sayur Sop/Bening", 1, "mangkuk", 50, 1),
        RawFood("Capcay Kuah", 1, "mangkuk", 70, 2),
        RawFood("Salad Sayur", 1, "piring", 40, 1),
        RawFood("Brokoli Rebus", 100, "gr", 35, 3),
        RawFood("Timun & Tomat", 1, "porsi", 20, 1),
        RawFood("Bayam Rebus", 1, "mangkuk", 23, 3)
    )

    // --- 4. LEMAK SEHAT & BUAH ---
    private val fatsAndFruits = listOf(
        RawFood("Alpukat", 1, "buah sedang", 320, 4),
        RawFood("Pisang", 1, "buah", 105, 1),
        RawFood("Apel Fuji", 1, "buah", 60, 0),
        RawFood("Kacang Almond", 15, "butir", 105, 4),
        RawFood("Susu Low Fat", 200, "ml", 100, 8),
        RawFood("Minyak Zaitun (Masak)", 1, "sdm", 80, 0),
        RawFood("Yoghurt Plain", 1, "cup", 60, 3)
    )

    // --- LOGIKA UTAMA: GENERATOR MENU CERDAS ---
    fun generateMealPlan(targetCalories: Int, goal: String): List<Meal> {
        val today = System.currentTimeMillis()
        val plan = mutableListOf<Meal>()

        // --- TENTUKAN MULTIPLIER (PENGALI PORSI) ---
        val carbMulti: Double
        val proteinMulti: Double

        when (goal) {
            "Bulking" -> {
                // Bulking: Makan BANYAK Karbo (1.5x - 2x lipat), Protein Tinggi (1.5x)
                carbMulti = 2.0
                proteinMulti = 1.5
            }
            "Cutting" -> {
                // Cutting: Makan DIKIT Karbo (0.8x - 1x), Protein TETAP TINGGI (1.2x) biar gak kempes
                carbMulti = 0.8
                proteinMulti = 1.2
            }
            else -> { // Maintenance
                // Normal: Porsi standar (1.0x)
                carbMulti = 1.0
                proteinMulti = 1.0
            }
        }

        // --- 1. BREAKFAST ---
        plan.add(createMeal(pickRandom(carbs), "Breakfast", today, if(goal=="Bulking") 1.5 else 1.0)) // Karbo pagi
        plan.add(createMeal(pickRandom(proteins), "Breakfast", today, 1.0)) // Protein standar pagi

        // --- 2. LUNCH (MAKAN BESAR) ---
        plan.add(createMeal(pickRandom(carbs), "Lunch", today, carbMulti))     // Karbo disesuaikan Goal
        plan.add(createMeal(pickRandom(proteins), "Lunch", today, proteinMulti)) // Protein disesuaikan Goal
        plan.add(createMeal(pickRandom(veggies), "Lunch", today, 1.0))         // Sayur tetap 1 porsi

        // --- 3. DINNER ---
        if (goal == "Cutting") {
            // Kalau Diet: Malam HINDARI Karbo berat, ganti Sayur/Buah + Protein
            plan.add(createMeal(pickRandom(proteins), "Dinner", today, 1.0))
            plan.add(createMeal(pickRandom(veggies), "Dinner", today, 1.0))
            plan.add(createMeal(pickRandom(fatsAndFruits), "Dinner", today, 0.5)) // Buah dikit aja
        } else {
            // Kalau Bulking/Normal: Makan malam tetap lengkap
            plan.add(createMeal(pickRandom(carbs), "Dinner", today, carbMulti * 0.8)) // Karbo dikit lebih rendah drpd siang
            plan.add(createMeal(pickRandom(proteins), "Dinner", today, proteinMulti))
            plan.add(createMeal(pickRandom(fatsAndFruits), "Dinner", today, 1.0))
        }

        // --- 4. SNACK (LOGIKA SPLIT) ---
        var currentCals = plan.sumOf { it.calories }
        var remaining = targetCalories - currentCals

        // Batas wajar satu kali snack adalah 300-400 kcal.
        // Kalau sisa kalori > 400, kita pecah jadi 2 snack agar porsinya masuk akal.

        if (remaining > 50) { // Kalau cuma sisa dikit (<50) abaikan saja
            if (remaining > 400) {
                // KASUS 1: Sisa Banyak (Misal 800 kcal) -> Pecah 2
                val snack1Cals = remaining / 2
                val snack2Cals = remaining - snack1Cals // Sisanya

                val snack1Name = if (goal == "Bulking") "Whey Protein / Mass Gainer" else "Yoghurt + Granola"
                val snack2Name = if (goal == "Bulking") "Roti Bakar Selai Kacang" else "Kacang Almond"

                plan.add(Meal(0, snack1Name, snack1Cals, 20, "Snack", today))
                plan.add(Meal(0, snack2Name, snack2Cals, 10, "Snack", today))

            } else {
                // KASUS 2: Sisa Sedikit (Misal 250 kcal) -> 1 Snack Cukup
                val snackName = if (goal == "Bulking") "Susu Full Cream + Pisang" else "Apel / Pir Potong"
                // Protein snack kita estimasi 5-10g
                plan.add(Meal(0, snackName, remaining, 5, "Snack", today))
            }
        }

        return plan
    }

    // Fungsi Helper: Mengubah RawFood menjadi Meal dengan porsi yang sudah dihitung
    private fun createMeal(raw: RawFood, type: String, date: Long, multiplier: Double): Meal {
        // Hitung Porsi Baru
        val newAmount = (raw.baseAmount * multiplier).toInt()
        val newCals = (raw.baseCals * multiplier).toInt()
        val newProtein = (raw.protein * multiplier).toInt()

        // Format Nama Baru: "Nasi Putih (200 gr)"
        val displayName = "${raw.name} ($newAmount ${raw.unit})"

        return Meal(
            id = 0,
            name = displayName,
            calories = newCals,
            protein = newProtein,
            type = type,
            dateInMillis = date
        )
    }

    private fun <T> pickRandom(list: List<T>): T {
        return list[Random.nextInt(list.size)]
    }
}