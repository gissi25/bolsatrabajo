package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.CategoriaHabilidad

class CategoriaHabilidadDao(private val db: ConnectionHelper) {

    fun getAll(): List<CategoriaHabilidad> {
        val results = db.executeQuery("SELECT * FROM CATEGORIA_HABILIDAD ORDER BY NOMBRE_CATEGORIA")
        return results.map { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0] as Int,
                nombre_categoria = row[1] as String
            )
        }
    }

    fun getById(id: Int): CategoriaHabilidad? {
        val results = db.getById("CATEGORIA_HABILIDAD", "ID_CATEGORIA_HABILIDAD", id)
        return results.firstOrNull()?.let { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0] as Int,
                nombre_categoria = row[1] as String
            )
        }
    }

    fun search(query: String): List<CategoriaHabilidad> {
        val results = db.search("CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA", query)
        return results.map { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0] as Int,
                nombre_categoria = row[1] as String
            )
        }
    }

    fun insert(data: CategoriaHabilidad): Long {
        val query = """
            INSERT INTO CATEGORIA_HABILIDAD (ID_CATEGORIA_HABILIDAD, NOMBRE_CATEGORIA)
            VALUES (${data.id_categoria_habilidad}, '${data.nombre_categoria}')
        """.trimIndent()
        return db.executeInsert(query)
    }

    fun update(data: CategoriaHabilidad): Int {
        val query = """
            UPDATE CATEGORIA_HABILIDAD SET
            NOMBRE_CATEGORIA = '${data.nombre_categoria}'
            WHERE ID_CATEGORIA_HABILIDAD = ${data.id_categoria_habilidad}
        """.trimIndent()
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int {
        return db.deleteById("CATEGORIA_HABILIDAD", "ID_CATEGORIA_HABILIDAD", id)
    }

    fun getCount(): Int = db.getCount("CATEGORIA_HABILIDAD")
}