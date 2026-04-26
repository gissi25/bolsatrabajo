package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Genero

class GeneroDao(private val db: ConnectionHelper) {

    fun getAll(): List<Genero> {
        val results = db.executeQuery("SELECT * FROM GENERO ORDER BY NOMBRE_GENERO")
        return results.map { row ->
            Genero(
                id_genero = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                nombre_genero = row[1] as? String ?: ""
            )
        }
    }

    fun search(query: String): List<Genero> {
        val sql = "SELECT * FROM GENERO WHERE NOMBRE_GENERO LIKE '%$query%'"
        val results = db.executeQuery(sql)
        return results.map { row ->
            Genero(
                id_genero = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                nombre_genero = row[1] as? String ?: ""
            )
        }
    }

    fun insert(data: Genero): Long {
        val query = "INSERT INTO GENERO (NOMBRE_GENERO) VALUES ('${data.nombre_genero}')"
        return db.executeInsert(query)
    }

    fun update(data: Genero): Int {
        val query = "UPDATE GENERO SET NOMBRE_GENERO = '${data.nombre_genero}' WHERE ID_GENERO = ${data.id_genero}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("GENERO", "ID_GENERO", id)

    fun getCount(): Int = db.getCount("GENERO")
}