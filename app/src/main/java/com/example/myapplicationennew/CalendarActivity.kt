package com.example.myapplicationennew

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
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
    private lateinit var dbHelper: DatabaseHelper

    private val fmtDefault = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val fmtTr = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendar = findViewById(R.id.materialCalendar)
        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        txtSummary = findViewById(R.id.txtSummary)
        listEmployers = findViewById(R.id.listEmployers)
        dbHelper = DatabaseHelper(this)

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        decorateCalendar()

        calendar.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            onDateSelected(date)
        })

        val today = CalendarDay.today()
        calendar.selectedDate = today
        onDateSelected(today)
    }

    private fun onDateSelected(day: CalendarDay) {
        val cal = Calendar.getInstance().apply {
            set(day.year, day.month , day.day)
        }

        val dateStr = fmtDefault.format(cal.time)
        txtSelectedDate.text = dateStr

        // ---- 1) İŞVEREN LİSTESİ ----
        val employers = dbHelper.getEmployersAddedOn(dateStr)

        if (employers.isEmpty()) {
            txtSummary.text = "Bu tarihte eklenen işveren yok."
        } else {
            txtSummary.text = "Bu tarihte eklenen işveren sayısı: ${employers.size}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, employers)
        listEmployers.adapter = adapter

        // İşveren tıklanınca → işlerini göster
        listEmployers.setOnItemClickListener { _, _, position, _ ->
            val employerName = employers[position]
            val employerId = dbHelper.getEmployerIdByName(employerName)

            val jobs = dbHelper.getJobsAddedOnForEmployer(dateStr, employerId)

            if (jobs.isEmpty()) {
                txtSummary.text = "$employerName için bu tarihte iş yok."
                listEmployers.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("İş bulunamadı"))
            } else {
                txtSummary.text = "$employerName için ${jobs.size} iş bulundu"

                val jobNames = jobs.map { "• ${it.name} (₺${it.moneyhowmuch})" }
                val jobAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, jobNames)
                listEmployers.adapter = jobAdapter
            }
        }
    }

    private fun decorateCalendar() {
        val countsByDate = dbHelper.getEmployerCountsByDate()
        val daysWithEmployers = mutableSetOf<CalendarDay>()

        for ((dateStr, _) in countsByDate) {
            parseDate(dateStr)?.let { date ->
                val cal = Calendar.getInstance().apply { time = date }
                val day = CalendarDay.from(cal)
                daysWithEmployers.add(day)
            }
        }

        calendar.addDecorator(EventDotDecorator(daysWithEmployers))
    }

    private fun parseDate(dateStr: String): Date? {
        return try {
            fmtDefault.parse(dateStr)
        } catch (_: ParseException) {
            try {
                fmtTr.parse(dateStr)
            } catch (_: Exception) {
                null
            }
        }
    }

    private class EventDotDecorator(private val days: Set<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = days.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f))
        }
    }
}
