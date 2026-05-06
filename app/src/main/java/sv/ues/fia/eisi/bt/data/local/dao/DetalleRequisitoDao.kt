package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.DetalleRequisito

class DetalleRequisitoDao(private val db: ConnectionHelper) {

    fun getAll(): List<DetalleRequisito> {
        val results = db.executeQuery("SELECT * FROM DETALLE_REQUISITO ORDER BY ID_DETALLE")
        return results.map { row ->
            DetalleRequisito(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_detalle = row[2].toString(),
                descripcion_requisito = row[3].toString()
            )
        }
    }

    fun getByOferta(nit: String, ofertaId: String): List<DetalleRequisito> {
        val results = db.executeQuery("SELECT * FROM DETALLE_REQUISITO WHERE NIT = '$nit' AND ID_OFERTA = '$ofertaId' ORDER BY ID_DETALLE")
        return results.map { row ->
            DetalleRequisito(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_detalle = row[2].toString(),
                descripcion_requisito = row[3].toString()
            )
        }
    }

    fun search(query: String): List<DetalleRequisito> {
        val results = db.search("DETALLE_REQUISITO", "DESCRIPCION_REQUISITO", query)
        return results.map { row ->
            DetalleRequisito(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_detalle = row[2].toString(),
                descripcion_requisito = row[3].toString()
            )
        }
    }

    fun insert(data: DetalleRequisito): Long {
        val query = "INSERT INTO DETALLE_REQUISITO (NIT, ID_OFERTA, ID_DETALLE, DESCRIPCION_REQUISITO) VALUES ('${data.nit}', '${data.id_oferta}', '${data.id_detalle}', '${data.descripcion_requisito}')"
        return db.executeInsert(query)
    }

    fun update(data: DetalleRequisito): Int {
        val query = "UPDATE DETALLE_REQUISITO SET DESCRIPCION_REQUISITO = '${data.descripcion_requisito}' WHERE NIT = '${data.nit}' AND ID_OFERTA = '${data.id_oferta}' AND ID_DETALLE = '${data.id_detalle}'"
        return db.executeUpdate(query)
    }

    fun delete(nit: String, ofertaId: String, detalleId: String): Int {
        return db.executeDelete("DELETE FROM DETALLE_REQUISITO WHERE NIT = '$nit' AND ID_OFERTA = '$ofertaId' AND ID_DETALLE = '$detalleId'")
    }

    fun getCount(): Int = db.getCount("DETALLE_REQUISITO")
}
