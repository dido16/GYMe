package com.example.gyme

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gyme.adapter.WorkoutAdapter
import com.example.gyme.data.AppDatabase
import com.example.gyme.data.Workout
import com.example.gyme.databinding.FragmentGymBinding // PENTING: Binding Fragment
import com.example.gyme.worker.ReminderWorker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GymFragment : Fragment() {

    // Setup Binding untuk Fragment (Agak beda dikit dari Activity)
    private var _binding: FragmentGymBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var adapter: WorkoutAdapter

    // 1. Inflate Layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGymBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 2. Logika Utama (Pengganti onCreate)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Database (Ganti 'this' dengan 'requireContext()')
        database = AppDatabase.getDatabase(requireContext())

        // Init RecyclerView
        adapter = WorkoutAdapter(
            list = emptyList(),
            onCheckChanged = { workout ->
                lifecycleScope.launch {
                    database.workoutDao().updateWorkout(workout)
                }
            },
            onDeleteRequest = { workout ->
                showDeleteConfirmation(workout)
            }
        )

        binding.rvWorkout.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkout.adapter = adapter

        // Setup Tombol Profil
        binding.btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // Setup Tombol (+)
        binding.fabAdd.setOnClickListener {
            showAddWorkoutDialog()
        }

        // Setup Notifikasi
        checkNotificationPermission()
        setupDailyReminder()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        setupDatabaseAndLoadWorkout()
    }

    // --- FITUR HAPUS LATIHAN ---
    private fun showDeleteConfirmation(workout: Workout) {
        AlertDialog.Builder(requireContext()) // Ganti 'this' jadi requireContext()
            .setTitle("Hapus Latihan?")
            .setMessage("Kamu yakin ingin menghapus '${workout.exerciseName}' dari jadwal?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    database.workoutDao().delete(workout)
                    setupDatabaseAndLoadWorkout()
                    Toast.makeText(requireContext(), "Latihan dihapus!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- DIALOG TAMBAH LATIHAN ---
    private fun showAddWorkoutDialog() {
        // Ganti LayoutInflater.from(this) jadi requireContext()
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_workout, null)

        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etMuscle = dialogView.findViewById<EditText>(R.id.etMuscleGroup)
        val etSets = dialogView.findViewById<EditText>(R.id.etSets)
        val etReps = dialogView.findViewById<EditText>(R.id.etReps)
        val etImage = dialogView.findViewById<EditText>(R.id.etImageUrl)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAdd.setOnClickListener {
            val name = etName.text.toString()
            val muscle = etMuscle.text.toString()
            val sets = etSets.text.toString()
            val reps = etReps.text.toString()
            val imageUrl = etImage.text.toString()

            if (name.isNotEmpty() && muscle.isNotEmpty()) {
                saveCustomWorkout(name, muscle, sets, reps, imageUrl)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Nama & Target Otot wajib diisi!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveCustomWorkout(name: String, muscle: String, sets: String, reps: String, imageUrl: String) {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                else -> "Sunday"
            }

            val finalImage = if (imageUrl.isNotEmpty()) imageUrl else "https://media.giphy.com/media/l0HlOaQcLJ2hHpYdy/giphy.gif"

            val newWorkout = Workout(
                day = dayName,
                exerciseName = name,
                muscleGroup = muscle,
                sets = "$sets Sets",
                reps = "$reps Reps",
                instructions = "Latihan kustom tambahan.",
                imageUrl = finalImage,
                isCompleted = false,
                weight = 0.0
            )

            database.workoutDao().insert(newWorkout)
            setupDatabaseAndLoadWorkout()
            Toast.makeText(requireContext(), "$name berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        // Ganti getSharedPreferences dengan requireActivity().getSharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val weight = sharedPref.getFloat("weight", 60f)
        val height = sharedPref.getFloat("height", 170f)
        val age = sharedPref.getInt("age", 20)
        val isMale = sharedPref.getBoolean("is_male", true)

        val heightInMeter = height / 100
        val bmi = weight / (heightInMeter * heightInMeter)

        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }
        val tdee = (bmr * 1.55) + 300

        val bmiStatus = if(bmi < 18.5) "Underweight" else if(bmi < 24.9) "Normal" else "Overweight"

        binding.tvCalorieTarget.text = "${tdee.toInt()} KCAL"
        binding.tvBmiInfo.text = "BMI: %.1f ($bmiStatus)".format(bmi)
    }

    private fun setupDatabaseAndLoadWorkout() {
        lifecycleScope.launch {
            val count = database.workoutDao().getCount()
            if (count == 0) {
                populateDatabase()
            }

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                else -> "Sunday"
            }

            binding.tvWorkoutTitle.text = "PROGRAM LATIHAN: $dayName"

            val workouts = database.workoutDao().getWorkoutsByDay(dayName)

            if (workouts.isNotEmpty()) {
                adapter.updateData(workouts)
            } else {
                Toast.makeText(requireContext(), "Hari ini Rest Day! Istirahatlah.", Toast.LENGTH_SHORT).show()
                adapter.updateData(emptyList())
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun setupDailyReminder() {
        // WorkManager butuh context
        val workManager = WorkManager.getInstance(requireContext())
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        dueDate.set(Calendar.HOUR_OF_DAY, 16)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("daily_gym_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    // Data dummy tetap sama, tidak ada perubahan logika
    private suspend fun populateDatabase() {
        val dummyData = listOf(
            // --- SENIN (PUSH) ---
            Workout(
                day = "Monday",
                exerciseName = "Bench Press",
                muscleGroup = "Chest",
                sets = "4 Sets",
                reps = "8-12 Reps",
                instructions = "Turunkan bar perlahan ke tengah dada, lalu dorong eksplosif ke atas. Jaga punggung tetap rata di bangku.",
                imageUrl = "https://media.giphy.com/media/l41Yy4J96X8ehz8xG/giphy.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Monday",
                exerciseName = "Overhead Press",
                muscleGroup = "Shoulder",
                sets = "3 Sets",
                reps = "10 Reps",
                instructions = "Berdiri tegak, dorong barbel dari depan bahu lurus ke atas kepala. Jangan melengkungkan punggung.",
                imageUrl = "https://i.makeagif.com/media/11-12-2015/F2x3_m.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Monday",
                exerciseName = "Incline Dumbbell Press",
                muscleGroup = "Chest",
                sets = "3 Sets",
                reps = "10-12 Reps",
                instructions = "Duduk di bangku miring (30-45 derajat), dorong dumbbell ke atas. Fokus pada dada bagian atas.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Incline-Dumbbell-Press.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Monday",
                exerciseName = "Tricep Pushdown",
                muscleGroup = "Tricep",
                sets = "3 Sets",
                reps = "15 Reps",
                instructions = "Gunakan kabel, kunci siku di samping pinggang. Tekan ke bawah hingga lengan lurus.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pushdown.gif",
                isCompleted = false,
                weight = 0.0
            ),

            // --- SELASA (PULL) ---
            Workout(
                day = "Tuesday",
                exerciseName = "Lat Pulldown",
                muscleGroup = "Back",
                sets = "4 Sets",
                reps = "12 Reps",
                instructions = "Duduk tegak, tarik bar ke arah dada atas. Bayangkan siku ditarik ke belakang.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Lat-Pulldown.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Tuesday",
                exerciseName = "Barbell Row",
                muscleGroup = "Back",
                sets = "3 Sets",
                reps = "10 Reps",
                instructions = "Bungkukkan badan 45 derajat, tarik barbel ke arah perut. Kencangkan otot punggung.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bent-Over-Row.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Tuesday",
                exerciseName = "Face Pull",
                muscleGroup = "Rear Delt",
                sets = "3 Sets",
                reps = "15 Reps",
                instructions = "Tarik tali ke arah wajah (hidung/dahi) dengan posisi siku tinggi.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Face-Pull.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Tuesday",
                exerciseName = "Bicep Curl",
                muscleGroup = "Bicep",
                sets = "3 Sets",
                reps = "12 Reps",
                instructions = "Angkat dumbbell dengan menekuk siku. Jangan ayunkan badan.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Curl.gif",
                isCompleted = false,
                weight = 0.0
            ),

            // --- RABU (LEGS) ---
            Workout(
                day = "Wednesday",
                exerciseName = "Barbell Squat",
                muscleGroup = "Legs",
                sets = "4 Sets",
                reps = "8-10 Reps",
                instructions = "Letakkan bar di punggung, jongkok hingga paha sejajar lantai. Dorong dengan tumit.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Squat.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Wednesday",
                exerciseName = "Leg Press",
                muscleGroup = "Legs",
                sets = "3 Sets",
                reps = "12 Reps",
                instructions = "Dorong beban dengan kaki, tapi jangan kunci lutut (jangan lurus total) saat di atas.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Press.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Wednesday",
                exerciseName = "Lunges",
                muscleGroup = "Legs",
                sets = "3 Sets",
                reps = "12 Reps",
                instructions = "Langkah lebar ke depan, turunkan pinggul hingga kedua lutut membentuk 90 derajat.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lunge.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Wednesday",
                exerciseName = "Calf Raise",
                muscleGroup = "Calf",
                sets = "4 Sets",
                reps = "20 Reps",
                instructions = "Jinjit setinggi mungkin, tahan sebentar di atas, lalu turun perlahan.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Calf-Raise.gif",
                isCompleted = false,
                weight = 0.0
            ),

            // --- KAMIS (PUSH Variasi) ---
            Workout(
                day = "Thursday",
                exerciseName = "Dumbbell Shoulder Press",
                muscleGroup = "Shoulder",
                sets = "3 Sets",
                reps = "12 Reps",
                instructions = "Duduk tegak, dorong dumbbell ke atas kepala sampai tangan hampir lurus.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Shoulder-Press.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Thursday",
                exerciseName = "Lateral Raise",
                muscleGroup = "Shoulder",
                sets = "4 Sets",
                reps = "15 Reps",
                instructions = "Angkat tangan ke samping setinggi bahu seperti mengepakkan sayap.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lateral-Raise.gif",
                isCompleted = false,
                weight = 0.0
            ),

            // --- JUMAT (PULL Variasi) ---
            Workout(
                day = "Friday",
                exerciseName = "Deadlift",
                muscleGroup = "Back & Legs",
                sets = "3 Sets",
                reps = "5 Reps",
                instructions = "Angkat beban dari lantai dengan punggung lurus dan dada membusung.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Deadlift.gif",
                isCompleted = false,
                weight = 0.0
            ),
            Workout(
                day = "Friday",
                exerciseName = "Hammer Curl",
                muscleGroup = "Bicep",
                sets = "3 Sets",
                reps = "12 Reps",
                instructions = "Pegang dumbbell netral (seperti memegang palu), angkat ke arah bahu.",
                imageUrl = "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hammer-Curl.gif",
                isCompleted = false,
                weight = 0.0
            )
        )
        database.workoutDao().insertAll(dummyData)
    }

    // PENTING: Hapus binding saat view hancur biar ga bocor memori
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}