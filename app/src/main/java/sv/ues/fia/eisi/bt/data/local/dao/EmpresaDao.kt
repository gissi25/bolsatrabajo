package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Empresa

class EmpresaDao(private val db: ConnectionHelper) {

    fun getAll(): List<Empresa> {
        val results = db.executeQuery("SELECT * FROM EMPRESA ORDER BY NOMBRE_EMPRESA")
        return results.map { row ->
            Empresa(
                id_empresa = row[0] as Int,
                id_distrito = row[1] as Int,
                nombre_empresa = row[2] as String,
                contacto_directo = row[3] as String,
                nit = row[4] as String
            )
        }
    }

    fun getById(id: Int): Empresa? {
        val results = db.getById("EMPRESA", "ID_EMPRESA", id)
        return results.firstOrNull()?.let { row ->
            Empresa(
                id_empresa = row[0] as Int,
                id_distrito = row[1] as Int,
                nombre_empresa = row[2] as String,
                contacto_directo = row[3] as String,
                nit = row[4] as String
            )
        }
    }

    fun search(query: String): List<Empresa> {
        val results = db.search("EMPRESA", "NOMBRE_EMPRESA", query)
        return results.map { row ->
            Empresa(
                id_empresa = row[0] as Int,
                id_distrito = row[1] as Int,
                nombre_empresa = row[2] as String,
                contacto_directo = row[3] as String,
                nit = row[4] as String
            )
        }
    }

    fun insert(data: Empresa): Long {
        val query = "INSERT INTO EMPRESA (ID_EMPRESA, ID_DISTRITO, NOMBRE_EMPRESA, CONTACTO_DIRECTO, NIT) VALUES (${data.id_empresa}, ${data.id_distrito}, '${data.nombre_empresa}', '${data.contacto_directo}', '${data.nit}')"
        return db.executeInsert(query)
    }

    fun update(data: Empresa): Int {
        val query = "UPDATE EMPRESA SET ID_DISTRITO = ${data.id_distrito}, NOMBRE_EMPRESA = '${data.nombre_empresa}', CONTACTO_DIRECTO = '${data.contacto_directo}', NIT = '${data.nit}' WHERE ID_EMPRESA = ${data.id_empresa}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("EMPRESA", "ID_EMPRESA", id)

    fun getCount(): Int = db.getCount("EMPRESA")
}