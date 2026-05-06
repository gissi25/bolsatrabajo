package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Habilidad

class HabilidadDao(private val db: ConnectionHelper) {

    private fun mapRowToHabilidad(row: List<Any>): Habilidad {
        return Habilidad(
            id_habilidad = row[1].toString(),
            id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
            nombre_habilidad = row[2].toString(),
            nombre_categoria = if (row.size > 3) row[3].toString() else null
        )
    }

    fun getAll(): List<Habilidad> {
        val sql = """
            SELECT h.*, c.NOMBRE_CATEGORIA 
            FROM HABILIDAD h
            INNER JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
            ORDER BY h.NOMBRE_HABILIDAD
        """.trimIndent()
        return db.executeQuery(sql).map { mapRowToHabilidad(it) }
    }

    fun getById(categoriaId: Int, habilidadId: String): Habilidad? {
        val sql = """
            SELECT h.*, c.NOMBRE_CATEGORIA 
            FROM HABILIDAD h
            INNER JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
            WHERE h.ID_CATEGORIA_HABILIDAD = $categoriaId AND h.ID_HABILIDAD = '$habilidadId'
        """.trimIndent()
        return db.executeQuery(sql).firstOrNull()?.let { mapRowToHabilidad(it) }
    }

    fun getByCategoria(categoriaId: Int): List<Habilidad> {
        val sql = """
            SELECT h.*, c.NOMBRE_CATEGORIA 
            FROM HABILIDAD h
            INNER JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
            WHERE h.ID_CATEGORIA_HABILIDAD = $categoriaId
            ORDER BY h.NOMBRE_HABILIDAD
        """.trimIndent()
        return db.executeQuery(sql).map { mapRowToHabilidad(it) }
    }

    fun search(query: String): List<Habilidad> {
        val sql = """
            SELECT h.*, c.NOMBRE_CATEGORIA 
            FROM HABILIDAD h
            INNER JOIN CATEGORIA_HABILIDAD c ON h.ID_CATEGORIA_HABILIDAD = c.ID_CATEGORIA_HABILIDAD
            WHERE h.NOMBRE_HABILIDAD LIKE '%$query%'
            ORDER BY h.NOMBRE_HABILIDAD
        """.trimIndent()
        return db.executeQuery(sql).map { mapRowToHabilidad(it) }
    }

    fun insert(data: Habilidad): Long {
        val query = "INSERT INTO HABILIDAD (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, NOMBRE_HABILIDAD) VALUES (${data.id_categoria_habilidad}, '${data.id_habilidad}', '${data.nombre_habilidad}')"
        return db.executeInsert(query)
    }

    fun update(data: Habilidad): Int {
        val query = "UPDATE HABILIDAD SET NOMBRE_HABILIDAD = '${data.nombre_habilidad}' WHERE ID_CATEGORIA_HABILIDAD = ${data.id_categoria_habilidad} AND ID_HABILIDAD = '${data.id_habilidad}'"
        return db.executeUpdate(query)
    }

    fun delete(categoriaId: Int, habilidadId: String): Int {
        return db.executeDelete("DELETE FROM HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = $categoriaId AND ID_HABILIDAD = '$habilidadId'")
    }

    fun getCount(): Int = db.getCount("HABILIDAD")
}
