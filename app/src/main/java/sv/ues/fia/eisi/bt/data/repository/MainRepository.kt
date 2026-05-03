package sv.ues.fia.eisi.bt.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.utils.PasswordHasher
import sv.ues.fia.eisi.bt.utils.TriggerErrorTranslator

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
        return try {
            val idCol = getIdColumn(tableName)
            getDb().execSQL("DELETE FROM $tableName WHERE $idCol = $id")
            true
        } catch (e: android.database.sqlite.SQLiteException) {
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    fun deleteRecordByRow(tableName: String, rowData: List<String>): Boolean {
        val columns = getColumnsForTable(tableName)
        val pkColumns = getPrimaryKeyColumns(tableName)
        return try {
            val whereClause = pkColumns.mapIndexed { i, col ->
                val idx = columns.indexOf(col)
                val value = if (idx >= 0 && idx < rowData.size) rowData[idx] else ""
                "$col = '$value'"
            }.joinToString(" AND ")
            getDb().execSQL("DELETE FROM $tableName WHERE $whereClause")
            true
        } catch (e: android.database.sqlite.SQLiteException) {
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    data class DependencyInfo(val tableName: String, val displayName: String, val count: Int)

    fun getDeleteDependencies(tableName: String, id: String): List<DependencyInfo> {
        val dependencies = getDependencyMap(tableName)
        val result = mutableListOf<DependencyInfo>()
        for ((childTable, fkColumn) in dependencies) {
            val idCol = getIdColumn(tableName)
            val cursor = getDb().rawQuery(
                "SELECT COUNT(*) FROM $childTable WHERE $fkColumn = ?",
                arrayOf(id)
            )
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                if (count > 0) {
                    result.add(DependencyInfo(
                        childTable,
                        childTable.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        count
                    ))
                }
            }
            cursor.close()
        }
        return result
    }

    private fun getDependencyMap(tableName: String): List<Pair<String, String>> {
        return when (tableName) {
            "DEPARTAMENTO" -> listOf("MUNICIPIO" to "ID_DEPARTAMENTO")
            "MUNICIPIO" -> listOf("DISTRITO" to "ID_MUNICIPIO")
            "DISTRITO" -> listOf("POSTULANTE" to "ID_DISTRITO", "EMPRESA" to "ID_DISTRITO")
            "GENERO" -> listOf("POSTULANTE" to "ID_GENERO")
            "TIPO_DOCUMENTO" -> listOf("POSTULANTE" to "ID_TIPO_DOCUMENTO")
            "INSTITUCION" -> listOf("OFERTA_ACADEMICA" to "ID_INSTITUCION", "CERTIFICACION" to "ID_INSTITUCION")
            "GRADO_ACADEMICO" -> listOf("OFERTA_TRABAJO" to "ID_GRADO_ACADEMICO", "OFERTA_ACADEMICA" to "ID_GRADO_ACADEMICO")
            "CATEGORIA_HABILIDAD" -> listOf("HABILIDAD" to "ID_CATEGORIA_HABILIDAD")
            "HABILIDAD" -> listOf("HABILIDAD_POSTULANTE" to "ID_HABILIDAD")
            "RED_SOCIAL" -> listOf("RED_SOCIAL_POSTULANTE" to "ID_RED_SOCIAL")
            "OFERTA_ACADEMICA" -> listOf("FORMACION_ACADEMICA" to "ID_OFERTA_ACADEMICA")
            "EMPRESA" -> listOf("OFERTA_TRABAJO" to "ID_EMPRESA", "EXPERIENCIA_LABORAL" to "ID_EMPRESA")
            "POSTULANTE" -> listOf(
                "POSTULACION" to "ID_POSTULANTE",
                "EXPERIENCIA_LABORAL" to "ID_POSTULANTE",
                "FORMACION_ACADEMICA" to "ID_POSTULANTE",
                "CERTIFICACION" to "ID_POSTULANTE",
                "HABILIDAD_POSTULANTE" to "ID_POSTULANTE",
                "RED_SOCIAL_POSTULANTE" to "ID_POSTULANTE"
            )
            else -> emptyList()
        }
    }

    private fun getPrimaryKeyColumns(tableName: String): List<String> {
        return when (tableName) {
            "OFERTA_TRABAJO" -> listOf("ID_EMPRESA", "ID_OFERTA")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE", "ID_EXPERIENCIA")
            "CERTIFICACION" -> listOf("ID_POSTULANTE", "ID_CERTIFICACION")
            "HABILIDAD_POSTULANTE" -> listOf("ID_HABILIDAD", "ID_POSTULANTE", "ID_HABILIDAD_POSTULANTE")
            "POSTULACION" -> listOf("ID_EMPRESA", "ID_OFERTA", "ID_POSTULANTE", "ID_POSTULACION")
            "DETALLE_REQUISITO" -> listOf("ID_DETALLE")
            else -> listOf(getIdColumn(tableName))
        }
    }

    private fun getAutoGenColumn(tableName: String): String {
        return when (tableName) {
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "EXPERIENCIA_LABORAL" -> "ID_EXPERIENCIA"
            "CERTIFICACION" -> "ID_CERTIFICACION"
            "HABILIDAD_POSTULANTE" -> "ID_HABILIDAD_POSTULANTE"
            "POSTULACION" -> "ID_POSTULACION"
            else -> getIdColumn(tableName)
        }
    }

    fun insertRecord(tableName: String, values: List<Any>): Long {
        val idCol = getAutoGenColumn(tableName)
        var columns = getColumnsForTable(tableName).filter { it != idCol }
        val finalValues = values.toMutableList()

        // Filtrar columnas virtuales que no existen en la tabla real
        if (tableName == "HABILIDAD_POSTULANTE") {
            val catIndex = getColumnsForTable(tableName).filter { it != idCol }.indexOf("ID_CATEGORIA_HABILIDAD")
            if (catIndex >= 0) {
                columns = columns.filterIndexed { index, _ -> index != catIndex }
                finalValues.removeAt(catIndex)
            }
        }

        android.util.Log.d("MainRepository", "Table: $tableName")
        android.util.Log.d("MainRepository", "AutoGenCol: $idCol")
        android.util.Log.d("MainRepository", "Columns: $columns")
        android.util.Log.d("MainRepository", "Values: $finalValues")

        if (finalValues.size != columns.size) {
            android.util.Log.e("MainRepository", "ERROR: Values count (${finalValues.size}) != Columns count (${columns.size})")
        }

        if (tableName == "USUARIO") {
            val pwdIndex = columns.indexOf("PASSWORD")
            if (pwdIndex >= 0 && pwdIndex < finalValues.size) {
                val plainPassword = finalValues[pwdIndex].toString()
                if (plainPassword.isNotBlank() && !plainPassword.startsWith("$2")) {
                    finalValues[pwdIndex] = PasswordHasher.hash(plainPassword)
                }
            }
        }

        val maxIdCursor = getDb().rawQuery("SELECT MAX($idCol) FROM $tableName", null)
        maxIdCursor.moveToFirst()
        val nextId = if (maxIdCursor.isNull(0)) 1 else maxIdCursor.getInt(0) + 1
        maxIdCursor.close()

        val cols = columns.joinToString(", ")
        val vals = finalValues.joinToString(", ") { "'$it'" }
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
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    fun updateRecord(tableName: String, id: Any, values: List<Any>): Int {
        val idCol = getAutoGenColumn(tableName)
        var columns = getColumnsForTable(tableName).filter { it != idCol }
        val finalValues = values.toMutableList()

        // Filtrar columnas virtuales que no existen en la tabla real
        if (tableName == "HABILIDAD_POSTULANTE") {
            val catIndex = getColumnsForTable(tableName).filter { it != idCol }.indexOf("ID_CATEGORIA_HABILIDAD")
            if (catIndex >= 0) {
                columns = columns.filterIndexed { index, _ -> index != catIndex }
                finalValues.removeAt(catIndex)
            }
        }

        val setClause = columns.zip(finalValues).map { "${it.first} = '${it.second}'" }.joinToString(", ")

        try {
            getDb().execSQL("UPDATE $tableName SET $setClause WHERE $idCol = $id")
            return 1
        } catch (e: Exception) {
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    data class TableInfo(val name: String, val displayName: String, val count: Int)

    fun getAllTablesWithCount(): List<TableInfo> {
        val tables = listOf(
            "CATEGORIA_HABILIDAD", "GENERO", "TIPO_DOCUMENTO", "DEPARTAMENTO",
            "MUNICIPIO", "DISTRITO", "INSTITUCION", "GRADO_ACADEMICO",
            "RED_SOCIAL", "POSTULANTE", "USUARIO", "EMPRESA",
            "OFERTA_TRABAJO", "DETALLE_REQUISITO", "OFERTA_ACADEMICA",
            "FORMACION_ACADEMICA", "EXPERIENCIA_LABORAL", "CERTIFICACION",
            "HABILIDAD", "HABILIDAD_POSTULANTE", "POSTULACION",
            "RED_SOCIAL_POSTULANTE"
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
        val sql = when (tableName) {
            "USUARIO" -> {
                "SELECT ID_USUARIO, USERNAME, ROL FROM USUARIO WHERE USERNAME LIKE '%$query%'"
            }
            "HABILIDAD" -> {
                """
                SELECT h.ID_HABILIDAD, IFNULL(c.NOMBRE_CATEGORIA, 'Sin Categoría'), h.NOMBRE_HABILIDAD 
                FROM HABILIDAD h
                LEFT JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
                WHERE h.NOMBRE_HABILIDAD LIKE '%$query%'
                ORDER BY h.NOMBRE_HABILIDAD
                """.trimIndent()
            }
            "MUNICIPIO" -> {
                """
                SELECT m.ID_MUNICIPIO, d.NOMBRE_DEPARTAMENTO, m.NOMBRE_MUNICIPIO 
                FROM MUNICIPIO m
                LEFT JOIN DEPARTAMENTO d ON m.ID_DEPARTAMENTO = d.ID_DEPARTAMENTO
                WHERE m.NOMBRE_MUNICIPIO LIKE '%$query%'
                """.trimIndent()
            }
            "DISTRITO" -> {
                """
                SELECT d.ID_DISTRITO, m.NOMBRE_MUNICIPIO, d.NOMBRE_DISTRITO 
                FROM DISTRITO d
                LEFT JOIN MUNICIPIO m ON d.ID_MUNICIPIO = m.ID_MUNICIPIO
                WHERE d.NOMBRE_DISTRITO LIKE '%$query%'
                """.trimIndent()
            }
            "EXPERIENCIA_LABORAL" -> {
                """
                SELECT e.ID_POSTULANTE, e.ID_EXPERIENCIA, e.ID_EMPRESA, 
                       e.PUESTO_TRABAJO, e.FECHA_INICIO, e.FECHA_FIN, 
                       e.DESCP_EXPERIENCIA_LABORAL, e.CONTACTO_REFERENCIA,
                       p.NOMBRE, p.APELLIDO
                FROM EXPERIENCIA_LABORAL e
                LEFT JOIN POSTULANTE p ON e.ID_POSTULANTE = p.ID_POSTULANTE
                WHERE p.NOMBRE LIKE '%$query%' OR p.APELLIDO LIKE '%$query%' OR e.PUESTO_TRABAJO LIKE '%$query%'
                ORDER BY p.APELLIDO, p.NOMBRE
                """.trimIndent()
            }
            "HABILIDAD_POSTULANTE" -> {
                """
                SELECT hp.ID_POSTULANTE, h.ID_CATEGORIA_HABILIDAD, hp.ID_HABILIDAD, hp.ID_HABILIDAD_POSTULANTE, hp.NIVEL_DESTREZA,
                       p.NOMBRE, p.APELLIDO, h.NOMBRE_HABILIDAD
                FROM HABILIDAD_POSTULANTE hp
                LEFT JOIN POSTULANTE p ON hp.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN HABILIDAD h ON hp.ID_HABILIDAD = h.ID_HABILIDAD
                WHERE p.NOMBRE LIKE '%$query%' OR p.APELLIDO LIKE '%$query%' OR h.NOMBRE_HABILIDAD LIKE '%$query%'
                ORDER BY p.APELLIDO, p.NOMBRE, h.NOMBRE_HABILIDAD
                """.trimIndent()
            }
            "POSTULACION" -> {
                """
                SELECT p.ID_EMPRESA, p.ID_OFERTA, p.ID_POSTULANTE, p.ID_POSTULACION, 
                       p.FECHA_APLICACION, p.ESTADO_PROCESO,
                       post.NOMBRE, post.APELLIDO, o.TITULO_PUESTO
                FROM POSTULACION p
                LEFT JOIN POSTULANTE post ON p.ID_POSTULANTE = post.ID_POSTULANTE
                LEFT JOIN OFERTA_TRABAJO o ON p.ID_EMPRESA = o.ID_EMPRESA AND p.ID_OFERTA = o.ID_OFERTA
                WHERE post.NOMBRE LIKE '%$query%' OR post.APELLIDO LIKE '%$query%' OR o.TITULO_PUESTO LIKE '%$query%'
                ORDER BY post.APELLIDO, post.NOMBRE, o.TITULO_PUESTO
                """.trimIndent()
            }
            "RED_SOCIAL_POSTULANTE" -> {
                """
                SELECT r.ID_RED_POSTUALNTE, r.ID_POSTULANTE, r.ID_RED_SOCIAL, r.URL_PERFIL,
                       p.NOMBRE, p.APELLIDO, rs.NOMBRE_RED
                FROM RED_SOCIAL_POSTULANTE r
                LEFT JOIN POSTULANTE p ON r.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN RED_SOCIAL rs ON r.ID_RED_SOCIAL = rs.ID_RED_SOCIAL
                WHERE p.NOMBRE LIKE '%$query%' OR p.APELLIDO LIKE '%$query%' OR rs.NOMBRE_RED LIKE '%$query%'
                ORDER BY p.APELLIDO, p.NOMBRE, rs.NOMBRE_RED
                """.trimIndent()
            }
            "OFERTA_TRABAJO" -> {
                """
                SELECT o.ID_EMPRESA, o.ID_OFERTA, o.ID_GRADO_ACADEMICO, o.TITULO_PUESTO,
                       o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, o.EXPERIENCIA_ANIOS,
                       o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO,
                       e.NOMBRE_EMPRESA, g.NOMBRE_GRADO
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.ID_EMPRESA = e.ID_EMPRESA
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                ORDER BY o.FECHA_PUBLICACION DESC
                """.trimIndent()
            }
            "DETALLE_REQUISITO" -> {
                """
                SELECT d.ID_DETALLE, d.ID_EMPRESA, d.ID_OFERTA, d.DESCRIPCION_REQUISITO,
                       o.TITULO_PUESTO, e.NOMBRE_EMPRESA
                FROM DETALLE_REQUISITO d
                LEFT JOIN OFERTA_TRABAJO o ON d.ID_EMPRESA = o.ID_EMPRESA AND d.ID_OFERTA = o.ID_OFERTA
                LEFT JOIN EMPRESA e ON d.ID_EMPRESA = e.ID_EMPRESA
                ORDER BY d.ID_DETALLE DESC
                """.trimIndent()
            }
            "OFERTA_ACADEMICA" -> {
                """
                SELECT o.ID_OFERTA_ACADEMICA, o.ID_GRADO_ACADEMICO, o.ID_INSTITUCION,
                       i.NOMBRE_INSTITUCION, g.NOMBRE_GRADO
                FROM OFERTA_ACADEMICA o
                LEFT JOIN INSTITUCION i ON o.ID_INSTITUCION = i.ID_INSTITUCION
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                ORDER BY i.NOMBRE_INSTITUCION
                """.trimIndent()
            }
            "CERTIFICACION" -> {
                """
                SELECT c.ID_POSTULANTE, c.ID_CERTIFICACION, c.ID_INSTITUCION,
                       c.NOMBRE_CERTIFICACION, c.CODIGO_CERTIFICACION, c.FECHA_CERTIFICACION,
                       p.NOMBRE, p.APELLIDO, i.NOMBRE_INSTITUCION
                FROM CERTIFICACION c
                LEFT JOIN POSTULANTE p ON c.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION
                ORDER BY p.APELLIDO, p.NOMBRE, c.NOMBRE_CERTIFICACION
                """.trimIndent()
            }
            "FORMACION_ACADEMICA" -> {
                """
                SELECT f.ID_FORMACION, f.ID_POSTULANTE, f.ID_OFERTA_ACADEMICA,
                       f.TITULO_OBTENIDO, f.FECHA_OBTENCION,
                       p.NOMBRE, p.APELLIDO,
                       oa.ID_GRADO_ACADEMICO, oa.ID_INSTITUCION
                FROM FORMACION_ACADEMICA f
                LEFT JOIN POSTULANTE p ON f.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN OFERTA_ACADEMICA oa ON f.ID_OFERTA_ACADEMICA = oa.ID_OFERTA_ACADEMICA
                ORDER BY p.APELLIDO, p.NOMBRE, f.FECHA_OBTENCION DESC
                """.trimIndent()
            }
            else -> {
                val cols = getColumnsForTable(tableName).joinToString(", ")
                "SELECT $cols FROM $tableName"
            }
        }
        
        return try {
            val cursor = getDb().rawQuery(sql, null)
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
            "USUARIO" -> emptyMap()
            "POSTULANTE" -> mapOf(
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
                "ID_OFERTA_ACADEMICA" to FkReference("ID_OFERTA_ACADEMICA", "OFERTA_ACADEMICA", "OFERTA_ACADEMICA"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE")
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "ID_HABILIDAD" to FkReference("ID_HABILIDAD", "HABILIDAD", "NOMBRE_HABILIDAD", "ID_CATEGORIA_HABILIDAD"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "NOMBRE"),
                "ID_CATEGORIA_HABILIDAD" to FkReference("ID_CATEGORIA_HABILIDAD", "CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA")
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

    fun getDropdownOptions(tableName: String, displayColumn: String, idColumn: String? = null): List<Pair<String, String>> {
        if (tableName == "ROL") {
            return listOf(
                Pair("postulante", "postulante"),
                Pair("empresa", "empresa"),
                Pair("admin", "admin")
            )
        }

        if (tableName == "OFERTA_ACADEMICA") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT o.ID_OFERTA_ACADEMICA, i.NOMBRE_INSTITUCION, g.NOMBRE_GRADO FROM OFERTA_ACADEMICA o " +
                    "LEFT JOIN INSTITUCION i ON o.ID_INSTITUCION = i.ID_INSTITUCION " +
                    "LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO",
                    null
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val institucion = cursor.getString(1) ?: ""
                    val grado = cursor.getString(2) ?: ""
                    val display = "$institucion - $grado".take(50)
                    options.add(Pair(id, display.ifBlank { id }))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Caso especial para POSTULANTE: mostrar "NOMBRE APELLIDO"
        if (tableName == "POSTULANTE") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT ID_POSTULANTE, NOMBRE, APELLIDO FROM POSTULANTE",
                    null
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val nombre = cursor.getString(1) ?: ""
                    val apellido = cursor.getString(2) ?: ""
                    val display = "$nombre $apellido".trim()
                    options.add(Pair(id, display.ifBlank { id }))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Caso especial para HABILIDAD: mostrar "[Categoría] Nombre"
        if (tableName == "HABILIDAD") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT h.ID_HABILIDAD, h.NOMBRE_HABILIDAD, c.NOMBRE_CATEGORIA FROM HABILIDAD h " +
                    "LEFT JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD",
                    null
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val nombre = cursor.getString(1) ?: ""
                    val categoria = cursor.getString(2) ?: "Sin Categoría"
                    val display = "[$categoria] $nombre"
                    options.add(Pair(id, display))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        return try {
            val idCol = idColumn ?: getDb().rawQuery("PRAGMA table_info($tableName)", null).use { cols ->
                var foundId = "ID_"
                while (cols.moveToNext()) {
                    val name = cols.getString(1) ?: ""
                    if (name.startsWith("ID_") && name != "ID_GENERO" && name != "ID_TIPO_DOCUMENTO" && name != "ID_DISTRITO") {
                        foundId = name
                        break
                    }
                }
                foundId
            }
            val cursor = getDb().rawQuery("SELECT * FROM $tableName", null)
            val actualIdColIndex = cursor.getColumnIndex(idCol)
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val id = if (actualIdColIndex >= 0) cursor.getInt(actualIdColIndex).toString() else cursor.getInt(0).toString()
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

    fun getFilteredOptions(childTable: String, childFkColumn: String, parentId: String, displayColumn: String? = null): List<Pair<String, String>> {
        // Casos especiales de visualización (mantener consistencia con getDropdownOptions)
        if (childTable == "HABILIDAD") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT h.ID_HABILIDAD, h.NOMBRE_HABILIDAD, c.NOMBRE_CATEGORIA FROM HABILIDAD h " +
                    "LEFT JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD " +
                    "WHERE h.$childFkColumn = ?",
                    arrayOf(parentId)
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val nombre = cursor.getString(1) ?: ""
                    val categoria = cursor.getString(2) ?: "Sin Categoría"
                    val display = "[$categoria] $nombre"
                    options.add(Pair(id, display))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (childTable == "POSTULANTE") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT ID_POSTULANTE, NOMBRE, APELLIDO FROM POSTULANTE WHERE $childFkColumn = ?",
                    arrayOf(parentId)
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val nombre = cursor.getString(1) ?: ""
                    val apellido = cursor.getString(2) ?: ""
                    val display = "$nombre $apellido".trim()
                    options.add(Pair(id, display.ifBlank { id }))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (childTable == "OFERTA_ACADEMICA") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT o.ID_OFERTA_ACADEMICA, i.NOMBRE_INSTITUCION, g.NOMBRE_GRADO FROM OFERTA_ACADEMICA o " +
                    "LEFT JOIN INSTITUCION i ON o.ID_INSTITUCION = i.ID_INSTITUCION " +
                    "LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO " +
                    "WHERE o.$childFkColumn = ?",
                    arrayOf(parentId)
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0).toString()
                    val institucion = cursor.getString(1) ?: ""
                    val grado = cursor.getString(2) ?: ""
                    val display = "$grado - $institucion".take(50)
                    options.add(Pair(id, display))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Caso genérico
        val idColumn = when (childTable) {
            "OFERTA_ACADEMICA" -> "ID_OFERTA_ACADEMICA"
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "HABILIDAD" -> "ID_HABILIDAD"
            else -> {
                val cols = getDb().rawQuery("PRAGMA table_info($childTable)", null)
                var found = "ID"
                cols.use { c ->
                    while (c.moveToNext()) {
                        val name = c.getString(1) ?: ""
                        if (name.startsWith("ID_") && name != "ID_GENERO" && name != "ID_TIPO_DOCUMENTO") {
                            found = name
                            break
                        }
                    }
                }
                found
            }
        }
        
        return try {
            val cursor = getDb().rawQuery("SELECT * FROM $childTable WHERE $childFkColumn = ?", arrayOf(parentId))
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val idColIdx = cursor.getColumnIndex(idColumn)
                val id = if (idColIdx >= 0) cursor.getInt(idColIdx).toString() else cursor.getInt(0).toString()
                
                val name = if (displayColumn != null) {
                    val colIdx = cursor.getColumnIndex(displayColumn)
                    if (colIdx >= 0) cursor.getString(colIdx) ?: id else id
                } else {
                    // Fallback a la segunda columna si no hay displayColumn
                    if (cursor.columnCount > 1) cursor.getString(1) ?: id else id
                }

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

    fun getDepartamentoByMunicipio(idMunicipio: String): String? {
        return try {
            val cursor = getDb().rawQuery(
                "SELECT ID_DEPARTAMENTO FROM MUNICIPIO WHERE ID_MUNICIPIO = ?",
                arrayOf(idMunicipio)
            )
            val result = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    fun getMunicipioByDistrito(idDistrito: String): String? {
        return try {
            val cursor = getDb().rawQuery(
                "SELECT ID_MUNICIPIO FROM DISTRITO WHERE ID_DISTRITO = ?",
                arrayOf(idDistrito)
            )
            val result = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    fun getColumnsForTable(tableName: String): List<String> {
        return when (tableName) {
            "USUARIO" -> listOf("ID_USUARIO", "USERNAME", "PASSWORD", "ROL")
            "POSTULANTE" -> listOf("ID_POSTULANTE", "ID_GENERO", "ID_TIPO_DOCUMENTO", "NUM_DOCUMENTO", "ID_DISTRITO", "NOMBRE", "APELLIDO", "FECHA_NACIMIENTO", "NUP", "DIRECCION_DETALLE", "TELEFONO_CASA", "TELEFONO_CELULAR", "EMAIL")
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
            "RED_SOCIAL" -> listOf("ID_RED_SOCIAL", "NOMBRE_RED")
            "OFERTA_ACADEMICA" -> listOf("ID_OFERTA_ACADEMICA", "ID_GRADO_ACADEMICO", "ID_INSTITUCION")
            "OFERTA_TRABAJO" -> listOf("ID_EMPRESA", "ID_OFERTA", "ID_GRADO_ACADEMICO", "TITULO_PUESTO", "FECHA_PUBLICACION", "FECHA_CADUCIDAD", "EXPERIENCIA_ANIOS", "EDAD_MINIMA", "EDAD_MAXIMA", "DESCRIPCION_OFERTA_TRABAJO")
            "CERTIFICACION" -> listOf("ID_POSTULANTE", "ID_CERTIFICACION", "ID_INSTITUCION", "NOMBRE_CERTIFICACION", "CODIGO_CERTIFICACION", "FECHA_CERTIFICACION")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE", "ID_EXPERIENCIA", "ID_EMPRESA", "PUESTO_TRABAJO", "FECHA_INICIO", "FECHA_FIN", "DESCP_EXPERIENCIA_LABORAL", "CONTACTO_REFERENCIA")
            "FORMACION_ACADEMICA" -> listOf("ID_FORMACION", "ID_POSTULANTE", "ID_OFERTA_ACADEMICA", "TITULO_OBTENIDO", "FECHA_OBTENCION")
            "HABILIDAD_POSTULANTE" -> listOf("ID_POSTULANTE", "ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "ID_HABILIDAD_POSTULANTE", "NIVEL_DESTREZA")
            "POSTULACION" -> listOf("ID_EMPRESA", "ID_OFERTA", "ID_POSTULANTE", "ID_POSTULACION", "FECHA_APLICACION", "ESTADO_PROCESO")
            "DETALLE_REQUISITO" -> listOf("ID_DETALLE", "ID_EMPRESA", "ID_OFERTA", "DESCRIPCION_REQUISITO")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_RED_POSTUALNTE", "ID_POSTULANTE", "ID_RED_SOCIAL", "URL_PERFIL")
            else -> listOf("NOMBRE")
        }
    }
}
