package com.example.myapplicationennew

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // --- UI ---
    private lateinit var linearLayout: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var menuAddEmployer: Button
    // opsiyonel (XML'de varsa doldurulur)
    private var menuHistoryBtn: Button? = null
    private var totalText: TextView? = null

    // --- Veri/DB ---
    private val employers = mutableListOf<Employer>()
    private lateinit var db: SQLiteDatabase
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        // View refs
        linearLayout = findViewById(R.id.linearLayout)
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        menuAddEmployer = findViewById(R.id.menuAddEmployer)
        menuHistoryBtn = findViewById(R.id.menuHistory)           // activity_main.xml'den gelir
        totalText = findViewById(R.id.menuTotalValue)             // activity_main.xml'den gelir

        // Drawer
        menuButton.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        menuAddEmployer.setOnClickListener {
            showEmployerInputDialog()
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        // DİKKAT: Artık dialog değil, tam ekran liste
        menuHistoryBtn?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Başlangıç
        loadEmployersFromDatabase()
        displayEmployers()
        updateTotalInDrawer()
    }

    override fun onResume() {
        super.onResume()
        // History ekranından dönünce liste ve TOTAL güncellensin
        loadEmployersFromDatabase()
        displayEmployers()
        updateTotalInDrawer()
    }

    // ---------- İşverenler ----------
    private fun showEmployerInputDialog() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Add New Employer")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout2, null)
        val editTextName = inputLayout.findViewById<EditText>(R.id.textnameWork2)
        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val name = editTextName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val exists = employers.any { it.name.equals(name, ignoreCase = true) && !it.isDeleted }
            if (!exists) {
                val newEmployerId = addEmployerToDatabase(name)
                val newEmployer = Employer(newEmployerId, name, mutableListOf(), getCurrentDate(), isDeleted = false)
                employers.add(newEmployer)
                displayEmployers()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun displayEmployers() {
        linearLayout.removeAllViews()
        for (employer in employers.filter { !it.isDeleted }) {
            // Tek satır: tıkla → içeri gir, uzun bas → işvereni sil
            val tv = TextView(this).apply {
                val totalIncome = employer.jobs.sumOf { it.moneyhowmuch } // mevcut davranış korunuyor
                text = "Employer: ${employer.name} - Total Income: $totalIncome $"
                setBackgroundResource(android.R.drawable.btn_default)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(14, 14, 14, 14) }

                setOnClickListener { displayJobs(employer) }
                setOnLongClickListener {
                    deleteEmployer(employer) // soft delete
                    true
                }
            }
            linearLayout.addView(tv)
        }
    }

    private fun deleteEmployer(employer: Employer) {
        AlertDialog.Builder(this)
            .setTitle("Delete Employer")
            .setMessage("Are you sure you want to delete this employer and all associated jobs?")
            .setPositiveButton("Delete") { d, _ ->
                val rows = deleteEmployerFromDatabase(employer.id)
                if (rows > 0) {
                    employer.isDeleted = true
                    displayEmployers()
                    Toast.makeText(this, "Employer deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete employer", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- İşler ----------
    private fun showJobInputDialog(employer: Employer) {
        val builder = AlertDialog.Builder(this)
            .setTitle("${employer.name} - Add New Job")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout, null)
        val editTextJob = inputLayout.findViewById<EditText>(R.id.textnameWork)
        val editTextMoney = inputLayout.findViewById<EditText>(R.id.TextMoney)
        val editTextDescription = inputLayout.findViewById<EditText>(R.id.TextDescription)
        val editTextPlace = inputLayout.findViewById<EditText>(R.id.TextPlace)
        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val jobName = editTextJob.text.toString()
            val money = editTextMoney.text.toString().toDoubleOrNull() ?: 0.0
            val desc = editTextDescription.text.toString()
            val place = editTextPlace.text.toString()

            addJobToDatabase(employer.id, jobName, money, place, desc)
            refreshEmployerJobs(employer)
            displayJobs(employer)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun displayJobs(employer: Employer) {
        linearLayout.removeAllViews()

        // ÜST BAR: Sol "Back", ortada işveren adı, SAĞ ÜSTTE "+"
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
        }

        val backBtn = Button(this).apply {
            text = "Back"
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener { displayEmployers() }
        }
        val title = TextView(this).apply {
            text = employer.name
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(12, 0, 12, 0)
        }
        val addJobBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            contentDescription = "Add Job"
            setBackgroundResource(android.R.color.transparent)
            setOnClickListener { showJobInputDialog(employer) }
        }

        header.addView(backBtn)
        header.addView(title)
        header.addView(addJobBtn)
        linearLayout.addView(header)

        // Sadece silinmemiş işleri göster
        for (job in employer.jobs.filter { !it.isDeleted }) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 16, 16, 16) }
                setBackgroundResource(R.drawable.rounded_button)
                // UZUN BAS: işi sil (History'e taşı)
                setOnLongClickListener {
                    softDeleteJob(job, employer)
                    true
                }
            }

            val check = CheckBox(this).apply {
                text = "İş: ${job.name}\nÜcret: ${job.moneyhowmuch} ₺\nMekan: ${job.place}\nAçıklama: ${job.description}"
                isChecked = job.isDone
                setOnCheckedChangeListener { _, checked ->
                    dbHelper.setJobDone(job.id.toInt(), checked)
                    job.isDone = checked
                    updateTotalInDrawer()
                }
            }

            container.addView(check, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            linearLayout.addView(container)
        }
    }

    private fun softDeleteJob(job: Job, employer: Employer) {
        AlertDialog.Builder(this)
            .setTitle("Delete Job")
            .setMessage("Move this job to History?")
            .setPositiveButton("OK") { d, _ ->
                dbHelper.softDeleteJob(job.id.toInt())
                job.isDeleted = true
                employer.jobs.removeAll { it.id == job.id }
                updateTotalInDrawer()
                displayJobs(employer)
                Toast.makeText(this, "Job moved to History", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- TOTAL ----------
    private fun updateTotalInDrawer() {
        val sum = dbHelper.getTotalEarned()
        totalText?.text = "₺ " + String.format("%.2f", sum)
    }

    // ---------- DB ----------
    private fun loadEmployersFromDatabase() {
        val cursor = db.query("Employers", null, null, null, null, null, null)
        employers.clear()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val dateAdded = cursor.getString(cursor.getColumnIndexOrThrow("dateAdded"))
                val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("isDeleted")) == 1
                val jobs = loadJobsForEmployer(id)
                employers.add(Employer(id, name, jobs, dateAdded, isDeleted))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun loadJobsForEmployer(employerId: Long): MutableList<Job> {
        val jobs = mutableListOf<Job>()
        val cursor = db.query("Jobs", null, "employer_id = ?", arrayOf(employerId.toString()), null, null, null)
        // Kolonların varlığını güvenli kontrol et (eski DB'de olmayabilir)
        val idxDone = cursor.getColumnIndex("isDone")
        val idxDel = cursor.getColumnIndex("isDeleted")

        if (cursor.moveToFirst()) {
            do {
                val jobId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val moneyhowmuch = cursor.getDouble(cursor.getColumnIndexOrThrow("moneyhowmuch"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val place = cursor.getString(cursor.getColumnIndexOrThrow("place"))
                val dateAdded = cursor.getString(cursor.getColumnIndexOrThrow("dateAdded"))
                val isDone = if (idxDone >= 0) cursor.getInt(idxDone) == 1 else false
                val isDeleted = if (idxDel >= 0) cursor.getInt(idxDel) == 1 else false
                jobs.add(Job(jobId, employerId, name, moneyhowmuch, place, description, dateAdded, isDone, isDeleted))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return jobs
    }

    private fun addEmployerToDatabase(name: String): Long {
        val date = getCurrentDate()
        val values = ContentValues().apply {
            put("name", name)
            put("dateAdded", date)
            put("isDeleted", 0)
        }
        val newId = db.insert("Employers", null, values)
        Toast.makeText(this, if (newId == -1L) "Failed to add employer" else "Employer added successfully", Toast.LENGTH_SHORT).show()
        return newId
    }

    private fun addJobToDatabase(employerId: Long, name: String, moneyhowmuch: Double, place: String, description: String) {
        val date = getCurrentDate()
        val values = ContentValues().apply {
            put("employer_id", employerId)
            put("name", name)
            put("moneyhowmuch", moneyhowmuch)
            put("place", place)
            put("description", description)
            put("dateAdded", date)
        }
        val jobId = db.insert("Jobs", null, values)
        if (jobId != -1L) {
            Toast.makeText(this, "Job added successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to add job", Toast.LENGTH_SHORT).show()
        }
        updateTotalInDrawer()
    }

    private fun deleteEmployerFromDatabase(employerId: Long): Int {
        val cv = ContentValues().apply { put("isDeleted", 1) }
        return db.update("Employers", cv, "id = ?", arrayOf(employerId.toString()))
    }

    private fun refreshEmployerJobs(employer: Employer) {
        employer.jobs.clear()
        employer.jobs.addAll(loadJobsForEmployer(employer.id))
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

    // ---------- Hesap Makinesi ----------
    fun openCalculator(view: View) {
        startActivity(Intent(this, CalculatorActivity::class.java))
    }
}

// --- Veri sınıfları ---
data class Employer(
    val id: Long,
    val name: String,
    val jobs: MutableList<Job>,
    val dateAdded: String,
    var isDeleted: Boolean = false,
)

data class Job(
    val id: Long,
    val employerId: Long,
    val name: String,
    val moneyhowmuch: Double,
    val place: String,
    val description: String,
    val dateAdded: String,
    var isDone: Boolean = false,
    var isDeleted: Boolean = false,
)
