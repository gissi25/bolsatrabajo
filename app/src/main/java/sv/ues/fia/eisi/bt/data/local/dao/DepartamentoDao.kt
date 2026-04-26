package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Departamento

class DepartamentoDao(private val db: ConnectionHelper) {

    fun getAll(): List<Departamento> {
        val results = db.executeQuery("SELECT * FROM DEPARTAMENTO ORDER BY NOMBRE_DEPARTAMENTO")
        return results.map { row ->
            Departamento(
                id_departamento = row[0] as Int,
                nombre_departamento = row[1] as String
            )
        }
    }

    fun getById(id: Int): Departamento? {
        val results = db.getById("DEPARTAMENTO", "ID_DEPARTAMENTO", id)
        return results.firstOrNull()?.let { row ->
            Departamento(
                id_departamento = row[0] as Int,
                nombre_departamento = row[1] as String
            )
        }
    }

    fun search(query: String): List<Departamento> {
        val results = db.search("DEPARTAMENTO", "NOMBRE_DEPARTAMENTO", query)
        return results.map { row ->
            Departamento(
                id_departamento = row[0] as Int,
                nombre_departamento = row[1] as String
            )
        }
    }

    fun insert(data: Departamento): Long {
        val query = "INSERT INTO DEPARTAMENTO (ID_DEPARTAMENTO, NOMBRE_DEPARTAMENTO) VALUES (${data.id_departamento}, '${data.nombre_departamento}')"
        return db.executeInsert(query)
    }

    fun update(data: Departamento): Int {
        val query = "UPDATE DEPARTAMENTO SET NOMBRE_DEPARTAMENTO = '${data.nombre_departamento}' WHERE ID_DEPARTAMENTO = ${data.id_departamento}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("DEPARTAMENTO", "ID_DEPARTAMENTO", id)

    fun getCount(): Int = db.getCount("DEPARTAMENTO")
}