package sv.ues.fia.eisi.bt.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.utils.PasswordHasher

class MainRepository(context: Context) {

    private val dbHelper = ConnectionHelper(context)
    private var db: SQLiteDatabase? = null

    data class Usuario(val id_usuario: Int, val username: String, val password: String, val rol: String)

    @Synchronized
    private fun getDb(): SQLiteDatabase {
        if (db == null || db?.isOpen != true) {
            db = dbHelper.writableDb
        }
        return db!!
    }

    fun login(username: String, password: String): Usuario? {
        val cursor = getDb().rawQuery("SELECT * FROM USUARIO WHERE USERNAME = ?", arrayOf(username))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        
        val idUsuario = cursor.getInt(0)
        var userName = ""
        var storedPassword = ""
        var rol = ""
        
        try {
            userName = cursor.getString(1)
            storedPassword = cursor.getString(2)
            rol = cursor.getString(3)
        } catch (e: Exception) {
            cursor.close()
            return null
        }
        
        cursor.close()
        
        if (PasswordHasher.verify(password, storedPassword)) {
            return Usuario(idUsuario, userName, storedPassword, rol)
        }
        return null
    }

    fun register(username: String, password: String, rol: String = "postulante"): Long {
        val hashedPassword = PasswordHasher.hash(password)
        
        return try {
            val checkCursor = getDb().rawQuery("SELECT COUNT(*) FROM USUARIO WHERE USERNAME = ?", arrayOf(username))
            checkCursor.moveToFirst()
            val existe = checkCursor.getInt(0)
            checkCursor.close()
            if (existe > 0) return -2L
            
            val maxUserIdRaw = getDb().rawQuery("SELECT MAX(ID_USUARIO) FROM USUARIO", null)
            maxUserIdRaw.moveToFirst()
            val newUserId = if (maxUserIdRaw.isNull(0)) 1 else maxUserIdRaw.getInt(0) + 1
            maxUserIdRaw.close()
            
            getDb().execSQL("INSERT INTO USUARIO (ID_USUARIO, USERNAME, PASSWORD, ROL) VALUES ($newUserId, '$username', '$hashedPassword', '$rol')")
            
            newUserId.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    fun deleteRecord(tableName: String, id: String): Boolean {
        val idCol = getIdColumn(tableName)
        getDb().execSQL("DELETE FROM $tableName WHERE $idCol = $id")
        return true
    }

    fun insertRecord(tableName: String, values: List<Any>): Long {
        val columns = getColumnsForTable(tableName).drop(1)
        val idCol = getIdColumn(tableName)
        
        android.util.Log.d("MainRepository", "Table: $tableName")
        android.util.Log.d("MainRepository", "Columns: $columns")
        android.util.Log.d("MainRepository", "Values: $values")
        
        if (values.size != columns.size) {
            android.util.Log.e("MainRepository", "ERROR: Values count (${values.size}) != Columns count (${columns.size})")
        }
        
        val valuesCopy = values.toMutableList()
        
        if (tableName == "USUARIO") {
            val pwdIndex = columns.indexOf("PASSWORD")
            if (pwdIndex >= 0 && pwdIndex < valuesCopy.size) {
                val plainPassword = valuesCopy[pwdIndex].toString()
                if (plainPassword.isNotBlank() && !plainPassword.startsWith("$2")) {
                    valuesCopy[pwdIndex] = PasswordHasher.hash(plainPassword)
                }
            }
        }
        
        val maxIdCursor = getDb().rawQuery("SELECT MAX($idCol) FROM $tableName", null)
        maxIdCursor.moveToFirst()
        val nextId = if (maxIdCursor.isNull(0)) 1 else maxIdCursor.getInt(0) + 1
        maxIdCursor.close()
        
        val cols = columns.joinToString(", ")
        val vals = valuesCopy.joinToString(", ") { "'$it'" }
        val sql = "INSERT INTO $tableName ($idCol, $cols) VALUES ($nextId, $vals)"
        
        android.util.Log.d("MainRepository", "SQL: $sql")
        
        getDb().beginTransaction()
        try {
            getDb().execSQL(sql)
            getDb().setTransactionSuccessful()
            getDb().endTransaction()
            return nextId.toLong()
        } catch (e: Exception) {
            getDb().endTransaction()
            android.util.Log.e("MainRepository", "Insert error: ${e.message}")
            throw e
        }
    }

    fun updateRecord(tableName: String, id: Any, values: List<Any>): Int {
        val columns = getColumnsForTable(tableName).drop(1)
        val idCol = getIdColumn(tableName)
        val setClause = columns.zip(values).map { "${it.first} = '${it.second}'" }.joinToString(", ")
        
        try {
            getDb().execSQL("UPDATE $tableName SET $setClause WHERE $idCol = $id")
            return 1
        } catch (e: Exception) {
            android.util.Log.e("MainRepository", "Update error: ${e.message}")
            throw e
        }
    }

    data class TableInfo(val name: String, val displayName: String, val count: Int)

    fun getAllTablesWithCount(): List<TableInfo> {
        val tables = listOf(
            "CATEGORIA_HABILIDAD", "GENERO", "TIPO_DOCUMENTO", "DEPARTAMENTO",
            "INSTITUCION", "GRADO_ACADEMICO", "RED_SOCIAL", "MUNICIPIO",
            "DISTRITO", "HABILIDAD", "EMPRESA", "OFERTA_ACADEMICA",
            "OFERTA_TRABAJO", "CERTIFICACION", "EXPERIENCIA_LABORAL",
            "FORMACION_ACADEMICA", "HABILIDAD_POSTULANTE", "POSTULACION",
            "DETALLE_REQUISITO", "RED_SOCIAL_POSTULANTE", "POSTULANTE", "USUARIO"
        )
        
        return try {
            tables.map { tableName ->
                val count = getDb().rawQuery("SELECT COUNT(*) FROM $tableName", null).apply {
                    moveToFirst()
                }.getInt(0)
                TableInfo(tableName, tableName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, count)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun searchTable(tableName: String, query: String): List<List<Any>> {
        return try {
            val cursor = getDb().rawQuery("SELECT * FROM $tableName", null)
            val results = mutableListOf<List<Any>>()
            
            while (cursor.moveToNext()) {
                val row = mutableListOf<Any>()
                for (i in 0 until cursor.columnCount) {
                    when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> row.add(cursor.getInt(i))
                        else -> row.add(cursor.getString(i) ?: "")
                    }
                }
                results.add(row)
            }
            cursor.close()
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    data class FkReference(val column: String, val refTable: String, val refDisplayColumn: String, val fkColumn: String = "")

    fun getFkReferences(tableName: String): Map<String, FkReference> {
        return when (tableName) {
            "USUARIO" -> mapOf("ROL" to FkReference("ROL", "ROL", "NOMBRE"))
            "POSTULANTE" -> mapOf(
                "ID_USUARIO" to FkReference("ID_USUARIO", "USUARIO", "USERNAME"),
                "ID_GENERO" to FkReference("ID_GENERO", "GENERO", "NOMBRE_GENERO"),
                "ID_TIPO_DOCUMENTO" to FkReference("ID_TIPO_DOCUMENTO", "TIPO_DOCUMENTO", "NOMBRE_TIPO"),
                "ID_DISTRITO" to FkReference("ID_DISTRITO", "DISTRITO", "NOMBRE_DISTRITO")
            )
            "MUNICIPIO" -> mapOf("ID_DEPARTAMENTO" to FkReference("ID_DEPARTAMENTO", "DEPARTAMENTO", "NOMBRE_DEPARTAMENTO", "ID_DEPARTAMENTO"))
            "DISTRITO" -> mapOf("ID_MUNICIPIO" to FkReference("ID_MUNICIPIO", "MUNICIPIO", "NOMBRE_MUNICIPIO", "ID_MUNICIPIO"))
            "HABILIDAD" -> mapOf("ID_CATEGORIA_HABILIDAD" to FkReference("ID_CATEGORIA_HABILIDAD", "CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA"))
            "EMPRESA" -> mapOf("ID_DISTRITO" to FkReference("ID_DISTRITO", "DISTRITO", "NOMBRE_DISTRITO"))
            "OFERTA_ACADEMICA" -> mapOf(
                "ID_INSTITUCION" to FkReference("ID_INSTITUCION", "INSTITUCION", "NOMBRE_INSTITUCION"),
                "ID_GRADO_ACADEMICO" to FkReference("ID_GRADO_ACADEMICO", "GRADO_ACADEMICO", "NOMBRE_GRADO")
            )
            "OFERTA_TRABAJO" -> mapOf(
                "ID_EMPRESA" to FkReference("ID_EMPRESA", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_GRADO_ACADEMICO" to FkReference("ID_GRADO_ACADEMICO", "GRADO_ACADEMICO", "NOMBRE_GRADO")
            )
            "CERTIFICACION" -> mapOf(
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE"),
                "ID_INSTITUCION" to FkReference("ID_INSTITUCION", "INSTITUCION", "NOMBRE_INSTITUCION")
            )
            "EXPERIENCIA_LABORAL" -> mapOf(
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE"),
                "ID_EMPRESA" to FkReference("ID_EMPRESA", "EMPRESA", "NOMBRE_EMPRESA")
            )
            "FORMACION_ACADEMICA" -> mapOf(
                "ID_OFERTA_ACADEMICA" to FkReference("ID_OFERTA_ACADEMICA", "OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE")
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "ID_HABILIDAD" to FkReference("ID_HABILIDAD", "HABILIDAD", "NOMBRE_HABILIDAD"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE")
            )
            "POSTULACION" -> mapOf(
                "ID_EMPRESA" to FkReference("ID_EMPRESA", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_OFERTA" to FkReference("ID_OFERTA", "OFERTA_TRABAJO", "TITULO_PUESTO"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE")
            )
            "DETALLE_REQUISITO" -> mapOf(
                "ID_EMPRESA" to FkReference("ID_EMPRESA", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_OFERTA" to FkReference("ID_OFERTA", "OFERTA_TRABAJO", "TITULO_PUESTO")
            )
            "RED_SOCIAL_POSTULANTE" -> mapOf(
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE"),
                "ID_RED_SOCIAL" to FkReference("ID_RED_SOCIAL", "RED_SOCIAL", "NOMBRE_RED")
            )
            else -> emptyMap()
        }
    }

    fun getDropdownOptions(tableName: String, displayColumn: String): List<Pair<String, String>> {
        if (tableName == "ROL") {
            return listOf(
                Pair("postulante", "postulante"),
                Pair("empresa", "empresa"),
                Pair("admin", "admin")
            )
        }
        
        return try {
            val cursor = getDb().rawQuery("SELECT * FROM $tableName", null)
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0).toString()
                val displayName = try {
                    val colIndex = cursor.getColumnIndex(displayColumn)
                    if (colIndex >= 0) cursor.getString(colIndex) ?: id else id
                } catch (e: Exception) { id }
                options.add(Pair(id, displayName))
            }
            cursor.close()
            options
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getFilteredOptions(childTable: String, childFkColumn: String, parentId: String): List<Pair<String, String>> {
        return try {
            val cursor = getDb().rawQuery("SELECT * FROM $childTable WHERE $childFkColumn = ?", arrayOf(parentId))
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0).toString()
                val name = cursor.getString(1) ?: id
                options.add(Pair(id, name))
            }
            cursor.close()
            options
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getIdColumn(tableName: String): String {
        return when (tableName) {
            "USUARIO" -> "ID_USUARIO"
            "POSTULANTE" -> "ID_POSTULANTE"
            "GENERO" -> "ID_GENERO"
            "TIPO_DOCUMENTO" -> "ID_TIPO_DOCUMENTO"
            "DEPARTAMENTO" -> "ID_DEPARTAMENTO"
            "MUNICIPIO" -> "ID_MUNICIPIO"
            "DISTRITO" -> "ID_DISTRITO"
            "HABILIDAD" -> "ID_HABILIDAD"
            "CATEGORIA_HABILIDAD" -> "ID_CATEGORIA_HABILIDAD"
            "EMPRESA" -> "ID_EMPRESA"
            "INSTITUCION" -> "ID_INSTITUCION"
            "GRADO_ACADEMICO" -> "ID_GRADO_ACADEMICO"
            "RED_SOCIAL" -> "ID_RED_SOCIAL"
            "OFERTA_ACADEMICA" -> "ID_OFERTA_ACADEMICA"
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "CERTIFICACION" -> "ID_CERTIFICACION"
            "EXPERIENCIA_LABORAL" -> "ID_EXPERIENCIA"
            "FORMACION_ACADEMICA" -> "ID_FORMACION"
            "HABILIDAD_POSTULANTE" -> "ID_HABILIDAD_POSTULANTE"
            "POSTULACION" -> "ID_POSTULACION"
            "DETALLE_REQUISITO" -> "ID_DETALLE"
            "RED_SOCIAL_POSTULANTE" -> "ID_RED_POSTUALNTE"
            else -> "ID"
        }
    }

    private fun getColumnsForTable(tableName: String): List<String> {
        return when (tableName) {
            "USUARIO" -> listOf("ID_USUARIO", "USERNAME", "PASSWORD", "ROL")
            "POSTULANTE" -> listOf("ID_POSTULANTE", "ID_USUARIO", "ID_GENERO", "ID_DISTRITO", "ID_TIPO_DOCUMENTO", "NUM_DOCUMENTO", "NOMBRE", "APELLIDO", "EMAIL", "FECHA_NACIMIENTO")
            "GENERO" -> listOf("ID_GENERO", "NOMBRE_GENERO")
            "TIPO_DOCUMENTO" -> listOf("ID_TIPO_DOCUMENTO", "NOMBRE_TIPO")
            "DEPARTAMENTO" -> listOf("ID_DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
            "MUNICIPIO" -> listOf("ID_MUNICIPIO", "ID_DEPARTAMENTO", "NOMBRE_MUNICIPIO")
            "DISTRITO" -> listOf("ID_DISTRITO", "ID_MUNICIPIO", "NOMBRE_DISTRITO")
            "HABILIDAD" -> listOf("ID_HABILIDAD", "ID_CATEGORIA_HABILIDAD", "NOMBRE_HABILIDAD")
            "CATEGORIA_HABILIDAD" -> listOf("ID_CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA")
            "EMPRESA" -> listOf("ID_EMPRESA", "ID_DISTRITO", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO", "NIT")
            "INSTITUCION" -> listOf("ID_INSTITUCION", "NOMBRE_INSTITUCION")
            "GRADO_ACADEMICO" -> listOf("ID_GRADO_ACADEMICO", "NOMBRE_GRADO")
            "RED_SOCIAL" -> listOf("ID_RED_SOCIAL", "NOMBRE_RED", "LOGO_ICONO")
            "OFERTA_ACADEMICA" -> listOf("ID_OFERTA_ACADEMICA", "ID_INSTITUCION", "ID_GRADO_ACADEMICO")
            "OFERTA_TRABAJO" -> listOf("ID_OFERTA", "ID_EMPRESA", "ID_GRADO_ACADEMICO", "TITULO_PUESTO", "FECHA_PUBLICACION", "FECHA_CADUCIDAD", "EXPERIENCIA_ANIOS", "EDAD_MINIMA", "EDAD_MAXIMA", "DESCRIPCION_OFERTA_TRABAJO")
            "CERTIFICACION" -> listOf("ID_CERTIFICACION", "ID_POSTULANTE", "ID_INSTITUCION", "NOMBRE_CERTIFICACION", "CODIGO_CERTIFICACION", "FECHA_CERTIFICACION")
            "EXPERIENCIA_LABORAL" -> listOf("ID_EXPERIENCIA", "ID_POSTULANTE", "ID_EMPRESA", "PUESTO_TRABAJO", "FECHA_INICIO", "FECHA_FIN", "DES_EXP_LABORAL", "CONTACTO_REFERENCIA")
            "FORMACION_ACADEMICA" -> listOf("ID_FORMACION", "ID_OFERTA_ACADEMICA", "ID_POSTULANTE", "TITULO_OBTENIDO", "FECHA_OBTENCION")
            "HABILIDAD_POSTULANTE" -> listOf("ID_HABILIDAD_POSTULANTE", "ID_HABILIDAD", "ID_POSTULANTE", "NIVEL_DESTREZA")
            "POSTULACION" -> listOf("ID_POSTULACION", "ID_EMPRESA", "ID_OFERTA", "ID_POSTULANTE", "FECHA_APLICACION", "ESTADO_PROCESO")
            "DETALLE_REQUISITO" -> listOf("ID_DETALLE", "ID_EMPRESA", "ID_OFERTA", "DESCRIPCION_REQUISITO")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_RED_POSTUALNTE", "ID_POSTULANTE", "ID_RED_SOCIAL", "URL_PERFIL")
            else -> listOf("NOMBRE")
        }
    }
}