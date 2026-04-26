package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Certificacion

class CertificacionDao(private val db: ConnectionHelper) {

    fun getAll(): List<Certificacion> {
        val results = db.executeQuery("SELECT * FROM CERTIFICACION ORDER BY NOMBRE_CERTIFICACION")
        return results.map { row ->
            Certificacion(
                id_postulante = row[0] as Int,
                id_certificacion = row[1] as Int,
                id_institucion = row[2] as Int,
                nombre_certificacion = row[3] as String,
                codigo_certificacion = row[4] as? String,
                fecha_certificacion = row[5] as? String
            )
        }
    }

    fun getById(postulanteId: Int, certificacionId: Int): Certificacion? {
        val results = db.executeQuery("SELECT * FROM CERTIFICACION WHERE ID_POSTULANTE = $postulanteId AND ID_CERTIFICACION = $certificacionId")
        return results.firstOrNull()?.let { row ->
            Certificacion(
                id_postulante = row[0] as Int,
                id_certificacion = row[1] as Int,
                id_institucion = row[2] as Int,
                nombre_certificacion = row[3] as String,
                codigo_certificacion = row[4] as? String,
                fecha_certificacion = row[5] as? String
            )
        }
    }

    fun getByPostulante(postulanteId: Int): List<Certificacion> {
        val results = db.executeQuery("SELECT * FROM CERTIFICACION WHERE ID_POSTULANTE = $postulanteId ORDER BY NOMBRE_CERTIFICACION")
        return results.map { row ->
            Certificacion(
                id_postulante = row[0] as Int,
                id_certificacion = row[1] as Int,
                id_institucion = row[2] as Int,
                nombre_certificacion = row[3] as String,
                codigo_certificacion = row[4] as? String,
                fecha_certificacion = row[5] as? String
            )
        }
    }

    fun search(query: String): List<Certificacion> {
        val results = db.search("CERTIFICACION", "NOMBRE_CERTIFICACION", query)
        return results.map { row ->
            Certificacion(
                id_postulante = row[0] as Int,
                id_certificacion = row[1] as Int,
                id_institucion = row[2] as Int,
                nombre_certificacion = row[3] as String,
                codigo_certificacion = row[4] as? String,
                fecha_certificacion = row[5] as? String
            )
        }
    }

    fun insert(data: Certificacion): Long {
        val codigo = if (data.codigo_certificacion != null) "'${data.codigo_certificacion}'" else "NULL"
        val fecha = if (data.fecha_certificacion != null) "'${data.fecha_certificacion}'" else "NULL"
        val query = "INSERT INTO CERTIFICACION (ID_POSTULANTE, ID_CERTIFICACION, ID_INSTITUCION, NOMBRE_CERTIFICACION, CODIGO_CERTIFICACION, FECHA_CERTIFICACION) VALUES (${data.id_postulante}, ${data.id_certificacion}, ${data.id_institucion}, '${data.nombre_certificacion}', $codigo, $fecha)"
        return db.executeInsert(query)
    }

    fun update(data: Certificacion): Int {
        val codigo = if (data.codigo_certificacion != null) "'${data.codigo_certificacion}'" else "NULL"
        val fecha = if (data.fecha_certificacion != null) "'${data.fecha_certificacion}'" else "NULL"
        val query = "UPDATE CERTIFICACION SET ID_INSTITUCION = ${data.id_institucion}, NOMBRE_CERTIFICACION = '${data.nombre_certificacion}', CODIGO_CERTIFICACION = $codigo, FECHA_CERTIFICACION = $fecha WHERE ID_POSTULANTE = ${data.id_postulante} AND ID_CERTIFICACION = ${data.id_certificacion}"
        return db.executeUpdate(query)
    }

    fun delete(postulanteId: Int, certificacionId: Int): Int {
        return db.executeDelete("DELETE FROM CERTIFICACION WHERE ID_POSTULANTE = $postulanteId AND ID_CERTIFICACION = $certificacionId")
    }

    fun getCount(): Int = db.getCount("CERTIFICACION")
}