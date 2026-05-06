package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.RedSocial

class RedSocialDao(private val db: ConnectionHelper) {

    fun getAll(): List<RedSocial> {
        val results = db.executeQuery("SELECT * FROM RED_SOCIAL ORDER BY NOMBRE_RED")
        return results.map { row ->
            RedSocial(
                id_red_social = row[0].toString().toIntOrNull() ?: 0,
                nombre_red = row[1].toString()
            )
        }
    }

    fun getById(id: Int): RedSocial? {
        val results = db.getById("RED_SOCIAL", "ID_RED_SOCIAL", id.toString())
        return results.firstOrNull()?.let { row ->
            RedSocial(
                id_red_social = row[0].toString().toIntOrNull() ?: 0,
                nombre_red = row[1].toString()
            )
        }
    }

    fun search(query: String): List<RedSocial> {
        val results = db.search("RED_SOCIAL", "NOMBRE_RED", query)
        return results.map { row ->
            RedSocial(
                id_red_social = row[0].toString().toIntOrNull() ?: 0,
                nombre_red = row[1].toString()
            )
        }
    }

    fun insert(data: RedSocial): Long {
        val query = "INSERT INTO RED_SOCIAL (NOMBRE_RED) VALUES ('${data.nombre_red}')"
        return db.executeInsert(query)
    }

    fun update(data: RedSocial): Int {
        val query = "UPDATE RED_SOCIAL SET NOMBRE_RED = '${data.nombre_red}' WHERE ID_RED_SOCIAL = ${data.id_red_social}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("RED_SOCIAL", "ID_RED_SOCIAL", id.toString())

    fun getCount(): Int = db.getCount("RED_SOCIAL")
}
