package com.example.myapplicationennew

//Bu satırlar, Android uygulamasının temel bileşenlerini içe aktarır ve
// bir aktivite sınıfı oluşturmak için kullanılır.
// Burada "DatabaseHelper" adında bir sınıf veya modül içe aktarılıyor
// farklı Android bileşenlerini içe aktarırlar. Örneğin, düğme (Button),
// diyaloglar (AlertDialog), veritabanı işlemleri için kullanılan sınıflar
// (ContentValues, SQLiteDatabase), günlükleme (Log) ve daha fazlasını içerirler.
// tarih ve saat biçimlemesi için kullanılan sınıfları ve yerel ayarları içe
// aktarır. Özellikle, "SimpleDateFormat" ile tarih ve saat değerleri
// biçimlenir ve "Locale" ile belirli bir dil ve bölge ayarı belirtilir.
import DatabaseHelper
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//silinen employer lar historıye gitmiyo
//ikinci employer eklendıgınde eklenen ıs bırıncı employera gıdıyo

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

        // Veritabanı bağlantısını oluştur
        val dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        // İşverenleri görüntüleme işlemi
        displayEmployers()
    }

    // add employer basıldıgndda cıkan pencere işlemi
    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Employer")

        val inputLayout = layoutInflater.inflate(R.layout.input_dialog_layout2, null)
        val editTextName = inputLayout.findViewById<EditText>(R.id.textnameWork2)

        builder.setView(inputLayout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val name = editTextName.text.toString()
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
    @SuppressLint("SetTextI18n", "ResourceAsColor")
    private fun displayEmployers() {
        // Mevcut işverenleri temizle
        linearLayout.removeAllViews()
        for (employer in employers) {
            // Her işvereni görüntüleme
            val employerLayout = LinearLayout(this)
            employerLayout.orientation = LinearLayout.HORIZONTAL
            val employerTextView = TextView(this)
            employerTextView.text = "Employer: ${employer.name} - Added on: ${employer.dateAdded}"
            employerTextView.setBackgroundResource(android.R.drawable.btn_default)
            employerTextView.setPadding(16, 16, 16, 16)
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            params.setMargins(14, 14, 14, 14)
            employerTextView.layoutParams = params
            employerTextView.setOnClickListener {
                displayJobs(employer)
                addButton.visibility = View.GONE
            }
            employerLayout.addView(employerTextView)

            // İşveren silme işlemi
            val deleteEmployerButton = Button(this)
            deleteEmployerButton.text = "-"
            deleteEmployerButton.setBackgroundResource(R.drawable.rounded_button)
            deleteEmployerButton.textSize = 20f
            deleteEmployerButton.setOnClickListener {
                deleteEmployer(employer)
            }
            employerLayout.addView(deleteEmployerButton)

            // Yeni iş ilanı eklemek için düğme
            val addJobButton = Button(this)
            addJobButton.text = "+"
            addJobButton.setBackgroundResource(R.drawable.rounded_button)
            addJobButton.textSize = 20f
            addJobButton.setOnClickListener {
                showJobInputDialog(employer)
            }
            employerLayout.addView(addJobButton)

            // İşvereni ekranın altına ekleyin
            linearLayout.addView(employerLayout)
        }
    }
    // "History" düğmesine tıkladığınızda işverenleri ve iş ilanlarını geçmiş görünümüne geçirmeyi yöneten işlev
    @SuppressLint("SetTextI18n")
    private fun displayHistory() {
        linearLayout.removeAllViews()

        val backButton = Button(this)
        backButton.text = "Back"
        backButton.setBackgroundResource(android.R.drawable.btn_default)
        backButton.setOnClickListener {
            displayEmployers()
            addButton.visibility = View.VISIBLE
            historyButton.visibility = View.VISIBLE
        }
        linearLayout.addView(backButton)

        // Sadece silinmiş işverenleri filtrele
        val deletedEmployers = employers.filter { it.isDeleted }

        for (employer in deletedEmployers) {
            val employerLayout = LinearLayout(this)
            employerLayout.orientation = LinearLayout.HORIZONTAL
            val employerTextView = TextView(this)
            employerTextView.text = "Employer: ${employer.name} - Added on: ${employer.dateAdded}"
            employerTextView.setBackgroundResource(android.R.drawable.btn_default)
            employerTextView.setPadding(16, 16, 16, 16)
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            params.setMargins(14, 14, 14, 14)
            employerTextView.layoutParams = params
            employerLayout.addView(employerTextView)

            linearLayout.addView(employerLayout)
        }
        historyButton.visibility = View.GONE
    }


    // Yeni iş ilanı eklemek için bir diyalog penceresi görüntüle
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
            val moneyhowmuch = editTextMoney.text.toString().toIntOrNull() ?: 0
            val description = editTextDescription.text.toString() // asılklama degırı

            // İş ilanını işverenin altına ekleyin ve veritabanına ekleyin
            addJobToDatabase(employer.id, job, moneyhowmuch, description)

            // Yeni iş ilanını ekledikten sonra işverenin iş ilanlarını görüntüleyin
            displayJobs(employer)

            editTextJob.text.clear()
            editTextMoney.text.clear()
            editTextDescription.text.clear() // Açıklama girişini temizleme

            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
    // Belirli bir işverenin işlerini görüntüleme işlemi
    @SuppressLint("SetTextI18n")
    private fun displayJobs(employer: Employer) {
        // İşverene ait işleri getirin
        linearLayout.removeAllViews()
        val backButton = Button(this)
        backButton.text = "Back"
        backButton.setBackgroundResource(android.R.drawable.btn_default)
        backButton.setOnClickListener {
            displayEmployers()
            addButton.visibility = View.VISIBLE
        }
        linearLayout.addView(backButton)

        for (job in employer.jobs) {
            // Her işi görüntüleme (eklenen işler ekleme)
            val jobTextView = TextView(this)
            val jobDetails = "Work: ${job.name}\nAmount: ${job.moneyhowmuch} $\nDescription: ${job.description}"
            jobTextView.text = jobDetails
            jobTextView.setBackgroundResource(R.drawable.rounded_button)
            jobTextView.setPadding(16, 16, 16, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 16, 16, 16)
            jobTextView.layoutParams = params
            linearLayout.addView(jobTextView)

            // İş silme işlemi
            val deleteJobButton = Button(this)
            deleteJobButton.text = "Delete Job"
            deleteJobButton.setOnClickListener {
                deleteJob(job)
            }
            linearLayout.addView(deleteJobButton)
        }
    }
    // İşlerin silme işlemi
    private fun deleteJob(job: Job) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Job")
        builder.setMessage("Are you sure you want to delete this job?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            // İş ilanını veritabanından sil
            deleteJobFromDatabase(job.id)

            // İşverenin işlerini yeniden görüntüle
            val employer = employers.find { it.id == job.employerId }
            if (employer != null) {
                employer.jobs.remove(job)
                displayJobs(employer)
            }

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    // İşvereni silme işlemi
    private fun deleteEmployer(employer: Employer) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Employer")
        builder.setMessage("Are you sure you want to delete this employer and all associated jobs?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            // İşvereni ve ilişkili işleri sil
            employer.isDeleted = true

            val employerId = employer.id
            deleteEmployerFromDatabase(employerId)

            // İşvereni listeden kaldır
             employers.remove(employer)
            // İşverenleri ve işleri yeniden görüntüle
            displayEmployers()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    // İşlerin veritabanından silme işlemi
    private fun deleteJobFromDatabase(jobId: Long) {
        // Veritabanından işi sil
        db.delete("Jobs", "id = ?", arrayOf(jobId.toString()))
    }
    // İşvereni ve ilişkili işleri veritabanından silme işlemi
    private fun deleteEmployerFromDatabase(employerId: Long) {
        // İşvereni veritabanından sil
        db.delete("Employers", "id = ?", arrayOf(employerId.toString()))

        // İşverene ait tüm işleri de sil
        db.delete("Jobs", "employer_id = ?", arrayOf(employerId.toString()))
    }
    // Yeni bir işvereni veritabanına ekleme işlemi
    private fun addEmployerToDatabase(name: String): Long {
        val date = getCurrentDate() // Tarih bilgisini alın
        val employerValues = ContentValues().apply {
            put("name", name)
            put("dateAdded", date)
        }
        return db.insert("Employers", null, employerValues)
    }
    // Yeni bir işleri veritabanına eklemek için işlem
    private fun addJobToDatabase(employerId: Long, name: String, moneyhowmuch: Int, description: String) {
        val date = getCurrentDate() // Tarih bilgisini alın
        val jobValues = ContentValues().apply {
            put("employer_id", employerId)
            put("name", name)
            put("moneyhowmuch", moneyhowmuch)
            put("description", description)
            put("dateAdded", date)
        }
        val jobId = db.insert("Jobs", null, jobValues)

        // Veritabanına iş başarıyla eklendi. Şimdi işverenin iş listesine yeni işi ekleyin.
        val employer = employers.find { it.id == employerId }
        if (employer != null) {
            val newJob = Job(jobId, employerId, name, moneyhowmuch.toString(), description, date)
            employer.jobs.add(newJob)
        }
        // İşverenleri güncelle
        displayEmployers()
    }

    // Geçerli tarihi almak için işlem
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }
    // Uygulama sonlandığında veritabanı bağlantısını kapatma işlemi
    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}

// İşveren sınıfı
data class Employer(val id: Long, val name: String, val jobs: MutableList<Job>, val dateAdded: String, var isDeleted: Boolean = false)
// is sinifi
data class Job(val id: Long, val employerId: Long, val name: String, val moneyhowmuch: String, val description: String, val dateAdded: String)
