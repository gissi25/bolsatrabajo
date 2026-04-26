package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Postulante

class PostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<Postulante> {
        val results = db.executeQuery("SELECT * FROM POSTULANTE ORDER BY APELLIDO, NOMBRE")
        return results.map { row ->
            Postulante(
                id_postulante = (row[0] as? Int) ?: (row[0] as? String)?.toIntOrNull() ?: 0,
                id_usuario = (row[1] as? Int) ?: (row[1] as? String)?.toIntOrNull(),
                id_genero = (row[2] as? Int) ?: (row[2] as? String)?.toIntOrNull() ?: 0,
                id_distrito = (row[3] as? Int) ?: (row[3] as? String)?.toIntOrNull(),
                id_tipo_documento = (row[4] as? Int) ?: (row[4] as? String)?.toIntOrNull() ?: 0,
                nombre = row[5] as? String ?: "",
                apellido = row[6] as? String ?: "",
                fecha_nacimiento = row[7] as? String,
                num_documento = row[8] as? String,
                nup = row[9] as? String,
                direccion_detalle = row[10] as? String,
                telefono_casa = row[11] as? String,
                telefono_celular = row[12] as? String,
                email = row[13] as? String
            )
        }
    }

    fun getById(id: Int): Postulante? {
        val results = db.getById("POSTULANTE", "ID_POSTULANTE", id)
        return results.firstOrNull()?.let { row ->
            Postulante(
                id_postulante = row[0] as Int,
                id_usuario = row[1] as? Int,
                id_genero = row[2] as Int,
                id_distrito = row[3] as? Int,
                id_tipo_documento = row[4] as Int,
                nombre = row[5] as String,
                apellido = row[6] as String,
                fecha_nacimiento = row[7] as? String,
                num_documento = row[8] as? String,
                nup = row[9] as? String,
                direccion_detalle = row[10] as? String,
                telefono_casa = row[11] as? String,
                telefono_celular = row[12] as? String,
                email = row[13] as? String
            )
        }
    }

    fun search(query: String): List<Postulante> {
        val sql = "SELECT * FROM POSTULANTE WHERE NOMBRE LIKE '%$query%' OR APELLIDO LIKE '%$query%' OR NUM_DOCUMENTO LIKE '%$query%' ORDER BY APELLIDO, NOMBRE"
        val results = db.executeQuery(sql)
        return results.map { row ->
            Postulante(
                id_postulante = row[0] as Int,
                id_usuario = row[1] as? Int,
                id_genero = row[2] as Int,
                id_distrito = row[3] as? Int,
                id_tipo_documento = row[4] as Int,
                nombre = row[5] as String,
                apellido = row[6] as String,
                fecha_nacimiento = row[7] as? String,
                num_documento = row[8] as? String,
                nup = row[9] as? String,
                direccion_detalle = row[10] as? String,
                telefono_casa = row[11] as? String,
                telefono_celular = row[12] as? String,
                email = row[13] as? String
            )
        }
    }

    fun insert(data: Postulante): Long {
        val usuarioId = data.id_usuario ?: "NULL"
        val distritoId = data.id_distrito ?: "NULL"
        val fechaNac = if (data.fecha_nacimiento != null) "'${data.fecha_nacimiento}'" else "NULL"
        val numDoc = if (data.num_documento != null) "'${data.num_documento}'" else "NULL"
        val nup = if (data.nup != null) "'${data.nup}'" else "NULL"
        val direccion = if (data.direccion_detalle != null) "'${data.direccion_detalle}'" else "NULL"
        val telCasa = if (data.telefono_casa != null) "'${data.telefono_casa}'" else "NULL"
        val telCel = if (data.telefono_celular != null) "'${data.telefono_celular}'" else "NULL"
        val email = if (data.email != null) "'${data.email}'" else "NULL"

        val query = """
            INSERT INTO POSTULANTE (ID_POSTULANTE, ID_USUARIO, ID_GENERO, ID_DISTRITO, ID_TIPO_DOCUMENTO, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUM_DOCUMENTO, NUP, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL)
            VALUES (${data.id_postulante}, $usuarioId, ${data.id_genero}, $distritoId, ${data.id_tipo_documento}, '${data.nombre}', '${data.apellido}', $fechaNac, $numDoc, $nup, $direccion, $telCasa, $telCel, $email)
        """.trimIndent()
        return db.executeInsert(query)
    }

    fun update(data: Postulante): Int {
        val usuarioId = data.id_usuario ?: "NULL"
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
            ID_USUARIO = $usuarioId, ID_GENERO = ${data.id_genero}, ID_DISTRITO = $distritoId,
            ID_TIPO_DOCUMENTO = ${data.id_tipo_documento}, NOMBRE = '${data.nombre}', APELLIDO = '${data.apellido}',
            FECHA_NACIMIENTO = $fechaNac, NUM_DOCUMENTO = $numDoc, NUP = $nup, DIRECCION_DETALLE = $direccion,
            TELEFONO_CASA = $telCasa, TELEFONO_CELULAR = $telCel, EMAIL = $email
            WHERE ID_POSTULANTE = ${data.id_postulante}
        """.trimIndent()
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("POSTULANTE", "ID_POSTULANTE", id)

    fun getCount(): Int = db.getCount("POSTULANTE")
}