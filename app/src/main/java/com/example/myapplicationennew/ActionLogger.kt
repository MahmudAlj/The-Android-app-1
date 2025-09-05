package com.example.myapplicationennew

import android.content.Context
import android.database.sqlite.SQLiteDatabase


object ActionLogger {

    private const val TABLE_ACTION_LOG = "action_log"
    private const val COL_LOG_ID = "id"
    private const val COL_LOG_MESSAGE = "message"
    private const val COL_LOG_TIME = "time_millis"

    private fun ensureTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ACTION_LOG (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_MESSAGE TEXT NOT NULL,
                $COL_LOG_TIME INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    fun log(context: Context, message: String, timeMillis: Long = System.currentTimeMillis()) {
        val helper = DatabaseHelper(context)
        val db = helper.writableDatabase
        ensureTable(db)
        db.execSQL(
            "INSERT INTO $TABLE_ACTION_LOG ($COL_LOG_MESSAGE, $COL_LOG_TIME) VALUES (?,?)",
            arrayOf(message, timeMillis)
        )
    }

    fun queryDay(context: Context, startMillis: Long, endMillis: Long): List<Pair<Long, String>> {
        val helper = DatabaseHelper(context)
        val db = helper.readableDatabase
        ensureTable(db)
        val list = mutableListOf<Pair<Long, String>>()
        db.rawQuery(
            """
            SELECT $COL_LOG_TIME, $COL_LOG_MESSAGE
            FROM $TABLE_ACTION_LOG
            WHERE $COL_LOG_TIME >= ? AND $COL_LOG_TIME < ?
            ORDER BY $COL_LOG_TIME DESC
            """.trimIndent(),
            arrayOf(startMillis.toString(), endMillis.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(c.getLong(0) to c.getString(1))
            }
        }
        return list
    }
}
