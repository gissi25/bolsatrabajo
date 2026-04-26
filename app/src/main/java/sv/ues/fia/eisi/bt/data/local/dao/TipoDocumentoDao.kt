package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.TipoDocumento

class TipoDocumentoDao(private val db: ConnectionHelper) {

    fun getAll(): List<TipoDocumento> {
        val results = db.executeQuery("SELECT * FROM TIPO_DOCUMENTO ORDER BY NOMBRE_TIPO")
        return results.map { row ->
            TipoDocumento(
                id_tipo_documento = row[0] as Int,
                nombre_tipo = row[1] as String
            )
        }
    }

    fun getById(id: Int): TipoDocumento? {
        val results = db.getById("TIPO_DOCUMENTO", "ID_TIPO_DOCUMENTO", id)
        return results.firstOrNull()?.let { row ->
            TipoDocumento(
                id_tipo_documento = row[0] as Int,
                nombre_tipo = row[1] as String
            )
        }
    }

    fun search(query: String): List<TipoDocumento> {
        val results = db.search("TIPO_DOCUMENTO", "NOMBRE_TIPO", query)
        return results.map { row ->
            TipoDocumento(
                id_tipo_documento = row[0] as Int,
                nombre_tipo = row[1] as String
            )
        }
    }

    fun insert(data: TipoDocumento): Long {
        val query = "INSERT INTO TIPO_DOCUMENTO (ID_TIPO_DOCUMENTO, NOMBRE_TIPO) VALUES (${data.id_tipo_documento}, '${data.nombre_tipo}')"
        return db.executeInsert(query)
    }

    fun update(data: TipoDocumento): Int {
        val query = "UPDATE TIPO_DOCUMENTO SET NOMBRE_TIPO = '${data.nombre_tipo}' WHERE ID_TIPO_DOCUMENTO = ${data.id_tipo_documento}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("TIPO_DOCUMENTO", "ID_TIPO_DOCUMENTO", id)

    fun getCount(): Int = db.getCount("TIPO_DOCUMENTO")
}