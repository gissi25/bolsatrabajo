package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Empresa

class EmpresaDao(private val db: ConnectionHelper) {

    fun getAll(): List<Empresa> {
        val results = db.executeQuery("SELECT * FROM EMPRESA ORDER BY NOMBRE_EMPRESA")
        return results.map { row ->
            Empresa(
                nit = row[0].toString(),
                id_distrito_depto = row[1].toString().toIntOrNull() ?: 0,
                id_distrito_municipio = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_id = row[3].toString().toIntOrNull() ?: 0,
                nombre_empresa = row[4].toString(),
                contacto_directo = row[5].toString()
            )
        }
    }

    fun getByNit(nit: String): Empresa? {
        val results = db.getById("EMPRESA", "NIT", nit)
        return results.firstOrNull()?.let { row ->
            Empresa(
                nit = row[0].toString(),
                id_distrito_depto = row[1].toString().toIntOrNull() ?: 0,
                id_distrito_municipio = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_id = row[3].toString().toIntOrNull() ?: 0,
                nombre_empresa = row[4].toString(),
                contacto_directo = row[5].toString()
            )
        }
    }

    fun search(query: String): List<Empresa> {
        val results = db.search("EMPRESA", "NOMBRE_EMPRESA", query)
        return results.map { row ->
            Empresa(
                nit = row[0].toString(),
                id_distrito_depto = row[1].toString().toIntOrNull() ?: 0,
                id_distrito_municipio = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_id = row[3].toString().toIntOrNull() ?: 0,
                nombre_empresa = row[4].toString(),
                contacto_directo = row[5].toString()
            )
        }
    }

    fun insert(data: Empresa): Long {
        val query = "INSERT INTO EMPRESA (NIT, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, NOMBRE_EMPRESA, CONTACTO_DIRECTO) VALUES ('${data.nit}', ${data.id_distrito_depto}, ${data.id_distrito_municipio}, ${data.id_distrito_id}, '${data.nombre_empresa}', '${data.contacto_directo}')"
        return db.executeInsert(query)
    }

    fun update(data: Empresa): Int {
        val query = "UPDATE EMPRESA SET ID_DISTRITO_DEPTO = ${data.id_distrito_depto}, ID_DISTRITO_MUNICIPIO = ${data.id_distrito_municipio}, ID_DISTRITO_ID = ${data.id_distrito_id}, NOMBRE_EMPRESA = '${data.nombre_empresa}', CONTACTO_DIRECTO = '${data.contacto_directo}' WHERE NIT = '${data.nit}'"
        return db.executeUpdate(query)
    }

    fun delete(nit: String): Int = db.deleteById("EMPRESA", "NIT", nit)

    fun getCount(): Int = db.getCount("EMPRESA")
}
