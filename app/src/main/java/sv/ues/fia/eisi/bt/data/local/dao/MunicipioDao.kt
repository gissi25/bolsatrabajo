package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Municipio

class MunicipioDao(private val db: ConnectionHelper) {

    fun getAll(): List<Municipio> {
        val results = db.executeQuery("SELECT * FROM MUNICIPIO ORDER BY NOMBRE_MUNICIPIO")
        return results.map { row ->
            Municipio(
                id_municipio = row[0] as Int,
                id_departamento = row[1] as Int,
                nombre_municipio = row[2] as String
            )
        }
    }

    fun getById(id: Int): Municipio? {
        val results = db.getById("MUNICIPIO", "ID_MUNICIPIO", id)
        return results.firstOrNull()?.let { row ->
            Municipio(
                id_municipio = row[0] as Int,
                id_departamento = row[1] as Int,
                nombre_municipio = row[2] as String
            )
        }
    }

    fun getByDepartamento(departamentoId: Int): List<Municipio> {
        val results = db.executeQuery("SELECT * FROM MUNICIPIO WHERE ID_DEPARTAMENTO = $departamentoId ORDER BY NOMBRE_MUNICIPIO")
        return results.map { row ->
            Municipio(
                id_municipio = row[0] as Int,
                id_departamento = row[1] as Int,
                nombre_municipio = row[2] as String
            )
        }
    }

    fun search(query: String): List<Municipio> {
        val results = db.search("MUNICIPIO", "NOMBRE_MUNICIPIO", query)
        return results.map { row ->
            Municipio(
                id_municipio = row[0] as Int,
                id_departamento = row[1] as Int,
                nombre_municipio = row[2] as String
            )
        }
    }

    fun insert(data: Municipio): Long {
        val query = "INSERT INTO MUNICIPIO (ID_MUNICIPIO, ID_DEPARTAMENTO, NOMBRE_MUNICIPIO) VALUES (${data.id_municipio}, ${data.id_departamento}, '${data.nombre_municipio}')"
        return db.executeInsert(query)
    }

    fun update(data: Municipio): Int {
        val query = "UPDATE MUNICIPIO SET ID_DEPARTAMENTO = ${data.id_departamento}, NOMBRE_MUNICIPIO = '${data.nombre_municipio}' WHERE ID_MUNICIPIO = ${data.id_municipio}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("MUNICIPIO", "ID_MUNICIPIO", id)

    fun getCount(): Int = db.getCount("MUNICIPIO")
}