package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Distrito

class DistritoDao(private val db: ConnectionHelper) {

    fun getAll(): List<Distrito> {
        val results = db.executeQuery("SELECT * FROM DISTRITO ORDER BY NOMBRE_DISTRITO")
        return results.map { row ->
            Distrito(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                id_distrito = row[2].toString().toIntOrNull() ?: 0,
                nombre_distrito = row[3].toString()
            )
        }
    }

    fun getById(deptoId: Int, munId: Int, distId: Int): Distrito? {
        val results = db.executeQuery("SELECT * FROM DISTRITO WHERE ID_DEPARTAMENTO = $deptoId AND ID_MUNICIPIO = $munId AND ID_DISTRITO = $distId")
        return results.firstOrNull()?.let { row ->
            Distrito(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                id_distrito = row[2].toString().toIntOrNull() ?: 0,
                nombre_distrito = row[3].toString()
            )
        }
    }

    fun getByMunicipio(deptoId: Int, municipioId: Int): List<Distrito> {
        val results = db.executeQuery("SELECT * FROM DISTRITO WHERE ID_DEPARTAMENTO = $deptoId AND ID_MUNICIPIO = $municipioId ORDER BY NOMBRE_DISTRITO")
        return results.map { row ->
            Distrito(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                id_distrito = row[2].toString().toIntOrNull() ?: 0,
                nombre_distrito = row[3].toString()
            )
        }
    }

    fun search(query: String): List<Distrito> {
        val results = db.search("DISTRITO", "NOMBRE_DISTRITO", query)
        return results.map { row ->
            Distrito(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                id_distrito = row[2].toString().toIntOrNull() ?: 0,
                nombre_distrito = row[3].toString()
            )
        }
    }

    fun insert(data: Distrito): Long {
        val query = "INSERT INTO DISTRITO (ID_DEPARTAMENTO, ID_MUNICIPIO, ID_DISTRITO, NOMBRE_DISTRITO) VALUES (${data.id_departamento}, ${data.id_municipio}, ${data.id_distrito}, '${data.nombre_distrito}')"
        return db.executeInsert(query)
    }

    fun update(data: Distrito): Int {
        val query = "UPDATE DISTRITO SET NOMBRE_DISTRITO = '${data.nombre_distrito}' WHERE ID_DEPARTAMENTO = ${data.id_departamento} AND ID_MUNICIPIO = ${data.id_municipio} AND ID_DISTRITO = ${data.id_distrito}"
        return db.executeUpdate(query)
    }

    fun delete(deptoId: Int, munId: Int, distId: Int): Int {
        return db.executeDelete("DELETE FROM DISTRITO WHERE ID_DEPARTAMENTO = $deptoId AND ID_MUNICIPIO = $munId AND ID_DISTRITO = $distId")
    }

    fun getCount(): Int = db.getCount("DISTRITO")
}
