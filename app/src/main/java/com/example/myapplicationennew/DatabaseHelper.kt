package com.example.myapplicationennew

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MyAppDatabase.db"
        // v5: Jobs tablosuna isDone + isDeleted eklendi, toplam için yardımcı metotlar eklendi
        private const val DATABASE_VERSION = 5

        const val TBL_EMPLOYERS = "Employers"
        const val EMP_ID = "id"
        const val EMP_NAME = "name"
        const val EMP_DATE_ADDED = "dateAdded"
        const val EMP_IS_DELETED = "isDeleted"

        const val TBL_JOBS = "Jobs"
        const val JOB_ID = "id"
        const val JOB_EMPLOYER_ID = "employer_id"
        const val JOB_NAME = "name"
        const val JOB_AMOUNT = "moneyhowmuch"
        const val JOB_PLACE = "place"
        const val JOB_DESC = "description"
        const val JOB_DATE_ADDED = "dateAdded"
        const val JOB_IS_DONE = "isDone"
        const val JOB_IS_DELETED = "isDeleted"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TBL_EMPLOYERS (
                $EMP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $EMP_NAME TEXT,
                $EMP_DATE_ADDED TEXT,
                $EMP_IS_DELETED INTEGER DEFAULT 0
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TBL_JOBS (
                $JOB_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $JOB_EMPLOYER_ID INTEGER,
                $JOB_NAME TEXT,
                $JOB_AMOUNT REAL,
                $JOB_PLACE TEXT,
                $JOB_DESC TEXT,
                $JOB_DATE_ADDED TEXT,
                $JOB_IS_DONE INTEGER DEFAULT 0,
                $JOB_IS_DELETED INTEGER DEFAULT 0,
                FOREIGN KEY($JOB_EMPLOYER_ID) REFERENCES $TBL_EMPLOYERS($EMP_ID) ON DELETE CASCADE
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Artımsal ve verisiz kayıp
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TBL_EMPLOYERS ADD COLUMN $EMP_IS_DELETED INTEGER DEFAULT 0;")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_PLACE TEXT DEFAULT '';")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_IS_DONE INTEGER DEFAULT 0;")
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_IS_DELETED INTEGER DEFAULT 0;")
        }
    }

    // ------- Yardımcı Metotlar -------

    fun setJobDone(jobId: Int, done: Boolean) {
        val values = ContentValues().apply { put(JOB_IS_DONE, if (done) 1 else 0) }
        writableDatabase.update(TBL_JOBS, values, "$JOB_ID=?", arrayOf(jobId.toString()))
    }

    fun softDeleteJob(jobId: Int) {
        val values = ContentValues().apply { put(JOB_IS_DELETED, 1) }
        writableDatabase.update(TBL_JOBS, values, "$JOB_ID=?", arrayOf(jobId.toString()))
    }

    fun restoreJob(jobId: Int) {
        val values = ContentValues().apply { put(JOB_IS_DELETED, 0) }
        writableDatabase.update(TBL_JOBS, values, "$JOB_ID=?", arrayOf(jobId.toString()))
    }

    fun hardDeleteJob(jobId: Int) {
        writableDatabase.delete(TBL_JOBS, "$JOB_ID=?", arrayOf(jobId.toString()))
    }

    fun getDeletedJobs(): Cursor {
        return readableDatabase.rawQuery(
            """
            SELECT j.$JOB_ID, j.$JOB_NAME, j.$JOB_AMOUNT, e.$EMP_NAME
            FROM $TBL_JOBS j
            LEFT JOIN $TBL_EMPLOYERS e ON j.$JOB_EMPLOYER_ID = e.$EMP_ID
            WHERE j.$JOB_IS_DELETED = 1
            ORDER BY j.$JOB_ID DESC
            """.trimIndent(), null
        )
    }

    fun getTotalEarned(): Double {
        readableDatabase.rawQuery(
            """
            SELECT SUM($JOB_AMOUNT) 
            FROM $TBL_JOBS 
            WHERE $JOB_IS_DONE = 1 AND $JOB_IS_DELETED = 0
            """.trimIndent(), null
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0).takeIf { !it.isNaN() } ?: 0.0 else 0.0
        }
    }
}
