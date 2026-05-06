package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Institucion

class InstitucionDao(private val db: ConnectionHelper) {

    fun getAll(): List<Institucion> {
        val results = db.executeQuery("SELECT * FROM INSTITUCION ORDER BY NOMBRE_INSTITUCION")
        return results.map { row ->
            Institucion(
                id_institucion = row[0].toString(),
                nombre_institucion = row[1].toString()
            )
        }
    }

    fun getById(id: String): Institucion? {
        val results = db.getById("INSTITUCION", "ID_INSTITUCION", id)
        return results.firstOrNull()?.let { row ->
            Institucion(
                id_institucion = row[0].toString(),
                nombre_institucion = row[1].toString()
            )
        }
    }

    fun search(query: String): List<Institucion> {
        val results = db.search("INSTITUCION", "NOMBRE_INSTITUCION", query)
        return results.map { row ->
            Institucion(
                id_institucion = row[0].toString(),
                nombre_institucion = row[1].toString()
            )
        }
    }

    fun insert(data: Institucion): Long {
        val query = "INSERT INTO INSTITUCION (ID_INSTITUCION, NOMBRE_INSTITUCION) VALUES ('${data.id_institucion}', '${data.nombre_institucion}')"
        return db.executeInsert(query)
    }

    fun update(data: Institucion): Int {
        val query = "UPDATE INSTITUCION SET NOMBRE_INSTITUCION = '${data.nombre_institucion}' WHERE ID_INSTITUCION = '${data.id_institucion}'"
        return db.executeUpdate(query)
    }

    fun delete(id: String): Int = db.deleteById("INSTITUCION", "ID_INSTITUCION", id)

    fun getCount(): Int = db.getCount("INSTITUCION")
}
