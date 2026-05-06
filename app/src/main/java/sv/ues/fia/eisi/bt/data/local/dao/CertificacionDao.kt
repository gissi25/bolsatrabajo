package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Certificacion

class CertificacionDao(private val db: ConnectionHelper) {

    fun getAll(): List<Certificacion> {
        val results = db.executeQuery("SELECT * FROM CERTIFICACION ORDER BY NOMBRE_CERTIFICACION")
        return results.map { row ->
            Certificacion(
                id_certificacion = row[0].toString(),
                id_institucion = row[1].toString(),
                id_postulante = row[2].toString(),
                nombre_certificacion = row[3].toString(),
                fecha_certificacion = row[4].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByPostulante(postulanteId: String): List<Certificacion> {
        val results = db.executeQuery("SELECT * FROM CERTIFICACION WHERE ID_POSTULANTE = '$postulanteId' ORDER BY NOMBRE_CERTIFICACION")
        return results.map { row ->
            Certificacion(
                id_certificacion = row[0].toString(),
                id_institucion = row[1].toString(),
                id_postulante = row[2].toString(),
                nombre_certificacion = row[3].toString(),
                fecha_certificacion = row[4].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<Certificacion> {
        val results = db.search("CERTIFICACION", "NOMBRE_CERTIFICACION", query)
        return results.map { row ->
            Certificacion(
                id_certificacion = row[0].toString(),
                id_institucion = row[1].toString(),
                id_postulante = row[2].toString(),
                nombre_certificacion = row[3].toString(),
                fecha_certificacion = row[4].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: Certificacion): Long {
        val fecha = if (data.fecha_certificacion != null) "'${data.fecha_certificacion}'" else "NULL"
        val query = "INSERT INTO CERTIFICACION (ID_CERTIFICACION, ID_INSTITUCION, ID_POSTULANTE, NOMBRE_CERTIFICACION, FECHA_CERTIFICACION) VALUES ('${data.id_certificacion}', '${data.id_institucion}', '${data.id_postulante}', '${data.nombre_certificacion}', $fecha)"
        return db.executeInsert(query)
    }

    fun update(data: Certificacion): Int {
        val fecha = if (data.fecha_certificacion != null) "'${data.fecha_certificacion}'" else "NULL"
        val query = "UPDATE CERTIFICACION SET ID_INSTITUCION = '${data.id_institucion}', NOMBRE_CERTIFICACION = '${data.nombre_certificacion}', FECHA_CERTIFICACION = $fecha WHERE ID_CERTIFICACION = '${data.id_certificacion}' AND ID_INSTITUCION = '${data.id_institucion}' AND ID_POSTULANTE = '${data.id_postulante}'"
        return db.executeUpdate(query)
    }

    fun delete(certId: String, institucionId: String, postulanteId: String): Int {
        return db.executeDelete("DELETE FROM CERTIFICACION WHERE ID_CERTIFICACION = '$certId' AND ID_INSTITUCION = '$institucionId' AND ID_POSTULANTE = '$postulanteId'")
    }

    fun getCount(): Int = db.getCount("CERTIFICACION")
}
