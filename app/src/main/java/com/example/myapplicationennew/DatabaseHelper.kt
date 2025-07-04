import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 3  // 🚨 Versiyonu 2 yaptık çünkü isDeleted sütunu eklendi
        private const val DATABASE_NAME = "MyAppDatabase.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // ✅ İşverenler tablosunu oluştur - isDeleted alanı dahil
        db.execSQL(
            "CREATE TABLE Employers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "dateAdded TEXT," +
                    "isDeleted INTEGER DEFAULT 0);" // ← eklendi!
        )

        // ✅ İşler tablosunu oluştur
        db.execSQL(
            "CREATE TABLE Jobs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employer_id INTEGER," +
                    "name TEXT," +
                    "moneyhowmuch REAL," +
                    "description TEXT," +
                    "dateAdded TEXT," +
                    "FOREIGN KEY(employer_id) REFERENCES Employers(id) ON DELETE CASCADE);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE Employers ADD COLUMN isDeleted INTEGER DEFAULT 0;")
        } else {
            db.execSQL("DROP TABLE IF EXISTS Jobs")
            db.execSQL("DROP TABLE IF EXISTS Employers")
            onCreate(db)
        }
    }
}
