package com.example.myapplicationennew

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listContainer: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DatabaseHelper(this)
        listContainer = findViewById(R.id.historyListContainer)
        emptyText = findViewById(R.id.historyEmptyText)

        val btnBack: Button = findViewById(R.id.historyBackBtn)
        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        populateList()
    }

    private fun populateList() {
        listContainer.removeAllViews()

        val items = mutableListOf<HistoryItem>()
        dbHelper.getDeletedJobs().use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(0)
                val name = c.getString(1)
                val amount = c.getDouble(2)
                val employerName = c.getString(3) ?: "Unknown"
                items.add(HistoryItem(id, name, amount, employerName))
            }
        }

        if (items.isEmpty()) {
            emptyText.text = "Silinmiş iş bulunmuyor."
            emptyText.visibility = android.view.View.VISIBLE
            return
        } else {
            emptyText.visibility = android.view.View.GONE
        }

        for (item in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }

            val title = TextView(this).apply {
                text = "${item.name} • ₺${item.amount} (${item.employer})"
                textSize = 16f
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val restoreBtn = Button(this).apply {
                text = "Geri Yükle"
                setOnClickListener {
                    dbHelper.restoreJob(item.id)
                    Toast.makeText(this@HistoryActivity, "İş geri yüklendi", Toast.LENGTH_SHORT).show()
                    populateList()
                }
            }

            val deleteBtn = Button(this).apply {
                text = "Kalıcı Sil"
                setOnClickListener {
                    dbHelper.hardDeleteJob(item.id)
                    Toast.makeText(this@HistoryActivity, "İş kalıcı olarak silindi", Toast.LENGTH_SHORT).show()
                    populateList()
                }
            }

            actions.addView(
                restoreBtn,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            actions.addView(
                deleteBtn,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )

            row.addView(title)
            row.addView(actions)

            listContainer.addView(row)
        }
    }

    data class HistoryItem(val id: Int, val name: String, val amount: Double, val employer: String)
}
