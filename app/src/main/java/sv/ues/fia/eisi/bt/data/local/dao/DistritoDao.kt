package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Distrito

class DistritoDao(private val db: ConnectionHelper) {

    fun getAll(): List<Distrito> {
        val results = db.executeQuery("SELECT * FROM DISTRITO ORDER BY NOMBRE_DISTRITO")
        return results.map { row ->
            Distrito(
                id_distrito = row[0] as Int,
                id_municipio = row[1] as Int,
                nombre_distrito = row[2] as String
            )
        }
    }

    fun getById(id: Int): Distrito? {
        val results = db.getById("DISTRITO", "ID_DISTRITO", id)
        return results.firstOrNull()?.let { row ->
            Distrito(
                id_distrito = row[0] as Int,
                id_municipio = row[1] as Int,
                nombre_distrito = row[2] as String
            )
        }
    }

    fun getByMunicipio(municipioId: Int): List<Distrito> {
        val results = db.executeQuery("SELECT * FROM DISTRITO WHERE ID_MUNICIPIO = $municipioId ORDER BY NOMBRE_DISTRITO")
        return results.map { row ->
            Distrito(
                id_distrito = row[0] as Int,
                id_municipio = row[1] as Int,
                nombre_distrito = row[2] as String
            )
        }
    }

    fun search(query: String): List<Distrito> {
        val results = db.search("DISTRITO", "NOMBRE_DISTRITO", query)
        return results.map { row ->
            Distrito(
                id_distrito = row[0] as Int,
                id_municipio = row[1] as Int,
                nombre_distrito = row[2] as String
            )
        }
    }

    fun insert(data: Distrito): Long {
        val query = "INSERT INTO DISTRITO (ID_DISTRITO, ID_MUNICIPIO, NOMBRE_DISTRITO) VALUES (${data.id_distrito}, ${data.id_municipio}, '${data.nombre_distrito}')"
        return db.executeInsert(query)
    }

    fun update(data: Distrito): Int {
        val query = "UPDATE DISTRITO SET ID_MUNICIPIO = ${data.id_municipio}, NOMBRE_DISTRITO = '${data.nombre_distrito}' WHERE ID_DISTRITO = ${data.id_distrito}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("DISTRITO", "ID_DISTRITO", id)

    fun getCount(): Int = db.getCount("DISTRITO")
}