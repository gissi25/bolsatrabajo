package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Postulacion

class PostulacionDao(private val db: ConnectionHelper) {

    fun getAll(): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_postulacion = row[0].toString(),
                nit = row[1].toString(),
                id_oferta = row[2].toString(),
                id_postulante = row[3].toString(),
                fecha_aplicacion = row[4].toString().takeIf { it.isNotBlank() },
                estado_proceso = row[5].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getById(postulacionId: String): Postulacion? {
        val results = db.getById("POSTULACION", "ID_POSTULACION", postulacionId)
        return results.firstOrNull()?.let { row ->
            Postulacion(
                id_postulacion = row[0].toString(),
                nit = row[1].toString(),
                id_oferta = row[2].toString(),
                id_postulante = row[3].toString(),
                fecha_aplicacion = row[4].toString().takeIf { it.isNotBlank() },
                estado_proceso = row[5].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByPostulante(postulanteId: String): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION WHERE ID_POSTULANTE = '$postulanteId' ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_postulacion = row[0].toString(),
                nit = row[1].toString(),
                id_oferta = row[2].toString(),
                id_postulante = row[3].toString(),
                fecha_aplicacion = row[4].toString().takeIf { it.isNotBlank() },
                estado_proceso = row[5].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByOferta(nit: String, ofertaId: String): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION WHERE NIT = '$nit' AND ID_OFERTA = '$ofertaId' ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_postulacion = row[0].toString(),
                nit = row[1].toString(),
                id_oferta = row[2].toString(),
                id_postulante = row[3].toString(),
                fecha_aplicacion = row[4].toString().takeIf { it.isNotBlank() },
                estado_proceso = row[5].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<Postulacion> {
        val results = db.search("POSTULACION", "ESTADO_PROCESO", query)
        return results.map { row ->
            Postulacion(
                id_postulacion = row[0].toString(),
                nit = row[1].toString(),
                id_oferta = row[2].toString(),
                id_postulante = row[3].toString(),
                fecha_aplicacion = row[4].toString().takeIf { it.isNotBlank() },
                estado_proceso = row[5].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: Postulacion): Long {
        val fecha = if (data.fecha_aplicacion != null) "'${data.fecha_aplicacion}'" else "NULL"
        val estado = if (data.estado_proceso != null) "'${data.estado_proceso}'" else "NULL"
        val query = "INSERT INTO POSTULACION (ID_POSTULACION, NIT, ID_OFERTA, ID_POSTULANTE, FECHA_APLICACION, ESTADO_PROCESO) VALUES ('${data.id_postulacion}', '${data.nit}', '${data.id_oferta}', '${data.id_postulante}', $fecha, $estado)"
        return db.executeInsert(query)
    }

    fun update(data: Postulacion): Int {
        val fecha = if (data.fecha_aplicacion != null) "'${data.fecha_aplicacion}'" else "NULL"
        val estado = if (data.estado_proceso != null) "'${data.estado_proceso}'" else "NULL"
        val query = "UPDATE POSTULACION SET FECHA_APLICACION = $fecha, ESTADO_PROCESO = $estado WHERE ID_POSTULACION = '${data.id_postulacion}'"
        return db.executeUpdate(query)
    }

    fun delete(postulacionId: String): Int {
        return db.executeDelete("DELETE FROM POSTULACION WHERE ID_POSTULACION = '$postulacionId'")
    }

    fun getCount(): Int = db.getCount("POSTULACION")
}
