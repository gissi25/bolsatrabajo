package sv.ues.fia.eisi.bt.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.utils.Constants
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
        val usernameLower = username.lowercase()
        val cursor = getDb().rawQuery("SELECT * FROM USUARIO WHERE USERNAME = ?", arrayOf(usernameLower))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        
        val idUsuario = cursor.getInt(0)
        val storedPassword = cursor.getString(2)
        val rol = cursor.getString(3)
        
        cursor.close()
        
        if (PasswordHasher.verify(password, storedPassword)) {
            return Usuario(idUsuario, usernameLower, storedPassword, rol)
        }
        return null
    }

    fun register(username: String, password: String, rol: String = "postulante"): Long {
        val usernameLower = username.lowercase()
        val hashedPassword = PasswordHasher.hash(password)
        
        return try {
            val checkCursor = getDb().rawQuery("SELECT COUNT(*) FROM USUARIO WHERE USERNAME = ?", arrayOf(usernameLower))
            checkCursor.moveToFirst()
            val existe = checkCursor.getInt(0)
            checkCursor.close()
            if (existe > 0) return -2L
            
            getDb().execSQL("INSERT INTO USUARIO (USERNAME, PASSWORD, ROL) VALUES ('$usernameLower', '$hashedPassword', '$rol')")
            
            val maxCursor = getDb().rawQuery("SELECT MAX(ID_USUARIO) FROM USUARIO", null)
            maxCursor.moveToFirst()
            val newId = maxCursor.getInt(0)
            maxCursor.close()
            newId.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    fun deleteRecord(tableName: String, id: String): Boolean {
        return try {
            val pkCols = getPrimaryKeyColumns(tableName)
            if (pkCols.size == 1) {
                getDb().execSQL("DELETE FROM $tableName WHERE ${pkCols[0]} = '$id'")
            } else {
                val parts = id.split("|")
                val whereClause = pkCols.mapIndexed { i, col -> "$col = '${parts.getOrElse(i) { "" }}'" }.joinToString(" AND ")
                getDb().execSQL("DELETE FROM $tableName WHERE $whereClause")
            }
            true
        } catch (e: android.database.sqlite.SQLiteException) {
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    fun deleteRecordByRow(tableName: String, rowData: List<String>): Boolean {
        val columns = getColumnsForTable(tableName)
        val pkColumns = getPrimaryKeyColumns(tableName)
        return try {
            val whereClause = pkColumns.map { col ->
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



    data class DependencyInfo(val tableName: String, val displayName: String, val count: Int, val depth: Int = 1)

    fun getDeleteDependencies(tableName: String, id: String): List<DependencyInfo> {
        val pkCols = getPrimaryKeyColumns(tableName)
        val pkValues = if (pkCols.size > 1) id.split("|") else listOf(id)
        val result = mutableListOf<DependencyInfo>()

        fun traverse(currentTable: String, currentPkValues: List<String>, depth: Int, seen: MutableSet<String>) {
            if (currentTable in seen) return
            seen.add(currentTable)
            val children = getAllDependencies(currentTable)
            for ((childTable, childFkCols) in children) {
                val whereClause = childFkCols.mapIndexed { i, col ->
                    val v = currentPkValues.getOrElse(i) { "" }
                    "$col = '$v'"
                }.joinToString(" AND ")
                val cursor = getDb().rawQuery(
                    "SELECT COUNT(*) FROM $childTable WHERE $whereClause", null
                )
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                cursor.close()
                if (count > 0) {
                    result.add(DependencyInfo(
                        childTable,
                        childTable.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        count,
                        depth
                    ))
                    val childPkCols = getPrimaryKeyColumns(childTable)
                    val allChildPks = mutableListOf<List<String>>()
                    val pkCursor = getDb().rawQuery(
                        "SELECT ${childPkCols.joinToString(",")} FROM $childTable WHERE $whereClause",
                        null
                    )
                    while (pkCursor.moveToNext()) {
                        val childPkVals = childPkCols.map { col ->
                            val idx = pkCursor.getColumnIndex(col)
                            if (idx >= 0) pkCursor.getString(idx) ?: "" else ""
                        }
                        allChildPks.add(childPkVals)
                    }
                    pkCursor.close()
                    for (childPkVals in allChildPks) {
                        traverse(childTable, childPkVals, depth + 1, seen)
                    }
                }
            }
        }

        traverse(tableName, pkValues, 1, mutableSetOf())
        return result
    }

    private fun getAllDependencies(tableName: String): List<Pair<String, List<String>>> {
        return when (tableName) {
            "DEPARTAMENTO" -> listOf("MUNICIPIO" to listOf("ID_DEPARTAMENTO"))
            "MUNICIPIO" -> listOf("DISTRITO" to listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO"))
            "DISTRITO" -> listOf("POSTULANTE" to listOf("ID_DISTRITO_DEPTO", "ID_DISTRITO_MUNICIPIO", "ID_DISTRITO_ID"), "EMPRESA" to listOf("ID_DISTRITO_DEPTO", "ID_DISTRITO_MUNICIPIO", "ID_DISTRITO_ID"))
            "GENERO" -> listOf("POSTULANTE" to listOf("ID_GENERO"))
            "TIPO_DOCUMENTO" -> listOf("POSTULANTE" to listOf("ID_TIPO_DOCUMENTO"))
            "INSTITUCION" -> listOf("OFERTA_ACADEMICA" to listOf("ID_INSTITUCION"), "CERTIFICACION" to listOf("ID_INSTITUCION"))
            "GRADO_ACADEMICO" -> listOf("OFERTA_TRABAJO" to listOf("ID_GRADO_ACADEMICO"), "OFERTA_ACADEMICA" to listOf("ID_GRADO_ACADEMICO"), "POSTULANTE" to listOf("ID_GRADO_ACADEMICO"))
            "TIPO_CERTIFICACION" -> listOf("CERTIFICACION" to listOf("ID_TIPO_CERTIFICACION"))
            "CATEGORIA_HABILIDAD" -> listOf("HABILIDAD" to listOf("ID_CATEGORIA_HABILIDAD"))
            "HABILIDAD" -> listOf("HABILIDAD_POSTULANTE" to listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD"))
            "RED_SOCIAL" -> listOf("RED_SOCIAL_POSTULANTE" to listOf("ID_RED_SOCIAL"))
            "EMPRESA" -> listOf("OFERTA_TRABAJO" to listOf("NIT"), "EXPERIENCIA_LABORAL" to listOf("NIT"))
            "POSTULANTE" -> listOf(
                "POSTULACION" to listOf("ID_POSTULANTE"),
                "EXPERIENCIA_LABORAL" to listOf("ID_POSTULANTE"),
                "FORMACION_ACADEMICA" to listOf("ID_POSTULANTE"),
                "CERTIFICACION" to listOf("ID_POSTULANTE"),
                "HABILIDAD_POSTULANTE" to listOf("ID_POSTULANTE"),
                "RED_SOCIAL_POSTULANTE" to listOf("ID_POSTULANTE")
            )
            "OFERTA_TRABAJO" -> listOf("DETALLE_REQUISITO" to listOf("NIT", "ID_OFERTA"), "POSTULACION" to listOf("NIT", "ID_OFERTA"))
            "OFERTA_ACADEMICA" -> listOf("FORMACION_ACADEMICA" to listOf("ID_OFERTA_ACADEMICA"))
            else -> emptyList()
        }
    }


    private fun getPrimaryKeyColumns(tableName: String): List<String> = Constants.getPrimaryKeyColumns(tableName)

    private fun getAutoGenColumns(): Set<String> {
        return setOf(
            "ID_USUARIO", "ID_GENERO", "ID_TIPO_DOCUMENTO",
            "ID_DEPARTAMENTO", "ID_GRADO_ACADEMICO", "ID_RED_SOCIAL",
            "ID_CATEGORIA_HABILIDAD", "ID_TIPO_CERTIFICACION"
        )
    }

    private fun getAutoGenColumn(tableName: String): String? = Constants.getAutoGenColumn(tableName)

    fun insertRecord(tableName: String, values: List<Any>): Long {
        val idCol = getAutoGenColumn(tableName)
        var columns = getColumnsForTable(tableName).filter { it != idCol }
        val finalValues = values.toMutableList()

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
                if (plainPassword.isNotBlank() && plainPassword.length < 50) {
                    finalValues[pwdIndex] = PasswordHasher.hash(plainPassword)
                }
            }
        }

        val lowerFields = setOf(
            "NOMBRE_CATEGORIA", "NOMBRE_GENERO", "NOMBRE_TIPO", "NOMBRE_DEPARTAMENTO",
            "NOMBRE_MUNICIPIO", "NOMBRE_DISTRITO", "NOMBRE_INSTITUCION", "NOMBRE_GRADO",
            "NOMBRE_RED", "NOMBRE_HABILIDAD", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO",
            "NOMBRE", "APELLIDO", "DIRECCION_DETALLE", "EMAIL",
            "TITULO_PUESTO", "DESCRIPCION_OFERTA_TRABAJO", "DESCRIPCION_REQUISITO",
            "NOMBRE_CERTIFICACION", "PUESTO_TRABAJO", "DESCP_EXPERIENCIA_LABORAL",
            "CONTACTO_REFERENCIA", "TITULO_OBTENIDO", "ESTADO_PROCESO", "URL_PERFIL",
            "USERNAME", "ROL"
        )

        val upperFields = emptySet<String>()

        val processedValues = finalValues.mapIndexed { i, v ->
            val col = columns.getOrNull(i) ?: ""
            when {
                col in lowerFields -> v.toString().lowercase()
                col in upperFields -> v.toString().uppercase()
                else -> v
            }
        }

        val columnsMap = columns.zip(processedValues).toMap()

        checkDuplicateInsert(tableName, columnsMap, idCol)

        val sql = if (idCol != null) {
            val maxIdCursor = getDb().rawQuery("SELECT MAX($idCol) FROM $tableName", null)
            maxIdCursor.moveToFirst()
            val nextId = if (maxIdCursor.isNull(0)) 1 else maxIdCursor.getInt(0) + 1
            maxIdCursor.close()
            val cols = "$idCol, ${columns.joinToString(", ")}"
            val vals = "$nextId, ${processedValues.joinToString(", ") { "'$it'" }}"
            "INSERT INTO $tableName ($cols) VALUES ($vals)"
        } else {
            val cols = columns.joinToString(", ")
            val vals = processedValues.joinToString(", ") { "'$it'" }
            "INSERT INTO $tableName ($cols) VALUES ($vals)"
        }

        android.util.Log.d("MainRepository", "SQL: $sql")

        getDb().beginTransaction()
        try {
            getDb().execSQL(sql)
            getDb().setTransactionSuccessful()
            getDb().endTransaction()
            return if (idCol != null) {
                val maxIdCursor = getDb().rawQuery("SELECT MAX($idCol) FROM $tableName", null)
                maxIdCursor.moveToFirst()
                val result = maxIdCursor.getInt(0)
                maxIdCursor.close()
                result.toLong()
            } else 1L
        } catch (e: Exception) {
            getDb().endTransaction()
            throw Exception(TriggerErrorTranslator.translate(e.message))
        }
    }

    private fun checkDuplicateInsert(tableName: String, cols: Map<String, Any>, idCol: String?) {
        if (idCol == null) {
            val pkCols = getPrimaryKeyColumns(tableName)
            if (pkCols.isNotEmpty()) {
                val whereClause = pkCols.joinToString(" AND ") { pk ->
                    val v = cols[pk]?.toString() ?: ""
                    "$pk = '$v'"
                }
                if (whereClause.isNotBlank()) {
                    val dupCursor = getDb().rawQuery("SELECT COUNT(*) FROM $tableName WHERE $whereClause", null)
                    dupCursor.moveToFirst()
                    val exists = dupCursor.getInt(0) > 0
                    dupCursor.close()
                    if (exists) throw Exception("Duplicado: Ya existe un registro con esa clave")
                }
            }
        }

        val dupChecks = getDuplicateCheckFields(tableName)
        for ((whereSql, errorMsg) in dupChecks) {
            val resolvedSql = replacePlaceholders(whereSql, cols)
            val dupCursor = getDb().rawQuery("SELECT COUNT(*) FROM $tableName WHERE $resolvedSql", null)
            dupCursor.moveToFirst()
            val exists = dupCursor.getInt(0) > 0
            dupCursor.close()
            if (exists) throw Exception(errorMsg)
        }
    }

    private fun checkDuplicateUpdate(tableName: String, cols: Map<String, Any>, whereCols: List<String>, whereVals: List<String>) {
        val dupChecks = getDuplicateCheckFields(tableName)
        for ((whereSql, errorMsg) in dupChecks) {
            val resolvedSql = replacePlaceholders(whereSql, cols)
            val excludeSql = whereCols.mapIndexed { i, col -> "$col != '${whereVals[i]}'" }.joinToString(" AND ")
            val fullSql = if (excludeSql.isNotBlank()) "$resolvedSql AND $excludeSql" else resolvedSql
            val dupCursor = getDb().rawQuery("SELECT COUNT(*) FROM $tableName WHERE $fullSql", null)
            dupCursor.moveToFirst()
            val exists = dupCursor.getInt(0) > 0
            dupCursor.close()
            if (exists) throw Exception(errorMsg)
        }
    }

    private fun getDuplicateCheckFields(tableName: String): List<Pair<String, String>> {
        return when (tableName) {
            "GENERO" -> listOf("LOWER(NOMBRE_GENERO) = LOWER('{NOMBRE_GENERO}')" to "Ya existe un genero con ese nombre")
            "CATEGORIA_HABILIDAD" -> listOf("LOWER(NOMBRE_CATEGORIA) = LOWER('{NOMBRE_CATEGORIA}')" to "Ya existe una categoria con ese nombre")
            "TIPO_DOCUMENTO" -> listOf("LOWER(NOMBRE_TIPO) = LOWER('{NOMBRE_TIPO}')" to "Ya existe un tipo de documento con ese nombre")
            "DEPARTAMENTO" -> listOf("LOWER(NOMBRE_DEPARTAMENTO) = LOWER('{NOMBRE_DEPARTAMENTO}')" to "Ya existe un departamento con ese nombre")
            "GRADO_ACADEMICO" -> listOf("LOWER(NOMBRE_GRADO) = LOWER('{NOMBRE_GRADO}')" to "Ya existe un grado academico con ese nombre")
            "RED_SOCIAL" -> listOf("LOWER(NOMBRE_RED) = LOWER('{NOMBRE_RED}')" to "Ya existe una red social con ese nombre")
            "TIPO_CERTIFICACION" -> listOf("LOWER(NOMBRE_TIPO) = LOWER('{NOMBRE_TIPO}')" to "Ya existe un tipo de certificacion con ese nombre")
            "INSTITUCION" -> listOf("LOWER(NOMBRE_INSTITUCION) = LOWER('{NOMBRE_INSTITUCION}')" to "Ya existe una institucion con ese nombre")
            "MUNICIPIO" -> listOf("ID_DEPARTAMENTO = {ID_DEPARTAMENTO} AND LOWER(NOMBRE_MUNICIPIO) = LOWER('{NOMBRE_MUNICIPIO}')" to "Ya existe un municipio con ese nombre en el departamento")
            "DISTRITO" -> listOf("ID_DEPARTAMENTO = {ID_DEPARTAMENTO} AND ID_MUNICIPIO = {ID_MUNICIPIO} AND LOWER(NOMBRE_DISTRITO) = LOWER('{NOMBRE_DISTRITO}')" to "Ya existe un distrito con ese nombre en el municipio")
            "HABILIDAD" -> listOf("LOWER(NOMBRE_HABILIDAD) = LOWER('{NOMBRE_HABILIDAD}')" to "Ya existe una habilidad con ese nombre")
            "EMPRESA" -> listOf(
                "NIT = '{NIT}'" to "Ya existe una empresa con ese NIT",
                "LOWER(NOMBRE_EMPRESA) = LOWER('{NOMBRE_EMPRESA}')" to "Ya existe una empresa con ese nombre"
            )
            "POSTULANTE" -> listOf(
                "NUM_DOCUMENTO = '{NUM_DOCUMENTO}'" to "Ya existe un postulante con ese documento",
                "LOWER(EMAIL) = LOWER('{EMAIL}')" to "Ya existe un postulante con ese email"
            )
            "USUARIO" -> listOf("LOWER(USERNAME) = LOWER('{USERNAME}')" to "Ya existe un usuario con ese nombre")
            "OFERTA_TRABAJO" -> listOf("NIT = '{NIT}' AND LOWER(TITULO_PUESTO) = LOWER('{TITULO_PUESTO}')" to "Ya existe una oferta con ese titulo en la empresa")
            "DETALLE_REQUISITO" -> listOf("NIT = '{NIT}' AND ID_OFERTA = '{ID_OFERTA}' AND LOWER(DESCRIPCION_REQUISITO) = LOWER('{DESCRIPCION_REQUISITO}')" to "Ya existe un requisito con esa descripcion en la oferta")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE = '{ID_POSTULANTE}' AND NIT = '{NIT}' AND LOWER(PUESTO_TRABAJO) = LOWER('{PUESTO_TRABAJO}')" to "Ya existe una experiencia con ese puesto para el postulante")
            "CERTIFICACION" -> listOf("ID_POSTULANTE = '{ID_POSTULANTE}' AND LOWER(NOMBRE_CERTIFICACION) = LOWER('{NOMBRE_CERTIFICACION}')" to "Ya existe una certificacion con ese nombre para el postulante")
            "POSTULACION" -> listOf("ID_POSTULANTE = '{ID_POSTULANTE}' AND NIT = '{NIT}' AND ID_OFERTA = '{ID_OFERTA}'" to "El postulante ya aplico a esta oferta")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_POSTULANTE = '{ID_POSTULANTE}' AND ID_RED_SOCIAL = {ID_RED_SOCIAL}" to "La red social ya esta vinculada al postulante")
            "HABILIDAD_POSTULANTE" -> listOf("ID_CATEGORIA_HABILIDAD = {ID_CATEGORIA_HABILIDAD} AND ID_HABILIDAD = '{ID_HABILIDAD}' AND ID_POSTULANTE = '{ID_POSTULANTE}'" to "La habilidad ya esta asignada al postulante")
            "OFERTA_ACADEMICA" -> listOf("ID_INSTITUCION = '{ID_INSTITUCION}' AND ID_GRADO_ACADEMICO = {ID_GRADO_ACADEMICO}" to "Ya existe una oferta academica para esa institucion y grado")
            else -> emptyList()
        }
    }

    private fun replacePlaceholders(sql: String, cols: Map<String, Any>): String {
        var result = sql
        val tokens = mutableMapOf<String, String>()
        cols.forEach { (key, _) ->
            val token = "\u0000$key\u0000"
            result = result.replace("{$key}", token)
            tokens[key] = token
        }
        tokens.forEach { (key, token) ->
            result = result.replace(token, cols[key]?.toString() ?: "")
        }
        return result
    }

    fun updateRecord(tableName: String, id: Any, values: List<Any>): Int {
        val idCol = getAutoGenColumn(tableName)
        var columns = getColumnsForTable(tableName).filter { it != idCol }
        val finalValues = values.toMutableList()

        val lowerFields = setOf(
            "NOMBRE_CATEGORIA", "NOMBRE_GENERO", "NOMBRE_TIPO", "NOMBRE_DEPARTAMENTO",
            "NOMBRE_MUNICIPIO", "NOMBRE_DISTRITO", "NOMBRE_INSTITUCION", "NOMBRE_GRADO",
            "NOMBRE_RED", "NOMBRE_HABILIDAD", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO",
            "NOMBRE", "APELLIDO", "DIRECCION_DETALLE", "EMAIL",
            "TITULO_PUESTO", "DESCRIPCION_OFERTA_TRABAJO", "DESCRIPCION_REQUISITO",
            "NOMBRE_CERTIFICACION", "PUESTO_TRABAJO", "DESCP_EXPERIENCIA_LABORAL",
            "CONTACTO_REFERENCIA", "TITULO_OBTENIDO", "ESTADO_PROCESO", "URL_PERFIL",
            "USERNAME", "ROL"
        )

        val upperFields = emptySet<String>()

        val processedValues = finalValues.mapIndexed { i, v ->
            val col = columns.getOrNull(i) ?: ""
            when {
                col in lowerFields -> v.toString().lowercase()
                col in upperFields -> v.toString().uppercase()
                else -> v
            }
        }

        val setClause = columns.zip(processedValues).map { "${it.first} = '${it.second}'" }.joinToString(", ")

        val pkCols = getPrimaryKeyColumns(tableName)
        val idStr = id.toString()
        val (wherePkCols, wherePkVals) = if (idCol != null) {
            listOf(idCol) to listOf(idStr)
        } else if (pkCols.size == 1) {
            pkCols to listOf(idStr)
        } else {
            pkCols to idStr.split("|")
        }

        val columnsMap = columns.zip(processedValues).toMap()
        checkDuplicateUpdate(tableName, columnsMap, wherePkCols, wherePkVals)

        val whereClause = if (idCol != null) {
            "$idCol = '$id'"
        } else {
            val pkCols = getPrimaryKeyColumns(tableName)
            val idStr = id.toString()
            if (pkCols.size == 1) {
                "${pkCols[0]} = '$idStr'"
            } else {
                val parts = idStr.split("|")
                pkCols.mapIndexed { i, col -> "$col = '${parts.getOrElse(i) { "" }}'" }.joinToString(" AND ")
            }
        }

        try {
            getDb().execSQL("UPDATE $tableName SET $setClause WHERE $whereClause")
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
            "TIPO_CERTIFICACION",
            "HABILIDAD", "HABILIDAD_POSTULANTE", "POSTULACION",
            "RED_SOCIAL_POSTULANTE"
        )
        
        return try {
            tables.map { tableName ->
                val cursor = getDb().rawQuery("SELECT COUNT(*) FROM $tableName", null)
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                cursor.close()
                TableInfo(tableName, tableName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, count)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun insertSeedData(): String? {
        val tablesToCheck = listOf(
            "CATEGORIA_HABILIDAD", "GENERO", "DEPARTAMENTO", "MUNICIPIO",
            "DISTRITO", "INSTITUCION", "GRADO_ACADEMICO", "RED_SOCIAL",
            "HABILIDAD", "EMPRESA", "TIPO_DOCUMENTO", "OFERTA_ACADEMICA",
            "TIPO_CERTIFICACION"
        )

        try {
            for (table in tablesToCheck) {
                val cursor = getDb().rawQuery("SELECT COUNT(*) FROM $table", null)
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                cursor.close()
                if (count > 0) {
                    return "Ya existen datos en la base de datos"
                }
            }

            getDb().beginTransaction()
            try {
                for (nombre in SeedData.DEPARTAMENTOS) {
                    insertRecord("DEPARTAMENTO", listOf(nombre))
                }
                for (nombre in SeedData.GENEROS) {
                    insertRecord("GENERO", listOf(nombre))
                }
                for (nombre in SeedData.CATEGORIAS_HABILIDAD) {
                    insertRecord("CATEGORIA_HABILIDAD", listOf(nombre))
                }
                for (nombre in SeedData.GRADOS_ACADEMICOS) {
                    insertRecord("GRADO_ACADEMICO", listOf(nombre))
                }
                for (nombre in SeedData.REDES_SOCIALES) {
                    insertRecord("RED_SOCIAL", listOf(nombre))
                }
                for (nombre in SeedData.TIPOS_CERTIFICACION) {
                    insertRecord("TIPO_CERTIFICACION", listOf(nombre))
                }
                for (row in SeedData.INSTITUCIONES) {
                    insertRecord("INSTITUCION", row)
                }
                for (row in SeedData.MUNICIPIOS) {
                    insertRecord("MUNICIPIO", row)
                }
                for (row in SeedData.DISTRITOS) {
                    insertRecord("DISTRITO", row)
                }
                for (row in SeedData.HABILIDADES) {
                    insertRecord("HABILIDAD", row)
                }
                for (row in SeedData.EMPRESAS) {
                    insertRecord("EMPRESA", row)
                }
                for (nombre in SeedData.TIPOS_DOCUMENTO) {
                    insertRecord("TIPO_DOCUMENTO", listOf(nombre))
                }
                for (row in SeedData.OFERTAS_ACADEMICAS) {
                    insertRecord("OFERTA_ACADEMICA", row)
                }
                getDb().setTransactionSuccessful()
            } finally {
                getDb().endTransaction()
            }
            return null
        } catch (e: Exception) {
            return "Error al insertar datos: ${e.message}"
        }
    }

    fun searchTable(tableName: String, query: String): List<List<Any>> {
        val sql = when (tableName) {
            "USUARIO" -> {
                "SELECT ID_USUARIO, USERNAME, PASSWORD, ROL FROM USUARIO WHERE USERNAME LIKE '%$query%'"
            }
            "HABILIDAD" -> {
                """
                SELECT h.ID_CATEGORIA_HABILIDAD, h.ID_HABILIDAD, h.NOMBRE_HABILIDAD, IFNULL(c.NOMBRE_CATEGORIA, 'Sin Categoria')
                FROM HABILIDAD h
                LEFT JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
                WHERE h.NOMBRE_HABILIDAD LIKE '%$query%'
                ORDER BY h.NOMBRE_HABILIDAD
                """.trimIndent()
            }
            "MUNICIPIO" -> {
                """
                SELECT m.ID_DEPARTAMENTO, m.ID_MUNICIPIO, m.NOMBRE_MUNICIPIO, d.NOMBRE_DEPARTAMENTO
                FROM MUNICIPIO m
                LEFT JOIN DEPARTAMENTO d ON m.ID_DEPARTAMENTO = d.ID_DEPARTAMENTO
                WHERE m.NOMBRE_MUNICIPIO LIKE '%$query%'
                """.trimIndent()
            }
            "DISTRITO" -> {
                """
                SELECT d.ID_DEPARTAMENTO, d.ID_MUNICIPIO, d.ID_DISTRITO, d.NOMBRE_DISTRITO, m.NOMBRE_MUNICIPIO
                FROM DISTRITO d
                LEFT JOIN MUNICIPIO m ON d.ID_DEPARTAMENTO = m.ID_DEPARTAMENTO AND d.ID_MUNICIPIO = m.ID_MUNICIPIO
                WHERE d.NOMBRE_DISTRITO LIKE '%$query%'
                """.trimIndent()
            }
            "EXPERIENCIA_LABORAL" -> {
                """
                SELECT e.ID_POSTULANTE, e.NIT, e.ID_EXPERIENCIA, 
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
                SELECT hp.ID_CATEGORIA_HABILIDAD, hp.ID_HABILIDAD, hp.ID_POSTULANTE, hp.NIVEL_DESTREZA,
                       p.NOMBRE, p.APELLIDO, h.NOMBRE_HABILIDAD
                FROM HABILIDAD_POSTULANTE hp
                LEFT JOIN POSTULANTE p ON hp.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN HABILIDAD h ON hp.ID_CATEGORIA_HABILIDAD = h.ID_CATEGORIA_HABILIDAD AND hp.ID_HABILIDAD = h.ID_HABILIDAD
                WHERE p.NOMBRE LIKE '%$query%' OR p.APELLIDO LIKE '%$query%' OR h.NOMBRE_HABILIDAD LIKE '%$query%'
                ORDER BY p.APELLIDO, p.NOMBRE, h.NOMBRE_HABILIDAD
                """.trimIndent()
            }
            "POSTULACION" -> {
                """
                SELECT p.ID_POSTULACION, p.NIT, p.ID_OFERTA, p.ID_POSTULANTE, 
                       p.FECHA_APLICACION, p.ESTADO_PROCESO,
                       post.NOMBRE, post.APELLIDO, o.TITULO_PUESTO
                FROM POSTULACION p
                LEFT JOIN POSTULANTE post ON p.ID_POSTULANTE = post.ID_POSTULANTE
                LEFT JOIN OFERTA_TRABAJO o ON p.NIT = o.NIT AND p.ID_OFERTA = o.ID_OFERTA
                WHERE post.NOMBRE LIKE '%$query%' OR post.APELLIDO LIKE '%$query%' OR o.TITULO_PUESTO LIKE '%$query%' OR p.ESTADO_PROCESO LIKE '%$query%'
                ORDER BY post.APELLIDO, post.NOMBRE, o.TITULO_PUESTO
                """.trimIndent()
            }
            "RED_SOCIAL_POSTULANTE" -> {
                """
                SELECT r.ID_POSTULANTE, r.ID_RED_SOCIAL, r.URL_PERFIL,
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
                SELECT o.NIT, o.ID_OFERTA, o.ID_GRADO_ACADEMICO, o.TITULO_PUESTO,
                       o.FECHA_PUBLICACION, o.FECHA_CADUCIDAD, o.EXPERIENCIA_ANIOS,
                       o.EDAD_MINIMA, o.EDAD_MAXIMA, o.DESCRIPCION_OFERTA_TRABAJO,
                       e.NOMBRE_EMPRESA, g.NOMBRE_GRADO
                FROM OFERTA_TRABAJO o
                LEFT JOIN EMPRESA e ON o.NIT = e.NIT
                LEFT JOIN GRADO_ACADEMICO g ON o.ID_GRADO_ACADEMICO = g.ID_GRADO_ACADEMICO
                WHERE o.TITULO_PUESTO LIKE '%$query%' OR e.NOMBRE_EMPRESA LIKE '%$query%' OR o.DESCRIPCION_OFERTA_TRABAJO LIKE '%$query%'
                ORDER BY o.FECHA_PUBLICACION DESC
                """.trimIndent()
            }
            "DETALLE_REQUISITO" -> {
                """
                SELECT d.NIT, d.ID_OFERTA, d.ID_DETALLE, d.DESCRIPCION_REQUISITO,
                       o.TITULO_PUESTO, e.NOMBRE_EMPRESA, o.FECHA_CADUCIDAD
                FROM DETALLE_REQUISITO d
                LEFT JOIN OFERTA_TRABAJO o ON d.NIT = o.NIT AND d.ID_OFERTA = o.ID_OFERTA
                LEFT JOIN EMPRESA e ON d.NIT = e.NIT
                WHERE d.DESCRIPCION_REQUISITO LIKE '%$query%' OR o.TITULO_PUESTO LIKE '%$query%' OR e.NOMBRE_EMPRESA LIKE '%$query%'
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
                WHERE o.ID_OFERTA_ACADEMICA LIKE '%$query%' OR i.NOMBRE_INSTITUCION LIKE '%$query%' OR g.NOMBRE_GRADO LIKE '%$query%'
                ORDER BY i.NOMBRE_INSTITUCION
                """.trimIndent()
            }
            "CERTIFICACION" -> {
                """
                SELECT c.ID_CERTIFICACION, c.ID_INSTITUCION, c.ID_POSTULANTE,
                       c.ID_TIPO_CERTIFICACION, c.NOMBRE_CERTIFICACION, c.FECHA_CERTIFICACION, c.PERIODO,
                       p.NOMBRE, p.APELLIDO, i.NOMBRE_INSTITUCION, tc.NOMBRE_TIPO
                FROM CERTIFICACION c
                LEFT JOIN POSTULANTE p ON c.ID_POSTULANTE = p.ID_POSTULANTE
                LEFT JOIN INSTITUCION i ON c.ID_INSTITUCION = i.ID_INSTITUCION
                LEFT JOIN TIPO_CERTIFICACION tc ON c.ID_TIPO_CERTIFICACION = tc.ID_TIPO_CERTIFICACION
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
                "ID_GRADO_ACADEMICO" to FkReference("ID_GRADO_ACADEMICO", "GRADO_ACADEMICO", "NOMBRE_GRADO"),
                "ID_DISTRITO_DEPTO" to FkReference("ID_DISTRITO_DEPTO", "DEPARTAMENTO", "NOMBRE_DEPARTAMENTO"),
                "ID_DISTRITO_MUNICIPIO" to FkReference("ID_DISTRITO_MUNICIPIO", "MUNICIPIO", "NOMBRE_MUNICIPIO"),
                "ID_DISTRITO_ID" to FkReference("ID_DISTRITO_ID", "DISTRITO", "NOMBRE_DISTRITO")
            )
            "MUNICIPIO" -> mapOf("ID_DEPARTAMENTO" to FkReference("ID_DEPARTAMENTO", "DEPARTAMENTO", "NOMBRE_DEPARTAMENTO"))
            "DISTRITO" -> mapOf(
                "ID_DEPARTAMENTO" to FkReference("ID_DEPARTAMENTO", "DEPARTAMENTO", "NOMBRE_DEPARTAMENTO"),
                "ID_MUNICIPIO" to FkReference("ID_MUNICIPIO", "MUNICIPIO", "NOMBRE_MUNICIPIO")
            )
            "HABILIDAD" -> mapOf("ID_CATEGORIA_HABILIDAD" to FkReference("ID_CATEGORIA_HABILIDAD", "CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA"))
            "EMPRESA" -> mapOf(
                "ID_DISTRITO_DEPTO" to FkReference("ID_DISTRITO_DEPTO", "DEPARTAMENTO", "NOMBRE_DEPARTAMENTO"),
                "ID_DISTRITO_MUNICIPIO" to FkReference("ID_DISTRITO_MUNICIPIO", "MUNICIPIO", "NOMBRE_MUNICIPIO"),
                "ID_DISTRITO_ID" to FkReference("ID_DISTRITO_ID", "DISTRITO", "NOMBRE_DISTRITO")
            )
            "OFERTA_ACADEMICA" -> mapOf(
                "ID_INSTITUCION" to FkReference("ID_INSTITUCION", "INSTITUCION", "NOMBRE_INSTITUCION"),
                "ID_GRADO_ACADEMICO" to FkReference("ID_GRADO_ACADEMICO", "GRADO_ACADEMICO", "NOMBRE_GRADO")
            )
            "OFERTA_TRABAJO" -> mapOf(
                "NIT" to FkReference("NIT", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_GRADO_ACADEMICO" to FkReference("ID_GRADO_ACADEMICO", "GRADO_ACADEMICO", "NOMBRE_GRADO")
            )
            "CERTIFICACION" -> mapOf(
                "ID_INSTITUCION" to FkReference("ID_INSTITUCION", "INSTITUCION", "NOMBRE_INSTITUCION"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE"),
                "ID_TIPO_CERTIFICACION" to FkReference("ID_TIPO_CERTIFICACION", "TIPO_CERTIFICACION", "NOMBRE_TIPO")
            )
            "EXPERIENCIA_LABORAL" -> mapOf(
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE"),
                "NIT" to FkReference("NIT", "EMPRESA", "NOMBRE_EMPRESA")
            )
            "FORMACION_ACADEMICA" -> mapOf(
                "ID_OFERTA_ACADEMICA" to FkReference("ID_OFERTA_ACADEMICA", "OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE")
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "ID_CATEGORIA_HABILIDAD" to FkReference("ID_CATEGORIA_HABILIDAD", "CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA"),
                "ID_HABILIDAD" to FkReference("ID_HABILIDAD", "HABILIDAD", "NOMBRE_HABILIDAD"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE")
            )
            "POSTULACION" -> mapOf(
                "NIT" to FkReference("NIT", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_OFERTA" to FkReference("ID_OFERTA", "OFERTA_TRABAJO", "TITULO_PUESTO"),
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE")
            )
            "DETALLE_REQUISITO" -> mapOf(
                "NIT" to FkReference("NIT", "EMPRESA", "NOMBRE_EMPRESA"),
                "ID_OFERTA" to FkReference("ID_OFERTA", "OFERTA_TRABAJO", "TITULO_PUESTO")
            )
            "RED_SOCIAL_POSTULANTE" -> mapOf(
                "ID_POSTULANTE" to FkReference("ID_POSTULANTE", "POSTULANTE", "ID_POSTULANTE"),
                "ID_RED_SOCIAL" to FkReference("ID_RED_SOCIAL", "RED_SOCIAL", "NOMBRE_RED")
            )
            else -> emptyMap()
        }
    }

    fun getDropdownOptions(tableName: String, displayColumn: String, idColumn: String? = null): List<Pair<String, String>> {
        if (tableName == "ROL") {
            return listOf(
                Pair("postulante", "postulante"),
                Pair("gerente de empresa", "gerente de empresa"),
                Pair("administrador", "administrador")
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
                    val id = cursor.getString(0) ?: ""
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

        if (tableName == "POSTULANTE") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT ID_POSTULANTE, NOMBRE, APELLIDO FROM POSTULANTE",
                    null
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: ""
                    if (displayColumn == "ID_POSTULANTE") {
                        options.add(Pair(id, id))
                    } else {
                        val nombre = cursor.getString(1) ?: ""
                        val apellido = cursor.getString(2) ?: ""
                        val display = "$nombre $apellido".trim()
                        options.add(Pair(id, display.ifBlank { id }))
                    }
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (tableName == "HABILIDAD") {
            return try {
                val cursor = getDb().rawQuery(
                    "SELECT h.ID_HABILIDAD, h.NOMBRE_HABILIDAD, c.NOMBRE_CATEGORIA FROM HABILIDAD h " +
                    "LEFT JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD",
                    null
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: ""
                    val nombre = cursor.getString(1) ?: ""
                    val categoria = cursor.getString(2) ?: "Sin Categoria"
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
            val idCol = idColumn ?: getIdColumn(tableName)
            val cursor = getDb().rawQuery("SELECT * FROM $tableName", null)
            val actualIdColIndex = cursor.getColumnIndex(idCol)
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val id = if (actualIdColIndex >= 0) cursor.getString(actualIdColIndex) ?: "" else cursor.getString(0) ?: ""
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

    fun getFilteredOptions(childTable: String, childFkColumn: String, parentId: String, displayColumn: String? = null, includeExpired: Boolean = false): List<Pair<String, String>> {
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
                    val id = cursor.getString(0) ?: ""
                    val nombre = cursor.getString(1) ?: ""
                    val categoria = cursor.getString(2) ?: "Sin Categoria"
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
                    val id = cursor.getString(0) ?: ""
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
                    val id = cursor.getString(0) ?: ""
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

        if (childTable == "OFERTA_TRABAJO" && childFkColumn == "NIT") {
            return try {
                val expirationFilter = if (!includeExpired) " AND o.FECHA_CADUCIDAD >= date('now')" else ""
                val cursor = getDb().rawQuery(
                    "SELECT o.ID_OFERTA, o.TITULO_PUESTO, e.NOMBRE_EMPRESA FROM OFERTA_TRABAJO o " +
                    "LEFT JOIN EMPRESA e ON o.NIT = e.NIT " +
                    "WHERE o.NIT = ?$expirationFilter " +
                    "ORDER BY o.FECHA_PUBLICACION DESC",
                    arrayOf(parentId)
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: ""
                    val titulo = cursor.getString(1) ?: ""
                    val display = titulo.take(50)
                    options.add(Pair(id, display.ifBlank { id }))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (childTable == "DISTRITO" && childFkColumn == "ID_MUNICIPIO") {
            return try {
                val parts = parentId.split("|")
                val deptoId = parts.getOrElse(0) { "" }
                val munId = parts.getOrElse(1) { "" }
                val cursor = getDb().rawQuery(
                    "SELECT ID_DISTRITO, NOMBRE_DISTRITO FROM DISTRITO WHERE ID_DEPARTAMENTO = ? AND ID_MUNICIPIO = ? ORDER BY NOMBRE_DISTRITO",
                    arrayOf(deptoId, munId)
                )
                val options = mutableListOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: ""
                    val nombre = cursor.getString(1) ?: ""
                    options.add(Pair(id, nombre.ifBlank { id }))
                }
                cursor.close()
                options
            } catch (e: Exception) {
                emptyList()
            }
        }

        val idColumn = when (childTable) {
            "OFERTA_ACADEMICA" -> "ID_OFERTA_ACADEMICA"
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "HABILIDAD" -> "ID_HABILIDAD"
            "EMPRESA" -> "NIT"
            "POSTULANTE" -> "ID_POSTULANTE"
            "INSTITUCION" -> "ID_INSTITUCION"
            else -> getIdColumn(childTable)
        }
        
        return try {
            val cursor = getDb().rawQuery("SELECT * FROM $childTable WHERE $childFkColumn = ?", arrayOf(parentId))
            val options = mutableListOf<Pair<String, String>>()
            while (cursor.moveToNext()) {
                val idColIdx = cursor.getColumnIndex(idColumn)
                val id = if (idColIdx >= 0) cursor.getString(idColIdx) ?: "" else cursor.getString(0) ?: ""
                
                val name = if (displayColumn != null) {
                    val colIdx = cursor.getColumnIndex(displayColumn)
                    if (colIdx >= 0) cursor.getString(colIdx) ?: id else id
                } else {
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
            "EMPRESA" -> "NIT"
            "INSTITUCION" -> "ID_INSTITUCION"
            "GRADO_ACADEMICO" -> "ID_GRADO_ACADEMICO"
            "RED_SOCIAL" -> "ID_RED_SOCIAL"
            "OFERTA_ACADEMICA" -> "ID_OFERTA_ACADEMICA"
            "OFERTA_TRABAJO" -> "ID_OFERTA"
            "CERTIFICACION" -> "ID_CERTIFICACION"
            "EXPERIENCIA_LABORAL" -> "ID_EXPERIENCIA"
            "FORMACION_ACADEMICA" -> "ID_FORMACION"
            "HABILIDAD_POSTULANTE" -> "ID_HABILIDAD"
            "POSTULACION" -> "ID_POSTULACION"
            "DETALLE_REQUISITO" -> "ID_DETALLE"
            "RED_SOCIAL_POSTULANTE" -> "ID_RED_SOCIAL"
            "TIPO_CERTIFICACION" -> "ID_TIPO_CERTIFICACION"
            else -> "ID"
        }
    }

    fun getDepartamentoByMunicipio(deptoId: String, munId: String): String? {
        return try {
            val cursor = getDb().rawQuery(
                "SELECT ID_DEPARTAMENTO FROM MUNICIPIO WHERE ID_DEPARTAMENTO = ? AND ID_MUNICIPIO = ?",
                arrayOf(deptoId, munId)
            )
            val result = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    fun getMunicipioByDistrito(deptoId: String, munId: String, distritoId: String): String? {
        return try {
            val cursor = getDb().rawQuery(
                "SELECT ID_MUNICIPIO FROM DISTRITO WHERE ID_DEPARTAMENTO = ? AND ID_MUNICIPIO = ? AND ID_DISTRITO = ?",
                arrayOf(deptoId, munId, distritoId)
            )
            val result = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    fun getColumnsForTable(tableName: String): List<String> = Constants.getColumnsForTable(tableName)
}
