package sv.ues.fia.eisi.bt.data.local

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ConnectionHelper(private val context: Context) {

    companion object {
        private const val DATABASE_NAME = "si.db"
        private const val DB_VERSION = 2
        private const val PREF_NAME = "db_version"
        private const val PREF_VERSION = "version"
    }

    private val dbPath: String
        get() = context.getDatabasePath(DATABASE_NAME).absolutePath

    val writableDb: SQLiteDatabase
        get() {
            val dbFile = File(dbPath)
            if (!dbFile.exists()) {
                copyDatabase()
            }
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
            applyMigrations(db)
            return db
        }

    val readableDb: SQLiteDatabase
        get() {
            val dbFile = File(dbPath)
            if (!dbFile.exists()) {
                copyDatabase()
            }
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            return db
        }

    fun getWritableDatabase(): SQLiteDatabase = writableDb
    fun getReadableDatabase(): SQLiteDatabase = readableDb

    private fun copyDatabase(): Boolean {
        val dbFile = File(dbPath)
        val dbDir = dbFile.parentFile
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs()
        }

        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(DATABASE_NAME)

            val outputStream = FileOutputStream(dbFile)
            val buffer = ByteArray(1024)

            inputStream.use { input ->
                outputStream.use { output ->
                    var length = input.read(buffer)
                    while (length > 0) {
                        output.write(buffer, 0, length)
                        length = input.read(buffer)
                    }
                    output.flush()
                }
            }
            val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(PREF_VERSION, DB_VERSION).apply()
            return true

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun applyMigrations(db: SQLiteDatabase) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentVersion = prefs.getInt(PREF_VERSION, 0)

        if (currentVersion < DB_VERSION) {
            when (currentVersion) {
                0 -> migrateV1ToV2(db)
            }
            prefs.edit().putInt(PREF_VERSION, DB_VERSION).apply()
        }
    }

    private fun migrateV1ToV2(db: SQLiteDatabase) {
        db.execSQL("DROP TRIGGER IF EXISTS TR_NIVEL_EDUCATIVO")
        db.execSQL("""
            CREATE TRIGGER TR_NIVEL_EDUCATIVO
            BEFORE INSERT ON FORMACION_ACADEMICA
            FOR EACH ROW
            BEGIN
                SELECT RAISE(ABORT, 'El sistema no permite registros con nivel de bachillerato o inferior.')
                WHERE (SELECT UPPER(g.NOMBRE_GRADO) FROM GRADO_ACADEMICO g 
                       INNER JOIN OFERTA_ACADEMICA oa ON g.ID_GRADO_ACADEMICO = oa.ID_GRADO_ACADEMICO
                       WHERE oa.ID_OFERTA_ACADEMICA = NEW.ID_OFERTA_ACADEMICA)
                IN ('BACHILLERATO', 'EDUCACION BASICA', 'PRIMARIA');
            END
        """)
    }

    fun getCount(tableName: String): Int {
        var count = 0
        try {
            val db = readableDb
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun executeQuery(query: String): List<List<Any>> {
        val results = mutableListOf<List<Any>>()
        try {
            val db = writableDb
            val cursor = db.rawQuery(query, null)
            while (cursor.moveToNext()) {
                val row = mutableListOf<Any>()
                for (i in 0 until cursor.columnCount) {
                    when {
                        cursor.isNull(i) -> row.add("")
                        else -> row.add(cursor.getString(i) ?: "")
                    }
                }
                results.add(row)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

fun executeInsert(query: String): Long {
        var id = -1L
        try {
            val db = writableDb
            db.execSQL(query)
            val cursor = db.rawQuery("SELECT last_insert_rowid()", null)
            if (cursor.moveToFirst()) {
                id = cursor.getLong(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    fun executeUpdate(query: String): Int {
        var rows = 0
        try {
            val db = writableDb
            db.execSQL(query)
            rows = 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    fun executeDelete(query: String): Int {
        var rows = 0
        try {
            val db = writableDb
            db.execSQL(query)
            rows = 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    fun search(tableName: String, column: String, value: String): List<List<Any>> {
        val query = "SELECT * FROM $tableName WHERE $column LIKE '%$value%'"
        return executeQuery(query)
    }

    fun getAll(tableName: String): List<List<Any>> {
        val query = "SELECT * FROM $tableName"
        return executeQuery(query)
    }

    fun getById(tableName: String, idName: String, idValue: Int): List<List<Any>> {
        val query = "SELECT * FROM $tableName WHERE $idName = $idValue"
        return executeQuery(query)
    }

    fun deleteById(tableName: String, idName: String, idValue: Int): Int {
        val query = "DELETE FROM $tableName WHERE $idName = $idValue"
        return executeDelete(query)
    }
}