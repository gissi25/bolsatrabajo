package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Postulante

class PostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<Postulante> {
        val results = db.executeQuery("SELECT * FROM POSTULANTE ORDER BY APELLIDO, NOMBRE")
        return results.map { row ->
            Postulante(
                id_postulante = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                id_genero = (row[1] as? Int) ?: (row[1] as? String)?.toIntOrNull() ?: 0,
                id_tipo_documento = (row[2] as? Int) ?: (row[2] as? String)?.toIntOrNull() ?: 0,
                id_distrito = (row[3] as? Int) ?: (row[3] as? String)?.toIntOrNull(),
                nombre = row[4] as? String ?: "",
                apellido = row[5] as? String ?: "",
                fecha_nacimiento = row[6] as? String,
                num_documento = row[7] as? String,
                nup = row[8] as? String,
                direccion_detalle = row[9] as? String,
                telefono_casa = row[10] as? String,
                telefono_celular = row[11] as? String,
                email = row[12] as? String
            )
        }
    }

    fun getById(id: Int): Postulante? {
        val results = db.getById("POSTULANTE", "ID_POSTULANTE", id)
        return results.firstOrNull()?.let { row ->
            Postulante(
                id_postulante = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                id_genero = (row[1] as? Int) ?: (row[1] as? String)?.toIntOrNull() ?: 0,
                id_tipo_documento = (row[2] as? Int) ?: (row[2] as? String)?.toIntOrNull() ?: 0,
                id_distrito = (row[3] as? Int) ?: (row[3] as? String)?.toIntOrNull(),
                nombre = row[4] as? String ?: "",
                apellido = row[5] as? String ?: "",
                fecha_nacimiento = row[6] as? String,
                num_documento = row[7] as? String,
                nup = row[8] as? String,
                direccion_detalle = row[9] as? String,
                telefono_casa = row[10] as? String,
                telefono_celular = row[11] as? String,
                email = row[12] as? String
            )
        }
    }

    fun search(query: String): List<Postulante> {
        val sql = "SELECT * FROM POSTULANTE WHERE NOMBRE LIKE '%$query%' OR APELLIDO LIKE '%$query%' OR NUM_DOCUMENTO LIKE '%$query%' ORDER BY APELLIDO, NOMBRE"
        val results = db.executeQuery(sql)
        return results.map { row ->
            Postulante(
                id_postulante = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                id_genero = (row[1] as? Int) ?: (row[1] as? String)?.toIntOrNull() ?: 0,
                id_tipo_documento = (row[2] as? Int) ?: (row[2] as? String)?.toIntOrNull() ?: 0,
                id_distrito = (row[3] as? Int) ?: (row[3] as? String)?.toIntOrNull(),
                nombre = row[4] as? String ?: "",
                apellido = row[5] as? String ?: "",
                fecha_nacimiento = row[6] as? String,
                num_documento = row[7] as? String,
                nup = row[8] as? String,
                direccion_detalle = row[9] as? String,
                telefono_casa = row[10] as? String,
                telefono_celular = row[11] as? String,
                email = row[12] as? String
            )
        }
    }

    fun insert(data: Postulante): Long {
        val distritoId = data.id_distrito ?: "NULL"
        val fechaNac = if (data.fecha_nacimiento != null) "'${data.fecha_nacimiento}'" else "NULL"
        val numDoc = if (data.num_documento != null) "'${data.num_documento}'" else "NULL"
        val nup = if (data.nup != null) "'${data.nup}'" else "NULL"
        val direccion = if (data.direccion_detalle != null) "'${data.direccion_detalle}'" else "NULL"
        val telCasa = if (data.telefono_casa != null) "'${data.telefono_casa}'" else "NULL"
        val telCel = if (data.telefono_celular != null) "'${data.telefono_celular}'" else "NULL"
        val email = if (data.email != null) "'${data.email}'" else "NULL"

        val query = """
            INSERT INTO POSTULANTE (ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, ID_DISTRITO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUM_DOCUMENTO, NUP, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL)
            VALUES (${data.id_postulante}, ${data.id_genero}, ${data.id_tipo_documento}, $distritoId, '${data.nombre}', '${data.apellido}', $fechaNac, $numDoc, $nup, $direccion, $telCasa, $telCel, $email)
        """.trimIndent()
        return db.executeInsert(query)
    }

    fun update(data: Postulante): Int {
        val distritoId = data.id_distrito ?: "NULL"
        val fechaNac = if (data.fecha_nacimiento != null) "'${data.fecha_nacimiento}'" else "NULL"
        val numDoc = if (data.num_documento != null) "'${data.num_documento}'" else "NULL"
        val nup = if (data.nup != null) "'${data.nup}'" else "NULL"
        val direccion = if (data.direccion_detalle != null) "'${data.direccion_detalle}'" else "NULL"
        val telCasa = if (data.telefono_casa != null) "'${data.telefono_casa}'" else "NULL"
        val telCel = if (data.telefono_celular != null) "'${data.telefono_celular}'" else "NULL"
        val email = if (data.email != null) "'${data.email}'" else "NULL"

        val query = """
            UPDATE POSTULANTE SET
            ID_GENERO = ${data.id_genero}, ID_TIPO_DOCUMENTO = ${data.id_tipo_documento}, ID_DISTRITO = $distritoId,
            NOMBRE = '${data.nombre}', APELLIDO = '${data.apellido}',
            FECHA_NACIMIENTO = $fechaNac, NUM_DOCUMENTO = $numDoc, NUP = $nup, DIRECCION_DETALLE = $direccion,
            TELEFONO_CASA = $telCasa, TELEFONO_CELULAR = $telCel, EMAIL = $email
            WHERE ID_POSTULANTE = ${data.id_postulante}
        """.trimIndent()
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("POSTULANTE", "ID_POSTULANTE", id)

    fun getCount(): Int = db.getCount("POSTULANTE")
}