package com.example.myapplicationennew

import DatabaseHelper
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.myapplicationennew.R
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val employers = mutableListOf<Employer>()
    private lateinit var linearLayout: LinearLayout
    private lateinit var db: SQLiteDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var menuAddEmployer: Button
    private lateinit var menuAddJob: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        linearLayout = findViewById(R.id.linearLayout)
        drawerLayout = findViewById(R.id.drawerLayout)
        menuButton = findViewById(R.id.menuButton)
        menuAddEmployer = findViewById(R.id.menuAddEmployer)
        menuAddJob = findViewById(R.id.menuAddJob)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        menuAddEmployer.setOnClickListener {
            showInputDialog()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        menuAddJob.setOnClickListener {
            Toast.makeText(this, "Please select an employer first.", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        loadEmployersFromDatabase()
        displayEmployers()
    }

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
                val employer = Employer(id, name, jobs, dateAdded, isDeleted)
                employers.add(employer)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Employer")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout2, null)
        val editTextName = inputLayout.findViewById<EditText>(R.id.textnameWork2)

        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val name = editTextName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val employer = employers.find { it.name == name }
            if (employer == null) {
                val newEmployerId = addEmployerToDatabase(name)
                val newEmployer = Employer(newEmployerId, name, mutableListOf(), getCurrentDate())
                employers.add(newEmployer)
                displayEmployers()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    @SuppressLint("SetTextI18n")
    private fun displayEmployers() {
        linearLayout.removeAllViews()
        for (employer in employers.filter { !it.isDeleted }) {
            val employerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val totalIncome = employer.jobs.sumOf { it.moneyhowmuch }
            val employerTextView = TextView(this).apply {
                text = "Employer: ${employer.name} - Total Income: $totalIncome $"
                setBackgroundResource(android.R.drawable.btn_default)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(14, 14, 14, 14)
                }
                setOnClickListener {
                    displayJobs(employer)
                }
            }

            employerLayout.addView(employerTextView)

            val deleteEmployerButton = Button(this).apply {
                text = "-"
                setBackgroundResource(R.drawable.rounded_button)
                textSize = 20f
                setOnClickListener {
                    deleteEmployer(employer)
                }
            }
            employerLayout.addView(deleteEmployerButton)

            val addJobButton = Button(this).apply {
                text = "+"
                setBackgroundResource(R.drawable.rounded_button)
                textSize = 20f
                setOnClickListener {
                    showJobInputDialog(employer)
                }
            }
            employerLayout.addView(addJobButton)

            linearLayout.addView(employerLayout)
        }
    }

    private fun showJobInputDialog(employer: Employer) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("${employer.name} - Add New Job")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout, null)
        val editTextJob = inputLayout.findViewById<EditText>(R.id.textnameWork)
        val editTextMoney = inputLayout.findViewById<EditText>(R.id.TextMoney)
        val editTextDescription = inputLayout.findViewById<EditText>(R.id.TextDescription)

        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val job = editTextJob.text.toString()
            val moneyhowmuch = editTextMoney.text.toString().toDoubleOrNull() ?: 0.0
            val description = editTextDescription.text.toString()

            addJobToDatabase(employer.id, job, moneyhowmuch, description)
            displayJobs(employer)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    @SuppressLint("SetTextI18n")
    private fun displayJobs(employer: Employer) {
        linearLayout.removeAllViews()

        val backButton = Button(this).apply {
            text = "Back"
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                displayEmployers()
            }
        }
        linearLayout.addView(backButton)

        for (job in employer.jobs) {
            val jobView = TextView(this).apply {
                text = "Work: ${job.name}\nAmount: ${job.moneyhowmuch} $\nDescription: ${job.description}"
                setBackgroundResource(R.drawable.rounded_button)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
                setOnLongClickListener {
                    deleteJob(job)
                    true
                }
            }
            linearLayout.addView(jobView)
        }
    }

    private fun deleteJob(job: Job) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Job")
        builder.setMessage("Are you sure you want to delete this job?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            deleteJobFromDatabase(job.id)
            val employer = employers.find { it.id == job.employerId }
            employer?.jobs?.remove(job)
            employer?.let { displayJobs(it) }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun deleteEmployer(employer: Employer) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Employer")
        builder.setMessage("Are you sure you want to delete this employer and all associated jobs?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            val employerId = employer.id
            val deletedRows = deleteEmployerFromDatabase(employerId)

            if (deletedRows > 0) {
                employer.isDeleted = true
                displayEmployers()
                Toast.makeText(this, "Employer deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete employer", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun deleteJobFromDatabase(jobId: Long) {
        val rowsDeleted = db.delete("Jobs", "id = ?", arrayOf(jobId.toString()))
        if (rowsDeleted > 0) {
            Toast.makeText(this, "Job deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete job", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteEmployerFromDatabase(employerId: Long): Int {
        val contentValues = ContentValues().apply {
            put("isDeleted", 1)
        }
        return db.update("Employers", contentValues, "id = ?", arrayOf(employerId.toString()))
    }

    private fun addEmployerToDatabase(name: String): Long {
        val date = getCurrentDate()
        val employerValues = ContentValues().apply {
            put("name", name)
            put("dateAdded", date)
            put("isDeleted", 0)
        }
        val newId = db.insert("Employers", null, employerValues)
        Toast.makeText(this,
            if (newId == -1L) "Failed to add employer" else "Employer added successfully",
            Toast.LENGTH_SHORT).show()
        return newId
    }

    private fun addJobToDatabase(employerId: Long, name: String, moneyhowmuch: Double, description: String) {
        val date = getCurrentDate()
        val jobValues = ContentValues().apply {
            put("employer_id", employerId)
            put("name", name)
            put("moneyhowmuch", moneyhowmuch)
            put("description", description)
            put("dateAdded", date)
        }
        val jobId = db.insert("Jobs", null, jobValues)

        if (jobId != -1L) {
            val employer = employers.find { it.id == employerId }
            if (employer != null) {
                employer.jobs.clear()
                employer.jobs.addAll(loadJobsForEmployer(employer.id))
            }
            Toast.makeText(this, "Job added successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to add job", Toast.LENGTH_SHORT).show()
        }
        displayEmployers()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

    fun openCalculator(view: View) {
        val intent = Intent(this, CalculatorActivity::class.java)
        startActivity(intent)
    }

    private fun loadJobsForEmployer(employerId: Long): MutableList<Job> {
        val jobs = mutableListOf<Job>()
        val cursor = db.query("Jobs", null, "employer_id = ?", arrayOf(employerId.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val jobId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val moneyhowmuch = cursor.getDouble(cursor.getColumnIndexOrThrow("moneyhowmuch"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                val dateAdded = cursor.getString(cursor.getColumnIndexOrThrow("dateAdded"))
                val job = Job(jobId, employerId, name, moneyhowmuch, description, dateAdded)
                jobs.add(job)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return jobs
    }
}

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
    val description: String,
    val dateAdded: String,
)
