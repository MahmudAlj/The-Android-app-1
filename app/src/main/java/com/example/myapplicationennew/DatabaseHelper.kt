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
        private const val DATABASE_VERSION = 7

        const val TBL_EMPLOYERS = "Employers"
        const val EMP_ID = "id"
        const val EMP_NAME = "name"
        const val EMP_DATE_ADDED = "dateAdded"
        const val EMP_IS_DELETED = "isDeleted"
        const val EMP_AUTO_APPROVE = "autoApprove"

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

        const val TBL_EXPENSES = "Expenses"
        const val EXP_ID = "id"
        const val EXP_AMOUNT = "amount"
        const val EXP_DESC = "description"
        const val EXP_TYPE = "type"
        const val EXP_DATE = "dateAdded"

    }
    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
        CREATE TABLE IF NOT EXISTS $TBL_EXPENSES (
            $EXP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $EXP_AMOUNT REAL NOT NULL,
            $EXP_DESC TEXT,
            $EXP_TYPE TEXT,
            $EXP_DATE TEXT
        )
    """.trimIndent())
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_EMPLOYERS (
                $EMP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $EMP_NAME TEXT NOT NULL,
                $EMP_DATE_ADDED TEXT,
                $EMP_IS_DELETED INTEGER DEFAULT 0,
                $EMP_AUTO_APPROVE INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TBL_JOBS (
                $JOB_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $JOB_EMPLOYER_ID INTEGER,
                $JOB_NAME TEXT NOT NULL,
                $JOB_AMOUNT REAL,
                $JOB_PLACE TEXT,
                $JOB_DESC TEXT,
                $JOB_DATE_ADDED TEXT,
                $JOB_IS_DONE INTEGER DEFAULT 0,
                $JOB_IS_DELETED INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT UNIQUE," +
                    "password TEXT)"
        )
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TBL_EMPLOYERS ADD COLUMN $EMP_IS_DELETED INTEGER DEFAULT 0;")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_PLACE TEXT DEFAULT '';")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_IS_DONE INTEGER DEFAULT 0;")
            db.execSQL("ALTER TABLE $TBL_JOBS ADD COLUMN $JOB_IS_DELETED INTEGER DEFAULT 0;")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TBL_EMPLOYERS ADD COLUMN $EMP_AUTO_APPROVE INTEGER DEFAULT 0;")
        }
        // Yeni migration’lar gerektiğinde buraya eklenir
    }
    // --- JOB yardımcı fonksiyonları ---
    fun setJobDone(jobId: Int, done: Boolean) {
        val values = ContentValues().apply {
            put(JOB_IS_DONE, if (done) 1 else 0)
        }
        writableDatabase.update(
            TBL_JOBS,
            values,
            "$JOB_ID = ?",
            arrayOf(jobId.toString())
        )
    }
    fun softDeleteJob(jobId: Int) {
        val values = ContentValues().apply {
            put(JOB_IS_DELETED, 1)
        }
        writableDatabase.update(
            TBL_JOBS,
            values,
            "$JOB_ID = ?",
            arrayOf(jobId.toString())
        )
    }
    fun softDeleteJobsByEmployer(employerId: Long) {
        val values = ContentValues().apply {
            put(JOB_IS_DELETED, 1)
        }
        writableDatabase.update(
            TBL_JOBS,
            values,
            "$JOB_EMPLOYER_ID = ?",
            arrayOf(employerId.toString())
        )
    }
    fun getDeletedJobs(): Cursor {
        return readableDatabase.rawQuery(
            """
            SELECT j.$JOB_ID,
                   j.$JOB_NAME,
                   j.$JOB_AMOUNT,
                   e.$EMP_NAME
            FROM $TBL_JOBS j
            LEFT JOIN $TBL_EMPLOYERS e
            ON j.$JOB_EMPLOYER_ID = e.$EMP_ID
            WHERE j.$JOB_IS_DELETED = 1
            ORDER BY j.$JOB_ID DESC
            """.trimIndent(),
            null
        )
    }
    fun restoreJob(jobId: Int) {
        val values = ContentValues().apply {
            put(JOB_IS_DELETED, 0)
        }
        writableDatabase.update(
            TBL_JOBS,
            values,
            "$JOB_ID = ?",
            arrayOf(jobId.toString())
        )
    }
    fun hardDeleteJob(jobId: Int) {
        writableDatabase.delete(
            TBL_JOBS,
            "$JOB_ID = ?",
            arrayOf(jobId.toString())
        )
    }
    fun getTotalNet(): Double {
        // 1) Tamamlanmış işler (isDone = 1)
        val income = readableDatabase.rawQuery(
            """
        SELECT COALESCE(SUM($JOB_AMOUNT), 0)
        FROM $TBL_JOBS
        WHERE $JOB_IS_DELETED = 0
          AND $JOB_IS_DONE = 1
        """.trimIndent(),
            null
        ).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }

        // 2) Giderler tablosu
        val expenses = readableDatabase.rawQuery(
            """
        SELECT COALESCE(SUM($EXP_AMOUNT), 0)
        FROM $TBL_EXPENSES
        """.trimIndent(),
            null
        ).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }

        return income - expenses
    }
    // --- Takvim ekranı için ---
    fun getEmployerCountsByDate(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        readableDatabase.rawQuery(
            """
            SELECT $EMP_DATE_ADDED, COUNT(*)
            FROM $TBL_EMPLOYERS
            WHERE $EMP_IS_DELETED = 0
            GROUP BY $EMP_DATE_ADDED
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = cursor.getString(0)
                val count = cursor.getInt(1)
                if (date != null) {
                    result[date] = count
                }
            }
        }
        return result
    }
    fun getEmployersAddedOn(date: String): List<String> {
        val employers = mutableListOf<String>()
        readableDatabase.rawQuery(
            """
            SELECT $EMP_NAME
            FROM $TBL_EMPLOYERS
            WHERE $EMP_IS_DELETED = 0
              AND $EMP_DATE_ADDED = ?
            ORDER BY $EMP_NAME
            """.trimIndent(),
            arrayOf(date)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                employers.add(cursor.getString(0))
            }
        }
        return employers
    }
    fun addExpense(amount: Double?, description: String, type: String?, date: String) {
        val values = ContentValues().apply {
            put(EXP_AMOUNT, amount)
            put(EXP_DESC, description)
            put(EXP_TYPE, type)
            put(EXP_DATE, date)
        }
        writableDatabase.insert(TBL_EXPENSES, null, values)
    }
    fun addExpense(amount: String, description: Double, type: String) {}
    fun getTotalExpenses(): Double {
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM($EXP_AMOUNT), 0) FROM $TBL_EXPENSES",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        }
    }
    fun registerUser(email: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put("email", email)
        values.put("password", password)

        return try {
            db.insert("users", null, values)
            true
        } catch (e: Exception) {
            false
        }
    }
    fun loginUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE email=? AND password=?",
            arrayOf(email, password)
        )

        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
    fun getJobsAddedOn(date: String): List<Job> {
        val jobs = mutableListOf<Job>()

        readableDatabase.rawQuery(
            """
        SELECT id, employer_id, name, moneyhowmuch, place, description, dateAdded, isDone
        FROM Jobs
        WHERE dateAdded = ?
          AND isDeleted = 0
        ORDER BY id DESC
        """,
            arrayOf(date)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val job = Job(
                    id = cursor.getLong(0),
                    employerId = cursor.getLong(1),
                    name = cursor.getString(2),
                    moneyhowmuch = cursor.getDouble(3),
                    place = cursor.getString(4),
                    description = cursor.getString(5),
                    dateAdded = cursor.getString(6),
                    isDone = cursor.getInt(7) == 1
                )
                jobs.add(job)
            }
        }
        return jobs
    }
    fun getJobsAddedOnForEmployer(date: String, employerId: Long): List<Job> {
        val jobs = mutableListOf<Job>()
        readableDatabase.rawQuery(
            """
        SELECT id, name, moneyhowmuch, place, description, dateAdded, isDone
        FROM Jobs
        WHERE dateAdded = ?
          AND employer_id = ?
          AND isDeleted = 0
        ORDER BY id DESC
        """.trimIndent(),
            arrayOf(date, employerId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                jobs.add(
                    Job(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        moneyhowmuch = cursor.getDouble(2),
                        place = cursor.getString(3),
                        description = cursor.getString(4),
                        dateAdded = cursor.getString(5),
                        isDone = cursor.getInt(6) == 1,
                        isDeleted = false
                    )
                )
            }
        }
        return jobs
    }

}
data class EmployerSimple(val id: Long, val name: String)
