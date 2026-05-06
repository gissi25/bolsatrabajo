package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Municipio

class MunicipioDao(private val db: ConnectionHelper) {

    fun getAll(): List<Municipio> {
        val results = db.executeQuery("SELECT * FROM MUNICIPIO ORDER BY NOMBRE_MUNICIPIO")
        return results.map { row ->
            Municipio(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                nombre_municipio = row[2].toString()
            )
        }
    }

    fun getById(deptoId: Int, munId: Int): Municipio? {
        val results = db.executeQuery("SELECT * FROM MUNICIPIO WHERE ID_DEPARTAMENTO = $deptoId AND ID_MUNICIPIO = $munId")
        return results.firstOrNull()?.let { row ->
            Municipio(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                nombre_municipio = row[2].toString()
            )
        }
    }

    fun getByDepartamento(departamentoId: Int): List<Municipio> {
        val results = db.executeQuery("SELECT * FROM MUNICIPIO WHERE ID_DEPARTAMENTO = $departamentoId ORDER BY NOMBRE_MUNICIPIO")
        return results.map { row ->
            Municipio(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                nombre_municipio = row[2].toString()
            )
        }
    }

    fun search(query: String): List<Municipio> {
        val results = db.search("MUNICIPIO", "NOMBRE_MUNICIPIO", query)
        return results.map { row ->
            Municipio(
                id_departamento = row[0].toString().toIntOrNull() ?: 0,
                id_municipio = row[1].toString().toIntOrNull() ?: 0,
                nombre_municipio = row[2].toString()
            )
        }
    }

    fun insert(data: Municipio): Long {
        val query = "INSERT INTO MUNICIPIO (ID_DEPARTAMENTO, ID_MUNICIPIO, NOMBRE_MUNICIPIO) VALUES (${data.id_departamento}, ${data.id_municipio}, '${data.nombre_municipio}')"
        return db.executeInsert(query)
    }

    fun update(data: Municipio): Int {
        val query = "UPDATE MUNICIPIO SET NOMBRE_MUNICIPIO = '${data.nombre_municipio}' WHERE ID_DEPARTAMENTO = ${data.id_departamento} AND ID_MUNICIPIO = ${data.id_municipio}"
        return db.executeUpdate(query)
    }

    fun delete(deptoId: Int, munId: Int): Int {
        return db.executeDelete("DELETE FROM MUNICIPIO WHERE ID_DEPARTAMENTO = $deptoId AND ID_MUNICIPIO = $munId")
    }

    fun getCount(): Int = db.getCount("MUNICIPIO")
}
