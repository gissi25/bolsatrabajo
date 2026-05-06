package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.CategoriaHabilidad

class CategoriaHabilidadDao(private val db: ConnectionHelper) {

    fun getAll(): List<CategoriaHabilidad> {
        val results = db.executeQuery("SELECT * FROM CATEGORIA_HABILIDAD ORDER BY NOMBRE_CATEGORIA")
        return results.map { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
                nombre_categoria = row[1].toString()
            )
        }
    }

    fun getById(id: Int): CategoriaHabilidad? {
        val results = db.getById("CATEGORIA_HABILIDAD", "ID_CATEGORIA_HABILIDAD", id.toString())
        return results.firstOrNull()?.let { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
                nombre_categoria = row[1].toString()
            )
        }
    }

    fun search(query: String): List<CategoriaHabilidad> {
        val results = db.search("CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA", query)
        return results.map { row ->
            CategoriaHabilidad(
                id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
                nombre_categoria = row[1].toString()
            )
        }
    }

    fun insert(data: CategoriaHabilidad): Long {
        val query = "INSERT INTO CATEGORIA_HABILIDAD (NOMBRE_CATEGORIA) VALUES ('${data.nombre_categoria}')"
        return db.executeInsert(query)
    }

    fun update(data: CategoriaHabilidad): Int {
        val query = "UPDATE CATEGORIA_HABILIDAD SET NOMBRE_CATEGORIA = '${data.nombre_categoria}' WHERE ID_CATEGORIA_HABILIDAD = ${data.id_categoria_habilidad}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("CATEGORIA_HABILIDAD", "ID_CATEGORIA_HABILIDAD", id.toString())

    fun getCount(): Int = db.getCount("CATEGORIA_HABILIDAD")
}
