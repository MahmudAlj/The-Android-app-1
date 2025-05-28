//Bu satırlar, Android uygulamasının temel bileşenlerini içe aktarır ve
// bir aktivite sınıfı oluşturmak için kullanılır.
// Burada "DatabaseHelper" adında bir sınıf veya modül içe aktarılıyor
// farklı Android bileşenlerini içe aktarırlar. Örneğin, düğme (Button),
// diyaloglar (AlertDialog), veritabanı işlemleri için kullanılan sınıflar
// (ContentValues, SQLiteDatabase), günlükleme (Log) ve daha fazlasını içerirler.
// tarih ve saat biçimlemesi için kullanılan sınıfları ve yerel ayarları içe
// aktarır. Özellikle, "SimpleDateFormat" ile tarih ve saat değerleri
// biçimlenir ve "Locale" ile belirli bir dil ve bölge ayarı belirtilir.

//silinen employer lar historıye gitmiyo
// calculate buttonu hıstorye gırıce gozukmesın
//calculate ekle
// back buttonu en altta yada en ustte bır ok
//takvim ekleme
// employerda tarih ve kazanılan toplam para
//calculate ve takvim yandan kaydırmalı bır pencere olsun
// eklenen işlerdeki uzun basımda detaylarin çikmasi
//bir işi silerken buttonla değil uzun basımda silinmesi
// bir iş eklerken direk iş verenin içine girip sağ üstten arti buttonuna basarak eklensin
// silme ekleme tuşlari kaldirip direk iş verenin içinde olucak
// Mahmud
//yeni eklemeler jeneksmvksmkldmvsrmjkdgreksfmkwlkvdsc
// yeni ekleme
package com.example.myapplicationennew
import DatabaseHelper
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.room.util.query

@SuppressLint("StaticFieldLeak")
private lateinit var addButton: Button

class MainActivity : AppCompatActivity() {
    private val employers = mutableListOf<Employer>() //işveren listesi
    private lateinit var linearLayout: LinearLayout // UI da isveren ve isleri gostermek ıcın kullanılır
    private lateinit var db: SQLiteDatabase // verı tabanı baglantısı
    private lateinit var historyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Veritabanı bağlantısını oluştur
        val dbHelper: DatabaseHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        // Gerekli görünümleri tanımla ve başlat
        linearLayout = findViewById(R.id.linearLayout)
        addButton = findViewById(R.id.newButton)
        "Add Employer".also { addButton.text = it }
        addButton.setOnClickListener {
            showInputDialog() // İşveren eklemek için diyalog penceresini göster
        }
        historyButton = findViewById(R.id.historyButton)
        "History".also { historyButton.text = it }
        historyButton.setOnClickListener {
            displayHistory()  // hıstoryı acmak ıcın penceresını goster
        }

