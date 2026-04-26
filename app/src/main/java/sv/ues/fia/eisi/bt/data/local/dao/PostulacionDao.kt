package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Postulacion

class PostulacionDao(private val db: ConnectionHelper) {

    fun getAll(): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_postulante = row[2] as Int,
                id_postulacion = row[3] as Int,
                fecha_aplicacion = row[4] as? String,
                estado_proceso = row[5] as? String
            )
        }
    }

    fun getById(empresaId: Int, ofertaId: Int, postulanteId: Int, postulacionId: Int): Postulacion? {
        val results = db.executeQuery("SELECT * FROM POSTULACION WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId AND ID_POSTULANTE = $postulanteId AND ID_POSTULACION = $postulacionId")
        return results.firstOrNull()?.let { row ->
            Postulacion(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_postulante = row[2] as Int,
                id_postulacion = row[3] as Int,
                fecha_aplicacion = row[4] as? String,
                estado_proceso = row[5] as? String
            )
        }
    }

    fun getByPostulante(postulanteId: Int): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION WHERE ID_POSTULANTE = $postulanteId ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_postulante = row[2] as Int,
                id_postulacion = row[3] as Int,
                fecha_aplicacion = row[4] as? String,
                estado_proceso = row[5] as? String
            )
        }
    }

    fun getByOferta(empresaId: Int, ofertaId: Int): List<Postulacion> {
        val results = db.executeQuery("SELECT * FROM POSTULACION WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId ORDER BY FECHA_APLICACION DESC")
        return results.map { row ->
            Postulacion(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_postulante = row[2] as Int,
                id_postulacion = row[3] as Int,
                fecha_aplicacion = row[4] as? String,
                estado_proceso = row[5] as? String
            )
        }
    }

    fun search(query: String): List<Postulacion> {
        val results = db.search("POSTULACION", "ESTADO_PROCESO", query)
        return results.map { row ->
            Postulacion(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_postulante = row[2] as Int,
                id_postulacion = row[3] as Int,
                fecha_aplicacion = row[4] as? String,
                estado_proceso = row[5] as? String
            )
        }
    }

    fun insert(data: Postulacion): Long {
        val fecha = if (data.fecha_aplicacion != null) "'${data.fecha_aplicacion}'" else "NULL"
        val estado = if (data.estado_proceso != null) "'${data.estado_proceso}'" else "NULL"
        val query = "INSERT INTO POSTULACION (ID_EMPRESA, ID_OFERTA, ID_POSTULANTE, ID_POSTULACION, FECHA_APLICACION, ESTADO_PROCESO) VALUES (${data.id_empresa}, ${data.id_oferta}, ${data.id_postulante}, ${data.id_postulacion}, $fecha, $estado)"
        return db.executeInsert(query)
    }

    fun update(data: Postulacion): Int {
        val fecha = if (data.fecha_aplicacion != null) "'${data.fecha_aplicacion}'" else "NULL"
        val estado = if (data.estado_proceso != null) "'${data.estado_proceso}'" else "NULL"
        val query = "UPDATE POSTULACION SET FECHA_APLICACION = $fecha, ESTADO_PROCESO = $estado WHERE ID_EMPRESA = ${data.id_empresa} AND ID_OFERTA = ${data.id_oferta} AND ID_POSTULANTE = ${data.id_postulante} AND ID_POSTULACION = ${data.id_postulacion}"
        return db.executeUpdate(query)
    }

    fun delete(empresaId: Int, ofertaId: Int, postulanteId: Int, postulacionId: Int): Int {
        return db.executeDelete("DELETE FROM POSTULACION WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId AND ID_POSTULANTE = $postulanteId AND ID_POSTULACION = $postulacionId")
    }

    fun getCount(): Int = db.getCount("POSTULACION")
}