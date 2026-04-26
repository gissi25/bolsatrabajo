package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.DetalleRequisito

class DetalleRequisitoDao(private val db: ConnectionHelper) {

    fun getAll(): List<DetalleRequisito> {
        val results = db.executeQuery("SELECT * FROM DETALLE_REQUISITO ORDER BY DESCRIPCION_REQUISITO")
        return results.map { row ->
            DetalleRequisito(
                id_detalle = row[0] as Int,
                id_empresa = row[1] as? Int,
                id_oferta = row[2] as? Int,
                descripcion_requisito = row[3] as String
            )
        }
    }

    fun getById(id: Int): DetalleRequisito? {
        val results = db.getById("DETALLE_REQUISITO", "ID_DETALLE", id)
        return results.firstOrNull()?.let { row ->
            DetalleRequisito(
                id_detalle = row[0] as Int,
                id_empresa = row[1] as? Int,
                id_oferta = row[2] as? Int,
                descripcion_requisito = row[3] as String
            )
        }
    }

    fun getByOferta(empresaId: Int, ofertaId: Int): List<DetalleRequisito> {
        val results = db.executeQuery("SELECT * FROM DETALLE_REQUISITO WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId ORDER BY DESCRIPCION_REQUISITO")
        return results.map { row ->
            DetalleRequisito(
                id_detalle = row[0] as Int,
                id_empresa = row[1] as? Int,
                id_oferta = row[2] as? Int,
                descripcion_requisito = row[3] as String
            )
        }
    }

    fun search(query: String): List<DetalleRequisito> {
        val results = db.search("DETALLE_REQUISITO", "DESCRIPCION_REQUISITO", query)
        return results.map { row ->
            DetalleRequisito(
                id_detalle = row[0] as Int,
                id_empresa = row[1] as? Int,
                id_oferta = row[2] as? Int,
                descripcion_requisito = row[3] as String
            )
        }
    }

    fun insert(data: DetalleRequisito): Long {
        val empresaId = data.id_empresa ?: "NULL"
        val ofertaId = data.id_oferta ?: "NULL"
        val query = "INSERT INTO DETALLE_REQUISITO (ID_DETALLE, ID_EMPRESA, ID_OFERTA, DESCRIPCION_REQUISITO) VALUES (${data.id_detalle}, $empresaId, $ofertaId, '${data.descripcion_requisito}')"
        return db.executeInsert(query)
    }

    fun update(data: DetalleRequisito): Int {
        val empresaId = data.id_empresa ?: "NULL"
        val ofertaId = data.id_oferta ?: "NULL"
        val query = "UPDATE DETALLE_REQUISITO SET ID_EMPRESA = $empresaId, ID_OFERTA = $ofertaId, DESCRIPCION_REQUISITO = '${data.descripcion_requisito}' WHERE ID_DETALLE = ${data.id_detalle}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("DETALLE_REQUISITO", "ID_DETALLE", id)

    fun getCount(): Int = db.getCount("DETALLE_REQUISITO")
}