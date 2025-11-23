package com.example.myapplicationennew
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.card.MaterialCardView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// iş eklerken gıder ekleme orda olucak
// takvımde ıslerenın eklendıgı ısler gozukmesı gerekıyor
//iş silme uzun tutma
//hesap makınesı tekrar bakılacak
// total dogru hesaplanmıyor gıderıde ekleme bır sey olmasın o dırek ıs te bır secenek olsun gıder dıye  ısım gıbı olucak ama totalde eklenıcek - olarak
// ıs veya ıs verende ayar dıye bır sey olsun onun otomatık onaylama yada sılme orda olsun
// işlerde sadece ücret ve iş olsun ustune bastıgında dıger detaylar cıksın
// uc nokta hep gozuksun

// bunlardan baska gırıs sayfası maıl ıle gırıs maıl ıle kaydolus
// reklamı aarastırma nasıl google playa yuklıyebılırız


class MainActivity : AppCompatActivity() {

    // --- UI ---
    private lateinit var linearLayout: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageButton
    private lateinit var menuAddEmployer: Button
    private lateinit var menuAddExpense: Button
    private var menuHistoryBtn: Button? = null
    private var menuCalendarBtn: Button? = null
    private var menuIncompleteBtn: Button? = null
    private var totalText: TextView? = null

    // --- Veri/DB ---
    private val employers = mutableListOf<Employer>()
    private lateinit var db: SQLiteDatabase
    private lateinit var dbHelper: DatabaseHelper

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    displayEmployers()
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
        findViewById<Button>(R.id.menuAddExpense).setOnClickListener {
            showExpenseDialog()
        }

        dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        // View refs
        linearLayout   = findViewById(R.id.linearLayout)
        drawerLayout   = findViewById(R.id.drawerLayout)
        menuButton     = findViewById(R.id.menuButton)
        menuAddEmployer= findViewById(R.id.menuAddEmployer)
        menuAddExpense  = findViewById(R.id.menuAddExpense)
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
        menuAddExpense.setOnClickListener {       // 🔹 yeni
            showAddExpenseDialog()
            drawerLayout.closeDrawer(GravityCompat.END)
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
    private fun showAddExpenseDialog() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Add Expense")

        val view = layoutInflater.inflate(R.layout.expense_dialog_layout, null)
        val editAmount = view.findViewById<EditText>(R.id.editExpenseAmount)
        val editDesc   = view.findViewById<EditText>(R.id.editExpenseDescription)
        val editType   = view.findViewById<EditText>(R.id.editExpenseType)

        builder.setView(view)

        builder.setPositiveButton("Save") { dialog, _ ->
            val amountText = editAmount.text.toString().replace(",", ".").trim()
            val amount = amountText.toDoubleOrNull()
            val desc = editDesc.text.toString().trim()
            val type = editType.text.toString().trim()

            if (amount == null) {
                Toast.makeText(this, "Amount must be a number", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val date = getCurrentDate()
            dbHelper.addExpense(amount, desc, if (type.isEmpty()) null else type, date)

            ActionLogger.log(this, "Expense added: $amount ₺, $desc")
            updateTotalInDrawer()
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun displayEmployers() {
        linearLayout.removeAllViews()
        for (employer in employers.filter { !it.isDeleted }) {
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                radius = 16f
                cardElevation = 6f
                setContentPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 16, 16, 16) }
            }

            val tv = TextView(this).apply {
                val totalIncome = employer.jobs
                    .filter { !it.isDeleted && it.isDone }
                    .sumOf { it.moneyhowmuch }
                text = "👤 ${employer.name}\n📅 ${employer.dateAdded}\n💰 Toplam: ₺$totalIncome"
                textSize = 16f
                setTextColor(getColor(android.R.color.black))
            }

            card.addView(tv)
            card.setOnClickListener { displayJobs(employer) }
            card.setOnLongClickListener {
                deleteEmployer(employer)
                true
            }
            linearLayout.addView(card)
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

        val addJobBtn = Button(this).apply {
            text = "➕ Yeni İş Ekle"
            setOnClickListener { showJobInputDialog(employer) }
        }
        linearLayout.addView(addJobBtn)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
        }
        val backBtn = Button(this).apply {
            text = "⬅ Geri"
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener { displayEmployers() }
        }
        val title = TextView(this).apply {
            text = "👤 ${employer.name} • 📅 ${employer.dateAdded}"
            textSize = 18f
        }

        header.addView(backBtn)
        header.addView(title)
        linearLayout.addView(header)

        // İş kartları
        val inflater = LayoutInflater.from(this)

        for (job in employer.jobs.filter { !it.isDeleted }) {

            // XML'den kartı yükle
            val card = inflater.inflate(R.layout.job_item, linearLayout, false)
            val check = card.findViewById<CheckBox>(R.id.checkJob)

            check.text =
                        "🛠 İş: ${job.name}\n" +
                        "💰 Ücret: ₺${job.moneyhowmuch}\n" +
                        "📍 Mekan: ${job.place}\n" +
                        "📝 Açıklama: ${job.description}\n" +
                        "📅 Tarih: ${job.dateAdded}"

            check.isChecked = job.isDone

            check.setOnCheckedChangeListener { _, checked ->
                dbHelper.setJobDone(job.id.toInt(), checked)
                job.isDone = checked
                updateTotalInDrawer()
            }

            // 🔥 Uzun basınca sil
            card.setOnLongClickListener {
                softDeleteJob(job, employer)
                true
            }

            // 🔥 Kaydırma hareketi
            var downX = 0f
            var isSwiping = false

            card.setOnTouchListener { v, event ->

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        isSwiping = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.x - downX

                        if (Math.abs(deltaX) > 20) isSwiping = true

                        if (isSwiping) {
                            v.translationX = deltaX
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val deltaX = event.x - downX

                        when {
                            deltaX > 150 -> {
                                // sağ kaydırma → tamamlandı
                                dbHelper.setJobDone(job.id.toInt(), true)
                                job.isDone = true
                                check.isChecked = true
                                updateTotalInDrawer()
                                Toast.makeText(this, "Tamamlandı ✓", Toast.LENGTH_SHORT).show()
                            }
                            deltaX < -150 -> {
                                // sol kaydırma → tamamlanmadı
                                dbHelper.setJobDone(job.id.toInt(), false)
                                job.isDone = false
                                check.isChecked = false
                                updateTotalInDrawer()
                                Toast.makeText(this, "Tamamlanmadı!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // kartı yerine döndür
                        v.animate().translationX(0f).setDuration(150).start()
                    }
                }

                true
            }

            linearLayout.addView(card)
        }
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

    private fun updateTotalInDrawer() {
        val income = dbHelper.getTotalEarned()
        val expenses = dbHelper.getTotalExpenses()
        val net = income - expenses

        totalText?.text = "₺ " + String.format(Locale.getDefault(), "%.2f", net)

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


    private fun showExpenseDialog() {
        val layout = layoutInflater.inflate(R.layout.input_dialog_layout, null)

        val name = layout.findViewById<EditText>(R.id.textnameWork)
        val money = layout.findViewById<EditText>(R.id.TextMoney)

        AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(layout)
            .setPositiveButton("Add") { dialog, _ ->
                val expName = name.text.toString().trim()
                val amount = money.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

                dbHelper.addExpense(expName, amount, getCurrentDate())
                updateTotalInDrawer()

                Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            // 🔹 CardView
            val card = androidx.cardview.widget.CardView(this).apply {
                radius = 20f
                cardElevation = 8f
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(20, 20, 20, 20)
                }
            }

            // 🔹 İç layout
            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }

            // 🔹 Job bilgisi
            val tv = TextView(this).apply {
                this.text = text
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.black, theme))
            }

            // 🔹 Action butonları
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 0)
            }

            val markDone = Button(this).apply {
                this.text = "Tamamla"
                setOnClickListener {
                    dbHelper.setJobDone(jobId.toInt(), true)
                    updateTotalInDrawer()
                    displayIncompleteJobs()
                }
            }
            val deleteBtn = Button(this).apply {
                this.text = "Sil"
                setOnClickListener {
                    dbHelper.softDeleteJob(jobId.toInt())
                    updateTotalInDrawer()
                    displayIncompleteJobs()
                }
            }

            actions.addView(markDone, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(deleteBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            innerLayout.addView(tv)
            innerLayout.addView(actions)
            card.addView(innerLayout)

            linearLayout.addView(card)
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