        loadEmployersFromDatabase() // İşverenleri yükle
        // İşverenleri görüntüleme işlemi
        displayEmployers()
    }
    class CalculatorActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_calculator)
        }

        fun calculate(view: View) {
            // Temel işlemler: toplama, çıkarma, çarpma, bölme
            val num1 = findViewById<EditText>(R.id.num1).text.toString().toDoubleOrNull()
            val num2 = findViewById<EditText>(R.id.num2).text.toString().toDoubleOrNull()
            val resultTextView = findViewById<TextView>(R.id.resultTextView)

            if (num1 != null && num2 != null) {
                when (view.id) {
                    R.id.addButton -> resultTextView.text = "Result: ${num1 + num2}"
                    R.id.subtractButton -> resultTextView.text = "Result: ${num1 - num2}"
                    R.id.multiplyButton -> resultTextView.text = "Result: ${num1 * num2}"
                    R.id.divideButton -> {
                        if (num2 != 0.0) {
                            resultTextView.text = "Result: ${num1 / num2}"
                        } else {
                            resultTextView.text = "Cannot divide by zero"
                        }
                    }
                }
            } else {
                resultTextView.text = "Please enter valid numbers"
            }
        }
    }

    //Veritabanından işverenleri çekmek
    private fun loadEmployersFromDatabase() {
        val cursor = db.query("Employers", null, null, null, null, null, null)
        employers.clear() // Önce işveren listesini temizle
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val dateAdded = cursor.getString(cursor.getColumnIndexOrThrow("dateAdded"))

                // İşverenin işlerini çekmek için:
                val jobs = loadJobsForEmployer(id)

                val employer = Employer(id, name, jobs, dateAdded)
                employers.add(employer)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    // add employer basıldıgndda cıkan pencere işlemi
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
                // Yeni işvereni veritabanına ekleyin
                val newEmployerId = addEmployerToDatabase(name)
                val newEmployer = Employer(newEmployerId, name, mutableListOf(), getCurrentDate())
                employers.add(newEmployer)
                // İşverenleri güncelle
                displayEmployers()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    // İşverenleri görüntüleme yeri(ana sayfa)
    // İşveren listesini güncelleyip tarih yerine toplam kazancı gösteriyoruz.
    @SuppressLint("SetTextI18n", "ResourceAsColor")
    private fun displayEmployers() {
        linearLayout.removeAllViews()
        for (employer in employers) {
            val employerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // İşverenin adını ve toplam kazancını gösteren TextView
            val totalIncome = employer.jobs.sumOf { it.moneyhowmuch } // Toplam kazanç
            val employerTextView = TextView(this).apply {
                text = "Employer: ${employer.name} - Total Income: $totalIncome $"
                setBackgroundResource(android.R.drawable.btn_default)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(14, 14, 14, 14)
                }
                setOnClickListener {
                    displayJobs(employer)
                    addButton.visibility = View.GONE
                }
            }

            employerLayout.addView(employerTextView)

            // İşveren silme butonu
            val deleteEmployerButton = Button(this).apply {
                text = "-"
                setBackgroundResource(R.drawable.rounded_button)
                textSize = 20f
                setOnClickListener {
                    deleteEmployer(employer)
                }
            }
            employerLayout.addView(deleteEmployerButton)

            // İş ekleme butonu
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

    // Silinen işverenlerin gösterildiği history ekranı
    @SuppressLint("SetTextI18n")
    private fun displayHistory() {
        linearLayout.removeAllViews()

        val backButton = Button(this).apply {
            text = "Back"
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                displayEmployers()
                addButton.visibility = View.VISIBLE
                historyButton.visibility = View.VISIBLE
            }
        }
        linearLayout.addView(backButton)

        val deletedEmployers = employers.filter { it.isDeleted }
        for (employer in deletedEmployers) {
            val employerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val employerTextView = TextView(this).apply {
                text = "Employer: ${employer.name} - Deleted"
                setBackgroundResource(android.R.drawable.btn_default)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(14, 14, 14, 14)
                }
            }
            employerLayout.addView(employerTextView)

            linearLayout.addView(employerLayout)
        }
        historyButton.visibility = View.GONE
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

            // İş ilanını işverenin altına ekleyin ve veritabanına ekleyin
            addJobToDatabase(employer.id, job, moneyhowmuch, description)

            // Yeni iş ilanını ekledikten sonra işverenin iş ilanlarını görüntüleyin
            displayJobs(employer)

            editTextJob.text.clear()
            editTextMoney.text.clear()
            editTextDescription.text.clear()

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
                addButton.visibility = View.VISIBLE
                historyButton.visibility = View.VISIBLE
            }
        }
        linearLayout.addView(backButton)

        for (job in employer.jobs) {
            val jobTextView = TextView(this).apply {
                val jobDetails = "Work: ${job.name}\nAmount: ${job.moneyhowmuch} $\nDescription: ${job.description}"
                text = jobDetails
                setBackgroundResource(R.drawable.rounded_button)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(16, 16, 16, 16)
                }
            }
            linearLayout.addView(jobTextView)

            val deleteJobButton = Button(this).apply {
                text = "Delete Job"
                setOnClickListener {
                    deleteJob(job)
                }
            }
            linearLayout.addView(deleteJobButton)
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
        val jobDeleted = db.delete("Jobs", "employer_id = ?", arrayOf(employerId.toString()))
        val employerDeleted = db.delete("Employers", "id = ?", arrayOf(employerId.toString()))
        return employerDeleted // Kaç satırın silindiğini döndür
    }

    private fun addEmployerToDatabase(name: String): Long {
        val date = getCurrentDate()
        val employerValues = ContentValues().apply {
            put("name", name)
            put("dateAdded", date)
        }
        val newId = db.insert("Employers", null, employerValues)
        if (newId == -1L) {
            Toast.makeText(this, "Failed to add employer", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Employer added successfully", Toast.LENGTH_SHORT).show()
        }
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

        // Veritabanına iş başarıyla eklendi. Şimdi işverenin iş listesine yeni işi ekleyin.
        if (jobId != -1L) {
            val employer = employers.find { it.id == employerId }
            if (employer != null) {
                val newJob = Job(jobId, employerId, name, moneyhowmuch.toDouble(), description, date)
                employer.jobs.add(newJob)
            }
            Toast.makeText(this, "Job added successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to add job", Toast.LENGTH_SHORT).show()
        }

        // İşverenleri güncelle
        displayEmployers()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
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
            val moneyhowmuch = cursor.getInt(cursor.getColumnIndexOrThrow("moneyhowmuch")).toDouble()
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

// İşveren sınıfı
data class Employer(
    val id: Long,
    val name: String,
    val jobs: MutableList<Job>,
    val dateAdded: String,
    var isDeleted: Boolean = false,
)
// is sinifi
data class Job(
    val id: Long,
    val employerId: Long,
    val name: String,
    val moneyhowmuch: Double,
    val description: String,
    val dateAdded: String,
)
