package com.example.myapplicationennew

import android.os.Bundle
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

        findViewById<Button>(R.id.historyBackBtn).setOnClickListener { finish() }

        populateList()
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
                val name = c.getString(1) ?: "—"
                val amt = c.getDouble(2)
                val emp = c.getString(3) ?: "—"
                items += HistoryItem(id, name, amt, emp)
            }
        }

        if (items.isEmpty()) {
            emptyText.text = "Silinmiş iş yok."
            emptyText.visibility = android.view.View.VISIBLE
            return
        } else {
            emptyText.visibility = android.view.View.GONE
        }

        for (item in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.rounded_button)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(16, 12, 16, 12)
                layoutParams = params
            }

            val title = TextView(this).apply {
                text = "${item.name} • ₺${String.format("%.2f", item.amount)}  (${item.employer})"
                textSize = 16f
            }
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val restoreBtn = Button(this).apply {
                text = "Geri Yükle"
                setOnClickListener {
                    dbHelper.restoreJob(item.id)
                    populateList()
                    Toast.makeText(this@HistoryActivity, "Geri yüklendi", Toast.LENGTH_SHORT).show()
                }
            }
            val deleteBtn = Button(this).apply {
                text = "Kalıcı Sil"
                setOnClickListener {
                    dbHelper.hardDeleteJob(item.id)
                    populateList()
                    Toast.makeText(this@HistoryActivity, "Kalıcı silindi", Toast.LENGTH_SHORT).show()
                }
            }

            actions.addView(restoreBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(deleteBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(title)
            row.addView(actions)

            listContainer.addView(row)
        }
    }

    data class HistoryItem(val id: Int, val name: String, val amount: Double, val employer: String)
}
