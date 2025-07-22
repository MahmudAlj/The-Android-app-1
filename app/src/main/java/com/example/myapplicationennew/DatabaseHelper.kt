import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 4
        private const val DATABASE_NAME = "MyAppDatabase.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE Employers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "dateAdded TEXT," +
                    "isDeleted INTEGER DEFAULT 0);"
        )

        db.execSQL(
            "CREATE TABLE Jobs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employer_id INTEGER," +
                    "name TEXT," +
                    "moneyhowmuch REAL," +
                    "place TEXT," +  // ✅ Mekan sütunu eklendi
                    "description TEXT," +
                    "dateAdded TEXT," +
                    "FOREIGN KEY(employer_id) REFERENCES Employers(id) ON DELETE CASCADE);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE Employers ADD COLUMN isDeleted INTEGER DEFAULT 0;")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE Jobs ADD COLUMN place TEXT DEFAULT '';")
        }
        if (oldVersion >= 4) {
            db.execSQL("DROP TABLE IF EXISTS Jobs")
            db.execSQL("DROP TABLE IF EXISTS Employers")
            onCreate(db)
        }
    }
}
