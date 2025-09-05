package com.example.myapplicationennew

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*



class CalendarActivity : AppCompatActivity() {

    private lateinit var calendar: MaterialCalendarView
    private lateinit var txtSelectedDate: TextView
    private lateinit var txtSummary: TextView
    private lateinit var listEmployers: ListView
    private lateinit var db: DatabaseHelper

    // Uygulamada kullanılan format: "dd MMMM yyyy"
    private val fmtDefault = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val fmtTr = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar) // :contentReference[oaicite:2]{index=2}

        db = DatabaseHelper(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        calendar = findViewById(R.id.materialCalendar)
        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        txtSummary = findViewById(R.id.txtSummary)
        listEmployers = findViewById(R.id.listEmployers)

        // 1) Takvime noktaları yükle
        decorateCalendarDots()

        // 2) Başlangıçta bugün seçili gibi davran
        val today = CalendarDay.today()
        calendar.selectedDate = today
        loadDay(today)

        // 3) Gün seçimi
        calendar.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            loadDay(date)
        })
    }

    override fun onResume() {
        super.onResume()
        // MainActivity'de yeni işveren ekledikten geri dönünce noktaları tazele
        decorateCalendarDots()
        calendar.selectedDate?.let { loadDay(it) }
    }

    /** Veritabanından (isDeleted=0) işveren sayısı olan günleri çekip noktaları basar. */
    private fun decorateCalendarDots() {
        val countsByDate = db.getEmployerCountsByDate() // "dd MMMM yyyy" -> count
        calendar.removeDecorators()

        if (countsByDate.isEmpty()) return

        val daysWithEvents = mutableSetOf<CalendarDay>()
        for ((dateStr, _) in countsByDate) {
            val date = parseDaySafely(dateStr) ?: continue
            val cal = Calendar.getInstance().apply { time = date }
            daysWithEvents.add(
                CalendarDay.from(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,   // MCV 1-12 bekler
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        calendar.addDecorator(EventDotDecorator(daysWithEvents))
    }

    /** Seçilen günün işverenlerini yükle ve listele. */
    private fun loadDay(day: CalendarDay) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.month - 1) // Calendar 0-11 bekler
            set(Calendar.DAY_OF_MONTH, day.day)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dateStr = fmtDefault.format(cal.time)
        txtSelectedDate.text = "Seçilen gün: $dateStr"

        val employers = db.getEmployersAddedOn(dateStr) // sadece isDeleted=0
        txtSummary.text = if (employers.isEmpty()) {
            "Bu günde eklenmiş işveren yok."
        } else {
            "Bu günde eklenen işveren sayısı: ${employers.size}"
        }

        listEmployers.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            employers.map { it.name }
        )

        listEmployers.setOnItemClickListener { _, _, position, _ ->
            val e = employers[position]
            Toast.makeText(this, "İşveren: ${e.name}", Toast.LENGTH_SHORT).show()
            // İstersen buradan işverene gitme/detay açılabilir.
        }
    }

    /** "dd MMMM yyyy" parse - cihaz dili ve TR denemesiyle güvenli. */
    private fun parseDaySafely(dateStr: String): Date? {
        return try {
            fmtDefault.parse(dateStr)
        } catch (_: ParseException) {
            try { fmtTr.parse(dateStr) } catch (_: ParseException) { null }
        } catch (_: Exception) { null }
    }

    /** Nokta dekoratörü: verilen günlere birer dot çizer. */
    private class EventDotDecorator(private val days: Set<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = days.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f)) // 8dp çapında nokta
        }
    }
}
