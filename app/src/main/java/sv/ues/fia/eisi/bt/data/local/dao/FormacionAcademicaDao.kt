package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.FormacionAcademica

class FormacionAcademicaDao(private val db: ConnectionHelper) {

    fun getAll(): List<FormacionAcademica> {
        val results = db.executeQuery("SELECT * FROM FORMACION_ACADEMICA ORDER BY FECHA_OBTENCION DESC")
        return results.map { row ->
            FormacionAcademica(
                id_formacion = row[0] as Int,
                id_postulante = row[1] as Int,
                id_oferta_academica = row[2] as? Int,
                titulo_obtenido = row[3] as String,
                fecha_obtencion = row[4] as? String
            )
        }
    }

    fun getById(id: Int): FormacionAcademica? {
        val results = db.getById("FORMACION_ACADEMICA", "ID_FORMACION", id)
        return results.firstOrNull()?.let { row ->
            FormacionAcademica(
                id_formacion = row[0] as Int,
                id_postulante = row[1] as Int,
                id_oferta_academica = row[2] as? Int,
                titulo_obtenido = row[3] as String,
                fecha_obtencion = row[4] as? String
            )
        }
    }

    fun getByPostulante(postulanteId: Int): List<FormacionAcademica> {
        val results = db.executeQuery("SELECT * FROM FORMACION_ACADEMICA WHERE ID_POSTULANTE = $postulanteId ORDER BY FECHA_OBTENCION DESC")
        return results.map { row ->
            FormacionAcademica(
                id_formacion = row[0] as Int,
                id_postulante = row[1] as Int,
                id_oferta_academica = row[2] as? Int,
                titulo_obtenido = row[3] as String,
                fecha_obtencion = row[4] as? String
            )
        }
    }

    fun search(query: String): List<FormacionAcademica> {
        val results = db.search("FORMACION_ACADEMICA", "TITULO_OBTENIDO", query)
        return results.map { row ->
            FormacionAcademica(
                id_formacion = row[0] as Int,
                id_postulante = row[1] as Int,
                id_oferta_academica = row[2] as? Int,
                titulo_obtenido = row[3] as String,
                fecha_obtencion = row[4] as? String
            )
        }
    }

    fun insert(data: FormacionAcademica): Long {
        val ofertaId = data.id_oferta_academica ?: "NULL"
        val fecha = if (data.fecha_obtencion != null) "'${data.fecha_obtencion}'" else "NULL"
        val query = "INSERT INTO FORMACION_ACADEMICA (ID_FORMACION, ID_POSTULANTE, ID_OFERTA_ACADEMICA, TITULO_OBTENIDO, FECHA_OBTENCION) VALUES (${data.id_formacion}, ${data.id_postulante}, $ofertaId, '${data.titulo_obtenido}', $fecha)"
        return db.executeInsert(query)
    }

    fun update(data: FormacionAcademica): Int {
        val ofertaId = data.id_oferta_academica ?: "NULL"
        val fecha = if (data.fecha_obtencion != null) "'${data.fecha_obtencion}'" else "NULL"
        val query = "UPDATE FORMACION_ACADEMICA SET ID_POSTULANTE = ${data.id_postulante}, ID_OFERTA_ACADEMICA = $ofertaId, TITULO_OBTENIDO = '${data.titulo_obtenido}', FECHA_OBTENCION = $fecha WHERE ID_FORMACION = ${data.id_formacion}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("FORMACION_ACADEMICA", "ID_FORMACION", id)

    fun getCount(): Int = db.getCount("FORMACION_ACADEMICA")
}