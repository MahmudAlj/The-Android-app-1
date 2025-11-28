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
import com.google.android.material.floatingactionbutton.FloatingActionButton

// iş silme uzun tutma
// ıs veya ıs verende ayar dıye bır sey olsun onun otomatık onaylama yada sılme orda olsun
// işlerde sadece ücret ve iş olsun ustune bastıgında dıger detaylar cıksın
// uc nokta hep gozuksun
// tekrar eden tuslari ayni kullanmak icin ayri xml de tanimla
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

        val fabAddJob = findViewById<FloatingActionButton>(R.id.fab_add_job)
        fabAddJob.setOnClickListener {
            val currentEmployer = null.also {
                it?.let { it1 -> showJobInputDialog(it1) }
            }
        }


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

        val activeEmployers = employers.filter { !it.isDeleted }

        // 2’şerli gruplamak için index’leri dolaş
        var index = 0
        while (index < activeEmployers.size) {

            // Yeni bir satır layout (horizontal)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(12, 12, 12, 12)
                }
            }

            // 1. kart
            val emp1 = activeEmployers[index]
            row.addView(createEmployerCard(emp1))

            // Eğer 2. kart varsa ekle
            if (index + 1 < activeEmployers.size) {
                val emp2 = activeEmployers[index + 1]
                row.addView(createEmployerCard(emp2))
            }

            linearLayout.addView(row)
            index += 2
        }
    }
    // 🔥 Kart oluşturan fonksiyon
    private fun createEmployerCard(employer: Employer): View {
        val card = com.google.android.material.card.MaterialCardView(this).apply {
            radius = 16f
            cardElevation = 6f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            setContentPadding(24, 24, 24, 24)
        }

        val totalIncome = employer.jobs
            .filter { !it.isDeleted && it.isDone }
            .sumOf { it.moneyhowmuch }

        val tv = TextView(this).apply {
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

        return card
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

        val jobNameTxt = inputLayout.findViewById<EditText>(R.id.textnameWork)
        val moneyTxt = inputLayout.findViewById<EditText>(R.id.TextMoney)
        val placeTxt = inputLayout.findViewById<EditText>(R.id.TextPlace)
        val descTxt = inputLayout.findViewById<EditText>(R.id.TextDescription)
        val expenseTxt = inputLayout.findViewById<EditText>(R.id.ExpenseAmount)
        val checkDaily = inputLayout.findViewById<CheckBox>(R.id.checkDaily)

        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->

            val name = jobNameTxt.text.toString().trim()
            val desc = descTxt.text.toString().trim()
            val place = placeTxt.text.toString().trim()

            val amount = moneyTxt.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            var expenseAmount = expenseTxt.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

            // 🔥 Gider varsa negatif yapıyoruz
            if (expenseAmount > 0) {
                expenseAmount = -expenseAmount
            }

            val isDone = if (checkDaily.isChecked) 1 else 0

            // 🔥 Job kaydı
            addJobToDatabase(
                employer.id,
                name,
                amount + expenseAmount,  // toplam iş tutarı + gider
                place,
                desc,
                isDone
            )

            refreshEmployerJobs(employer)
            displayJobs(employer)
            updateTotalInDrawer()

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { d, _ -> d.dismiss() }
        builder.show()
    }
    @SuppressLint("SetTextI18n")
    private fun displayJobs(employer: Employer) {

        linearLayout.removeAllViews()

        // ➕ FAB (sağ altta)
        val addBtn = Button(this).apply {
            text = "＋"
            textSize = 30f
            setPadding(20, 20, 20, 20)
            setBackgroundResource(R.drawable.fab_circle)
            setOnClickListener { showJobInputDialog(employer) }
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 32, 32)
        }
        addBtn.layoutParams = params

        // Üst bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
        }
        val backBtn = Button(this).apply {
            text = "⬅ Geri"
            setOnClickListener { displayEmployers() }
        }
        val title = TextView(this).apply {
            text = "👤 ${employer.name} • 📅 ${employer.dateAdded}"
            textSize = 18f
        }

        header.addView(backBtn)
        header.addView(title)
        linearLayout.addView(header)

        val inflater = LayoutInflater.from(this)

        for (job in employer.jobs.filter { !it.isDeleted }) {

            val card = inflater.inflate(R.layout.job_item, linearLayout, false)
            val check = card.findViewById<CheckBox>(R.id.checkJob)
            val moreBtn = card.findViewById<ImageButton>(R.id.moreButton)

            check.text =
                "🛠 ${job.name}\n" +
                        "💰 ₺${job.moneyhowmuch}\n" +
                        "📅 ${job.dateAdded}"

            check.isChecked = job.isDone

            check.setOnCheckedChangeListener { _, checked ->
                dbHelper.setJobDone(job.id.toInt(), checked)
                job.isDone = checked
                updateTotalInDrawer()
            }

            // 🔥 Üç nokta menü (Düzenle — Sil)
            moreBtn.setOnClickListener {
                val popup = android.widget.PopupMenu(this, moreBtn)
                popup.menu.add("Düzenle")
                popup.menu.add("Sil")

                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Düzenle" -> showEditJobDialog(employer, job)
                        "Sil" -> softDeleteJob(job, employer)
                    }
                    true
                }
                popup.show()
            }

            // 🔥 Kaydırarak tamamla / geri al
            card.setOnTouchListener(object : View.OnTouchListener {
                var downX = 0f
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    event ?: return false

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                        }
                        MotionEvent.ACTION_UP -> {
                            val deltaX = event.x - downX
                            if (deltaX > 150) {
                                check.isChecked = true
                                dbHelper.setJobDone(job.id.toInt(), true)
                                updateTotalInDrawer()
                                Toast.makeText(this@MainActivity, "Tamamlandı ✓", Toast.LENGTH_SHORT).show()
                            }
                            if (deltaX < -150) {
                                check.isChecked = false
                                dbHelper.setJobDone(job.id.toInt(), false)
                                updateTotalInDrawer()
                                Toast.makeText(this@MainActivity, "Tamamlanmadı!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    return true
                }
            })

            linearLayout.addView(card)
        }

        // ➕ buton en sona
        linearLayout.addView(addBtn)
    }

    // İŞ DÜZENLEME DİYALOĞU
    private fun showEditJobDialog(employer: Employer, job: Job) {

        val builder = AlertDialog.Builder(this).setTitle("İşi Düzenle")

        val layout = layoutInflater.inflate(R.layout.input_dialog_layout, null)

        val nameTxt = layout.findViewById<EditText>(R.id.textnameWork)
        val moneyTxt = layout.findViewById<EditText>(R.id.TextMoney)
        val placeTxt = layout.findViewById<EditText>(R.id.TextPlace)
        val descTxt = layout.findViewById<EditText>(R.id.TextDescription)
        val expTxt = layout.findViewById<EditText>(R.id.ExpenseAmount)
        val dailyChk = layout.findViewById<CheckBox>(R.id.checkDaily)

        nameTxt.setText(job.name)
        placeTxt.setText(job.place)
        descTxt.setText(job.description)
        dailyChk.isChecked = job.isDone

        if (job.moneyhowmuch < 0)
            expTxt.setText((-job.moneyhowmuch).toString())
        else
            moneyTxt.setText(job.moneyhowmuch.toString())

        builder.setView(layout)

        builder.setPositiveButton("Kaydet") { d, _ ->

            val name = nameTxt.text.toString().trim()
            val amount = moneyTxt.text.toString().toDoubleOrNull() ?: 0.0
            var expense = expTxt.text.toString().toDoubleOrNull() ?: 0.0

            if (expense > 0) expense = -expense

            val finalAmount = amount + expense

            updateJobInDatabase(job.id, name, finalAmount, placeTxt.text.toString(), descTxt.text.toString(), if (dailyChk.isChecked) 1 else 0)

            refreshEmployerJobs(employer)
            displayJobs(employer)
            updateTotalInDrawer()

            Toast.makeText(this, "İş güncellendi", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("İptal", null)
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
        val net = dbHelper.getTotalNet()
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
    private fun addJobToDatabase(employerId: Long, name: String, moneyhowmuch: Double, place: String, description: String, isDoneValue: Int) {
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
    private fun updateJobInDatabase(jobId: Long, name: String, amount: Double, place: String, desc: String, isDone: Int) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.JOB_NAME, name)
            put(DatabaseHelper.JOB_AMOUNT, amount)
            put(DatabaseHelper.JOB_PLACE, place)
            put(DatabaseHelper.JOB_DESC, desc)
            put(DatabaseHelper.JOB_IS_DONE, isDone)
        }

        db.update(
            DatabaseHelper.TBL_JOBS,
            values,
            "${DatabaseHelper.JOB_ID} = ?",
            arrayOf(jobId.toString())
        )
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
    var name: String,
    var moneyhowmuch: Double,
    var place: String,
    var description: String,
    val dateAdded: String,
    var isDone: Boolean = false,
    var isDeleted: Boolean = false,
)
