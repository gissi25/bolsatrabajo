package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Habilidad

class HabilidadDao(private val db: ConnectionHelper) {

    fun getAll(): List<Habilidad> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD ORDER BY NOMBRE_HABILIDAD")
        return results.map { row ->
            Habilidad(
                id_habilidad = row[0] as Int,
                id_categoria_habilidad = row[1] as Int,
                nombre_habilidad = row[2] as String
            )
        }
    }

    fun getById(id: Int): Habilidad? {
        val results = db.getById("HABILIDAD", "ID_HABILIDAD", id)
        return results.firstOrNull()?.let { row ->
            Habilidad(
                id_habilidad = row[0] as Int,
                id_categoria_habilidad = row[1] as Int,
                nombre_habilidad = row[2] as String
            )
        }
    }

    fun getByCategoria(categoriaId: Int): List<Habilidad> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD WHERE ID_CATEGORIA_HABILIDAD = $categoriaId ORDER BY NOMBRE_HABILIDAD")
        return results.map { row ->
            Habilidad(
                id_habilidad = row[0] as Int,
                id_categoria_habilidad = row[1] as Int,
                nombre_habilidad = row[2] as String
            )
        }
    }

    fun search(query: String): List<Habilidad> {
        val results = db.search("HABILIDAD", "NOMBRE_HABILIDAD", query)
        return results.map { row ->
            Habilidad(
                id_habilidad = row[0] as Int,
                id_categoria_habilidad = row[1] as Int,
                nombre_habilidad = row[2] as String
            )
        }
    }

    fun insert(data: Habilidad): Long {
        val query = "INSERT INTO HABILIDAD (ID_HABILIDAD, ID_CATEGORIA_HABILIDAD, NOMBRE_HABILIDAD) VALUES (${data.id_habilidad}, ${data.id_categoria_habilidad}, '${data.nombre_habilidad}')"
        return db.executeInsert(query)
    }

    fun update(data: Habilidad): Int {
        val query = "UPDATE HABILIDAD SET ID_CATEGORIA_HABILIDAD = ${data.id_categoria_habilidad}, NOMBRE_HABILIDAD = '${data.nombre_habilidad}' WHERE ID_HABILIDAD = ${data.id_habilidad}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("HABILIDAD", "ID_HABILIDAD", id)

    fun getCount(): Int = db.getCount("HABILIDAD")
}