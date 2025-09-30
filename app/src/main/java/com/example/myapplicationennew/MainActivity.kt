package com.example.myapplicationennew
// toplam para yanliş hesapliyor  ✅ düzeltildi (sadece isDone=true toplanıyor)
// günlük diye bir seçenek olsun işler otomatik onaylansin  ✅ eklendi (CheckBox ile)
// iş verendeki gelir onaylamadiğim halde hepsini toplama atiyor  ✅ senkronize edildi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
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
    private var menuHistoryBtn: Button? = null
    private var menuCalendarBtn: Button? = null
    private var menuIncompleteBtn: Button? = null   // (opsiyonel) Tamamlanmayan işler
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
        linearLayout   = findViewById(R.id.linearLayout)
        drawerLayout   = findViewById(R.id.drawerLayout)
        menuButton     = findViewById(R.id.menuButton)
        menuAddEmployer= findViewById(R.id.menuAddEmployer)
        menuHistoryBtn = findViewById(R.id.menuHistory)
        menuCalendarBtn= findViewById(R.id.menuCalendar)
        menuIncompleteBtn = findViewById(R.id.menuIncomplete) // XML'de yoksa null olur, sorun değil
        totalText      = findViewById(R.id.menuTotalValue)

        // Drawer
        menuButton.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        menuAddEmployer.setOnClickListener {
            showEmployerInputDialog()
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        menuHistoryBtn?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        menuCalendarBtn?.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        menuIncompleteBtn?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            displayIncompleteJobs()
        }

        // Başlangıç
        loadEmployersFromDatabase()
        displayEmployers()
        updateTotalInDrawer()
    }

    override fun onResume() {
        super.onResume()
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
        val checkAutoApprove = inputLayout.findViewById<CheckBox>(R.id.checkAutoApprove)

        builder.setView(inputLayout)
        builder.setPositiveButton("Add") { dialog, _ ->
            val name = editTextName.text.toString().trim()
            val autoApprove = checkAutoApprove.isChecked
            if (name.isEmpty()) {
                Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val exists = employers.any { it.name.equals(name, ignoreCase = true) && !it.isDeleted }
            if (!exists) {
                val newEmployerId = addEmployerToDatabase(name, autoApprove)
                val newEmployer = Employer(
                    newEmployerId,
                    name,
                    mutableListOf(),
                    getCurrentDate(),
                    isDeleted = false,
                    autoApprove = autoApprove
                )
                employers.add(newEmployer)
                ActionLogger.log(this, "İşveren eklendi: $name")
                displayEmployers()
            } else {
                Toast.makeText(this, "Employer already exists", Toast.LENGTH_SHORT).show()
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
            val tv = TextView(this).apply {
                val totalIncome = employer.jobs
                    .filter { !it.isDeleted && it.isDone }   // ✅ sadece onaylanmış işler
                    .sumOf { it.moneyhowmuch }
                text = "Employer: ${employer.name}\nEklenme: ${employer.dateAdded}  •  Toplam: $totalIncome ₺"
                setSingleLine(false)
                setBackgroundResource(android.R.drawable.btn_default)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(14, 14, 14, 14) }

                setOnClickListener { displayJobs(employer) }
                setOnLongClickListener {
                    deleteEmployer(employer)
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
                val rows = softDeleteEmployerInDatabase(employer.id)
                dbHelper.softDeleteJobsByEmployer(employer.id)
                if (rows > 0) {
                    employer.isDeleted = true
                    employer.jobs.forEach { it.isDeleted = true }
                    updateTotalInDrawer()
                    displayEmployers()
                    Toast.makeText(this, "Employer deleted", Toast.LENGTH_SHORT).show()
                    ActionLogger.log(this, "İşveren silindi: ${employer.name}")
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
        val checkDaily = inputLayout.findViewById<CheckBox>(R.id.checkDaily)

        builder.setView(inputLayout)
        builder.setPositiveButton("Add") { dialog, _ ->
            val jobName = editTextJob.text.toString().trim()
            val money = editTextMoney.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val desc = editTextDescription.text.toString().trim()
            val place = editTextPlace.text.toString().trim()

            // ✅ Öncelik işverende autoApprove, yoksa günlük seçeneği
            val isDoneValue = if (employer.autoApprove) {
                1
            } else {
                if (checkDaily.isChecked) 1 else 0
            }
            addJobToDatabase(employer.id, jobName, money, place, desc, isDoneValue)
            ActionLogger.log(this, "İş eklendi: ${employer.name} • $jobName • ₺$money • $place")
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

        // Üst bar
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
            text = "${employer.name} • ${employer.dateAdded}"
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

        // İş kartları
        for (job in employer.jobs.filter { !it.isDeleted }) {
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 16, 16, 16) }
                setBackgroundResource(R.drawable.rounded_button)
                setOnLongClickListener {
                    showJobActionsDialog(employer, job)
                    true
                }
            }

            val check = CheckBox(this).apply {
                text = "İş: ${job.name}\nÜcret: ${job.moneyhowmuch} ₺\nMekan: ${job.place}\nAçıklama: ${job.description}\nTarih: ${job.dateAdded}"
                isChecked = job.isDone
                setOnCheckedChangeListener { _, checked ->
                    dbHelper.setJobDone(job.id.toInt(), checked)
                    job.isDone = checked
                    updateTotalInDrawer()
                    // İşveren toplamı ekranda da güncellensin:
                    displayJobs(employer)
                    ActionLogger.log(this@MainActivity, "İş ${if (checked) "tamamlandı" else "geri alındı"}: ${job.name} (${employer.name})")
                }
                setOnLongClickListener {
                    showJobActionsDialog(employer, job)
                    true
                }
            }

            container.addView(check, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            linearLayout.addView(container)
        }
    }

    // UZUN BAS MENÜSÜ: DÜZENLE / SİL
    private fun showJobActionsDialog(employer: Employer, job: Job) {
        val options = arrayOf("Düzenle", "Sil", "İptal")
        AlertDialog.Builder(this)
            .setTitle(job.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditJobDialog(employer, job)
                    1 -> softDeleteJob(job, employer)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    // İŞ DÜZENLEME DİYALOĞU
    private fun showEditJobDialog(employer: Employer, job: Job) {
        val builder = AlertDialog.Builder(this)
            .setTitle("İşi Düzenle")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout, null)
        val editTextJob = inputLayout.findViewById<EditText>(R.id.textnameWork)
        val editTextMoney = inputLayout.findViewById<EditText>(R.id.TextMoney)
        val editTextDescription = inputLayout.findViewById<EditText>(R.id.TextDescription)
        val editTextPlace = inputLayout.findViewById<EditText>(R.id.TextPlace)

        editTextJob.setText(job.name)
        editTextMoney.setText(job.moneyhowmuch.toString())
        editTextDescription.setText(job.description)
        editTextPlace.setText(job.place)

        builder.setView(inputLayout)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = editTextJob.text.toString().trim()
            val newMoney = editTextMoney.text.toString().replace(",", ".").toDoubleOrNull()
            val newDesc = editTextDescription.text.toString().trim()
            val newPlace = editTextPlace.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (newMoney == null) {
                Toast.makeText(this, "Money must be a number", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val rows = updateJobInDatabase(job.id, newName, newMoney, newPlace, newDesc)
            if (rows > 0) {
                val updated = job.copy(
                    name = newName,
                    moneyhowmuch = newMoney,
                    description = newDesc,
                    place = newPlace
                )
                val index = employer.jobs.indexOfFirst { it.id == job.id }
                if (index >= 0) employer.jobs[index] = updated

                updateTotalInDrawer()
                displayJobs(employer)
                Toast.makeText(this, "Job updated", Toast.LENGTH_SHORT).show()
                ActionLogger.log(this, "İş güncellendi: ${job.name} → $newName (${employer.name})")
            } else {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        builder.show()
    }

    // İŞİ SOFT DELETE
    private fun softDeleteJob(job: Job, employer: Employer) {
        AlertDialog.Builder(this)
            .setTitle("İşi Sil")
            .setMessage("\"${job.name}\" silinsin mi? (History'e taşınacak)")
            .setPositiveButton("Sil") { d, _ ->
                val ok = softDeleteJobInDatabase(job.id)
                if (ok > 0) {
                    employer.jobs.firstOrNull { it.id == job.id }?.isDeleted = true
                    updateTotalInDrawer()
                    displayJobs(employer)
                    Toast.makeText(this, "İş silindi", Toast.LENGTH_SHORT).show()
                    ActionLogger.log(this, "İş silindi: ${job.name} (${employer.name})")
                } else {
                    Toast.makeText(this, "Silme başarısız", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    // ---------- TOTAL ----------
    private fun updateTotalInDrawer() {
        val sum = dbHelper.getTotalEarned() // sadece isDone=1, isDeleted=0
        totalText?.text = "₺ " + String.format(Locale.getDefault(), "%.2f", sum)
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
                val autoApprove = cursor.getInt(cursor.getColumnIndexOrThrow("autoApprove")) == 1
                val jobs = loadJobsForEmployer(id)
                employers.add(
                    Employer(id, name, jobs, dateAdded, isDeleted, autoApprove)
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
    }



    private fun loadJobsForEmployer(employerId: Long): MutableList<Job> {
        val jobs = mutableListOf<Job>()
        val cursor = db.query("Jobs", null, "employer_id = ?", arrayOf(employerId.toString()), null, null, null)

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

    private fun addEmployerToDatabase(name: String, autoApprove: Boolean): Long {
        val date = getCurrentDate()
        val values = ContentValues().apply {
            put("name", name)
            put("dateAdded", date)
            put("isDeleted", 0)
            put("autoApprove", if (autoApprove) 1 else 0) // ✅ yeni sütun
        }
        return db.insert("Employers", null, values)
    }



    private fun addJobToDatabase(
        employerId: Long,
        name: String,
        moneyhowmuch: Double,
        place: String,
        description: String,
        isDoneValue: Int
    ) {
        val date = getCurrentDate()
        val values = ContentValues().apply {
            put("employer_id", employerId)
            put("name", name)
            put("moneyhowmuch", moneyhowmuch)
            put("place", place)
            put("description", description)
            put("dateAdded", date)
            put("isDone", isDoneValue)   // ✅ CheckBox’a göre ayarlanıyor
            put("isDeleted", 0)
        }
        db.insert("Jobs", null, values)
        updateTotalInDrawer()
    }

    private fun updateJobInDatabase(jobId: Long, name: String, moneyhowmuch: Double, place: String, description: String): Int {
        val values = ContentValues().apply {
            put("name", name)
            put("moneyhowmuch", moneyhowmuch)
            put("place", place)
            put("description", description)
        }
        return db.update("Jobs", values, "id = ?", arrayOf(jobId.toString()))
    }

    // SOFT DELETE: Jobs -> isDeleted=1
    private fun softDeleteJobInDatabase(jobId: Long): Int {
        dbHelper.softDeleteJob(jobId.toInt())
        return 1
    }

    // SOFT DELETE: Employers -> isDeleted=1
    private fun softDeleteEmployerInDatabase(employerId: Long): Int {
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

    // ---------- Tamamlanmayan İşler (opsiyonel) ----------
    @SuppressLint("SetTextI18n")
    private fun displayIncompleteJobs() {
        linearLayout.removeAllViews()

        // Üst bar
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
            text = "Tamamlanmayan İşler"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(12, 0, 12, 0)
        }
        header.addView(backBtn)
        header.addView(title)
        linearLayout.addView(header)

        // Jobs + Employer isimlerini JOIN ile çek
        val list = mutableListOf<Triple<String, String, Long>>() // (jobText, employerName, jobId)
        db.rawQuery(
            """
            SELECT j.id, j.name, j.moneyhowmuch, j.place, j.description, j.dateAdded, e.name
            FROM Jobs j
            LEFT JOIN Employers e ON j.employer_id = e.id
            WHERE j.isDone = 0 AND j.isDeleted = 0 AND (e.isDeleted = 0 OR e.isDeleted IS NULL)
            ORDER BY j.id DESC
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                val jobId = c.getLong(0)
                val name = c.getString(1) ?: ""
                val money = c.getDouble(2)
                val place = c.getString(3) ?: ""
                val desc = c.getString(4) ?: ""
                val date = c.getString(5) ?: ""
                val empName = c.getString(6) ?: "—"
                val text = "İş: $name\nÜcret: $money ₺\nMekan: $place\nAçıklama: $desc\nTarih: $date\nİşveren: $empName"
                list.add(Triple(text, empName, jobId))
            }
        }

        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Tamamlanmayan iş yok."
                setPadding(16,16,16,16)
            }
            linearLayout.addView(empty)
            return
        }

        for ((text, _, jobId) in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 12, 16, 12) }
                setBackgroundResource(R.drawable.rounded_button)
            }
            val tv = TextView(this).apply {
                this.text = text
                setSingleLine(false)
            }
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val markDone = Button(this).apply {
                val text = "Tamamla"
                setOnClickListener {
                    dbHelper.setJobDone(jobId.toInt(), true)
                    updateTotalInDrawer()
                    displayIncompleteJobs()
                }
            }
            val deleteBtn = Button(this).apply {
                val text = "Sil"
                setOnClickListener {
                    dbHelper.softDeleteJob(jobId.toInt())
                    updateTotalInDrawer()
                    displayIncompleteJobs()
                }
            }
            actions.addView(markDone, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(deleteBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            row.addView(tv)
            row.addView(actions)
            linearLayout.addView(row)
        }
    }
}

// --- Veri sınıfları ---
data class Employer(
    val id: Long,
    val name: String,
    val jobs: MutableList<Job>,
    val dateAdded: String,
    var isDeleted: Boolean = false,
    var autoApprove: Boolean = false
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
