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
        private const val DATABASE_VERSION = 6   // ✅ versiyon artırıldı

        const val TBL_EMPLOYERS = "Employers"
        const val EMP_ID = "id"
        const val EMP_NAME = "name"
        const val EMP_DATE_ADDED = "dateAdded"
        const val EMP_IS_DELETED = "isDeleted"
        const val EMP_AUTO_APPROVE = "autoApprove"   // ✅ yeni sütun

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
        CREATE TABLE Employersğ (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            dateAdded TEXT,
            isDeleted INTEGER DEFAULT 0,
            autoApprove INTEGER DEFAULT 0   
             
        )
        """.trimIndent()
        )

        db.execSQL(
            """
        CREATE TABLE Jobs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            employer_id INTEGER,
            name TEXT,
            moneyhowmuch REAL,
            place TEXT,
            description TEXT,
            dateAdded TEXT,
            isDone INTEGER DEFAULT 0,
            isDeleted INTEGER DEFAULT 0
        )
        """.trimIndent()
        )
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TBL_EMPLOYERS ADD COLUMN $EMP_AUTO_APPROVE INTEGER DEFAULT 0;")
        }
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE Employers ADD COLUMN autoApprove INTEGER DEFAULT 0")
        }
    }

    // ------- Yardımcı Metotlar (Jobs) -------

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

    /** Silinmiş işleri getirir */
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

    /** ✅ Sadece isDone=1, isDeleted=0 ve işveren de silinmemiş olanların toplamını döner */
    fun getTotalEarned(): Double {
        readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM(j.$JOB_AMOUNT), 0)
            FROM $TBL_JOBS j
            JOIN $TBL_EMPLOYERS e ON j.$JOB_EMPLOYER_ID = e.$EMP_ID
            WHERE j.$JOB_IS_DONE = 1
              AND j.$JOB_IS_DELETED = 0
              AND e.$EMP_IS_DELETED = 0
            """.trimIndent(),
            null
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }
    fun softDeleteJobsByEmployer(employerId: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("isDeleted", 1)
        }
        db.update("Jobs", values, "employer_id = ?", arrayOf(employerId.toString()))
    }
    fun getEmployerCountsByDate(): Map<String, Int> {
        val db = readableDatabase
        val result = mutableMapOf<String, Int>()
        val cursor = db.rawQuery(
            "SELECT dateAdded, COUNT(*) FROM Employers WHERE isDeleted = 0 GROUP BY dateAdded",
            null
        )
        while (cursor.moveToNext()) {
            val date = cursor.getString(0)
            val count = cursor.getInt(1)
            result[date] = count
        }
        cursor.close()
        return result
    }

    fun getEmployersAddedOn(date: String): List<String> {
        val db = readableDatabase
        val employers = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT name FROM Employers WHERE isDeleted = 0 AND dateAdded = ?",
            arrayOf(date)
        )
        while (cursor.moveToNext()) {
            employers.add(cursor.getString(0))
        }
        cursor.close()
        return employers
    }


}

/** Employer listesinde sadece id+isim gerektiği için basit tip */
data class EmployerSimple(val id: Long, val name: String)
