package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.OfertaAcademica

class OfertaAcademicaDao(private val db: ConnectionHelper) {

    fun getAll(): List<OfertaAcademica> {
        val results = db.executeQuery("SELECT * FROM OFERTA_ACADEMICA")
        return results.map { row ->
            OfertaAcademica(
                id_oferta_academica = row[0] as Int,
                id_institucion = row[1] as? Int,
                id_grado_academico = row[2] as? Int
            )
        }
    }

    fun getById(id: Int): OfertaAcademica? {
        val results = db.getById("OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA", id)
        return results.firstOrNull()?.let { row ->
            OfertaAcademica(
                id_oferta_academica = row[0] as Int,
                id_institucion = row[1] as? Int,
                id_grado_academico = row[2] as? Int
            )
        }
    }

    fun insert(data: OfertaAcademica): Long {
        val instId = data.id_institucion ?: "NULL"
        val gradoId = data.id_grado_academico ?: "NULL"
        val query = "INSERT INTO OFERTA_ACADEMICA (ID_OFERTA_ACADEMICA, ID_INSTITUCION, ID_GRADO_ACADEMICO) VALUES (${data.id_oferta_academica}, $instId, $gradoId)"
        return db.executeInsert(query)
    }

    fun update(data: OfertaAcademica): Int {
        val instId = data.id_institucion ?: "NULL"
        val gradoId = data.id_grado_academico ?: "NULL"
        val query = "UPDATE OFERTA_ACADEMICA SET ID_INSTITUCION = $instId, ID_GRADO_ACADEMICO = $gradoId WHERE ID_OFERTA_ACADEMICA = ${data.id_oferta_academica}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("OFERTA_ACADEMICA", "ID_OFERTA_ACADEMICA", id)

    fun getCount(): Int = db.getCount("OFERTA_ACADEMICA")
}