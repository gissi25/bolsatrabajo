package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Usuario

class UsuarioDao(private val db: ConnectionHelper) {

    fun getAll(): List<Usuario> {
        val results = db.executeQuery("SELECT * FROM USUARIO ORDER BY USERNAME")
        return results.map { row ->
            Usuario(
                id_usuario = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                username = row[1] as? String ?: "",
                password = row[2] as? String ?: "",
                rol = row[3] as? String ?: ""
            )
        }
    }

    fun getById(id: Int): Usuario? {
        val results = db.getById("USUARIO", "ID_USUARIO", id)
        return results.firstOrNull()?.let { row ->
            Usuario(
                id_usuario = row[0] as Int,
                username = row[1] as String,
                password = row[2] as String,
                rol = row[3] as String
            )
        }
    }

    fun getByUsername(username: String): Usuario? {
        val results = db.executeQuery("SELECT * FROM USUARIO WHERE USERNAME = '$username'")
        return results.firstOrNull()?.let { row ->
            Usuario(
                id_usuario = row[0] as Int,
                username = row[1] as String,
                password = row[2] as String,
                rol = row[3] as String
            )
        }
    }

    fun search(query: String): List<Usuario> {
        val results = db.search("USUARIO", "USERNAME", query)
        return results.map { row ->
            Usuario(
                id_usuario = row[0] as Int,
                username = row[1] as String,
                password = row[2] as String,
                rol = row[3] as String
            )
        }
    }

    fun insert(data: Usuario): Long {
        val query = "INSERT INTO USUARIO (ID_USUARIO, USERNAME, PASSWORD, ROL) VALUES (${data.id_usuario}, '${data.username}', '${data.password}', '${data.rol}')"
        return db.executeInsert(query)
    }

    fun update(data: Usuario): Int {
        val query = "UPDATE USUARIO SET USERNAME = '${data.username}', PASSWORD = '${data.password}', ROL = '${data.rol}' WHERE ID_USUARIO = ${data.id_usuario}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("USUARIO", "ID_USUARIO", id)

    fun getCount(): Int = db.getCount("USUARIO")
}