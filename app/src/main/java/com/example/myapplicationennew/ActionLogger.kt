package com.example.myapplicationennew

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

object ActionLogger {

    private const val DB_NAME = "action_log.db"
    private const val TABLE_ACTION_LOG = "action_log"
    private const val COL_LOG_ID = "id"
    private const val COL_LOG_MESSAGE = "message"
    private const val COL_LOG_TIME = "time_millis"

    private fun getDb(context: Context): SQLiteDatabase {
        val db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)
        ensureTable(db)
        return db
    }

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

    fun log(context: Context, message: String) {
        try {
            val db = getDb(context)
            val values = ContentValues().apply {
                put(COL_LOG_MESSAGE, message)
                put(COL_LOG_TIME, System.currentTimeMillis())
            }
            db.insert(TABLE_ACTION_LOG, null, values)
            db.close()
        } catch (_: Exception) {
        }
    }
}
