package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.GradoAcademico

class GradoAcademicoDao(private val db: ConnectionHelper) {

    fun getAll(): List<GradoAcademico> {
        val results = db.executeQuery("SELECT * FROM GRADO_ACADEMICO ORDER BY NOMBRE_GRADO")
        return results.map { row ->
            GradoAcademico(
                id_grado_academico = row[0].toString().toIntOrNull() ?: 0,
                nombre_grado = row[1].toString()
            )
        }
    }

    fun getById(id: Int): GradoAcademico? {
        val results = db.getById("GRADO_ACADEMICO", "ID_GRADO_ACADEMICO", id.toString())
        return results.firstOrNull()?.let { row ->
            GradoAcademico(
                id_grado_academico = row[0].toString().toIntOrNull() ?: 0,
                nombre_grado = row[1].toString()
            )
        }
    }

    fun search(query: String): List<GradoAcademico> {
        val results = db.search("GRADO_ACADEMICO", "NOMBRE_GRADO", query)
        return results.map { row ->
            GradoAcademico(
                id_grado_academico = row[0].toString().toIntOrNull() ?: 0,
                nombre_grado = row[1].toString()
            )
        }
    }

    fun insert(data: GradoAcademico): Long {
        val query = "INSERT INTO GRADO_ACADEMICO (NOMBRE_GRADO) VALUES ('${data.nombre_grado}')"
        return db.executeInsert(query)
    }

    fun update(data: GradoAcademico): Int {
        val query = "UPDATE GRADO_ACADEMICO SET NOMBRE_GRADO = '${data.nombre_grado}' WHERE ID_GRADO_ACADEMICO = ${data.id_grado_academico}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("GRADO_ACADEMICO", "ID_GRADO_ACADEMICO", id.toString())

    fun getCount(): Int = db.getCount("GRADO_ACADEMICO")
}
