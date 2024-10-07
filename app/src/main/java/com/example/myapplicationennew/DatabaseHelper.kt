import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MyAppDatabase.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // İşverenler tablosunu oluşturun
        db.execSQL(
            "CREATE TABLE Employers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "dateAdded TEXT);"
        )

        // İşler tablosunu oluşturun
        db.execSQL(
            "CREATE TABLE Jobs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "employer_id INTEGER," +
                    "name TEXT," +
                    "moneyhowmuch REAL," +  // Küsuratlı para değerleri için REAL tipi
                    "description TEXT," +
                    "dateAdded TEXT," +     // İşin eklenme tarihi
                    "FOREIGN KEY(employer_id) REFERENCES Employers(id) ON DELETE CASCADE);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Mevcut tabloları sil
        db.execSQL("DROP TABLE IF EXISTS Jobs")
        db.execSQL("DROP TABLE IF EXISTS Employers")

        // Yeni tabloyu tekrar oluştur
        onCreate(db)
    }
}
