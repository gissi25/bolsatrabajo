package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.Postulante

class PostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<Postulante> {
        val results = db.executeQuery("SELECT * FROM POSTULANTE ORDER BY APELLIDO, NOMBRE")
        return results.map { row ->
            Postulante(
                id_postulante = row[0].toString(),
                id_genero = row[1].toString().toIntOrNull() ?: 0,
                id_tipo_documento = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_depto = row[3].toString().toIntOrNull(),
                id_distrito_municipio = row[4].toString().toIntOrNull(),
                id_distrito_id = row[5].toString().toIntOrNull(),
                nombre = row[6].toString(),
                apellido = row[7].toString(),
                fecha_nacimiento = row[8].toString().takeIf { it.isNotBlank() },
                num_documento = row[9].toString().takeIf { it.isNotBlank() },
                nup = row[10].toString().takeIf { it.isNotBlank() },
                direccion_detalle = row[11].toString().takeIf { it.isNotBlank() },
                telefono_casa = row[12].toString().takeIf { it.isNotBlank() },
                telefono_celular = row[13].toString().takeIf { it.isNotBlank() },
                email = row[14].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getById(id: String): Postulante? {
        val results = db.getById("POSTULANTE", "ID_POSTULANTE", id)
        return results.firstOrNull()?.let { row ->
            Postulante(
                id_postulante = row[0].toString(),
                id_genero = row[1].toString().toIntOrNull() ?: 0,
                id_tipo_documento = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_depto = row[3].toString().toIntOrNull(),
                id_distrito_municipio = row[4].toString().toIntOrNull(),
                id_distrito_id = row[5].toString().toIntOrNull(),
                nombre = row[6].toString(),
                apellido = row[7].toString(),
                fecha_nacimiento = row[8].toString().takeIf { it.isNotBlank() },
                num_documento = row[9].toString().takeIf { it.isNotBlank() },
                nup = row[10].toString().takeIf { it.isNotBlank() },
                direccion_detalle = row[11].toString().takeIf { it.isNotBlank() },
                telefono_casa = row[12].toString().takeIf { it.isNotBlank() },
                telefono_celular = row[13].toString().takeIf { it.isNotBlank() },
                email = row[14].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<Postulante> {
        val sql = "SELECT * FROM POSTULANTE WHERE NOMBRE LIKE '%$query%' OR APELLIDO LIKE '%$query%' OR NUM_DOCUMENTO LIKE '%$query%' ORDER BY APELLIDO, NOMBRE"
        val results = db.executeQuery(sql)
        return results.map { row ->
            Postulante(
                id_postulante = row[0].toString(),
                id_genero = row[1].toString().toIntOrNull() ?: 0,
                id_tipo_documento = row[2].toString().toIntOrNull() ?: 0,
                id_distrito_depto = row[3].toString().toIntOrNull(),
                id_distrito_municipio = row[4].toString().toIntOrNull(),
                id_distrito_id = row[5].toString().toIntOrNull(),
                nombre = row[6].toString(),
                apellido = row[7].toString(),
                fecha_nacimiento = row[8].toString().takeIf { it.isNotBlank() },
                num_documento = row[9].toString().takeIf { it.isNotBlank() },
                nup = row[10].toString().takeIf { it.isNotBlank() },
                direccion_detalle = row[11].toString().takeIf { it.isNotBlank() },
                telefono_casa = row[12].toString().takeIf { it.isNotBlank() },
                telefono_celular = row[13].toString().takeIf { it.isNotBlank() },
                email = row[14].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: Postulante): Long {
        val distritoDepto = data.id_distrito_depto?.toString() ?: "NULL"
        val distritoMun = data.id_distrito_municipio?.toString() ?: "NULL"
        val distritoId = data.id_distrito_id?.toString() ?: "NULL"
        val fechaNac = if (data.fecha_nacimiento != null) "'${data.fecha_nacimiento}'" else "NULL"
        val numDoc = if (data.num_documento != null) "'${data.num_documento}'" else "NULL"
        val nup = if (data.nup != null) "'${data.nup}'" else "NULL"
        val direccion = if (data.direccion_detalle != null) "'${data.direccion_detalle}'" else "NULL"
        val telCasa = if (data.telefono_casa != null) "'${data.telefono_casa}'" else "NULL"
        val telCel = if (data.telefono_celular != null) "'${data.telefono_celular}'" else "NULL"
        val email = if (data.email != null) "'${data.email}'" else "NULL"

        val query = """
            INSERT INTO POSTULANTE (ID_POSTULANTE, ID_GENERO, ID_TIPO_DOCUMENTO, ID_DISTRITO_DEPTO, ID_DISTRITO_MUNICIPIO, ID_DISTRITO_ID, NOMBRE, APELLIDO, FECHA_NACIMIENTO, NUM_DOCUMENTO, NUP, DIRECCION_DETALLE, TELEFONO_CASA, TELEFONO_CELULAR, EMAIL)
            VALUES ('${data.id_postulante}', ${data.id_genero}, ${data.id_tipo_documento}, $distritoDepto, $distritoMun, $distritoId, '${data.nombre}', '${data.apellido}', $fechaNac, $numDoc, $nup, $direccion, $telCasa, $telCel, $email)
        """.trimIndent()
        return db.executeInsert(query)
    }

    fun update(data: Postulante): Int {
        val distritoDepto = data.id_distrito_depto?.toString() ?: "NULL"
        val distritoMun = data.id_distrito_municipio?.toString() ?: "NULL"
        val distritoId = data.id_distrito_id?.toString() ?: "NULL"
        val fechaNac = if (data.fecha_nacimiento != null) "'${data.fecha_nacimiento}'" else "NULL"
        val numDoc = if (data.num_documento != null) "'${data.num_documento}'" else "NULL"
        val nup = if (data.nup != null) "'${data.nup}'" else "NULL"
        val direccion = if (data.direccion_detalle != null) "'${data.direccion_detalle}'" else "NULL"
        val telCasa = if (data.telefono_casa != null) "'${data.telefono_casa}'" else "NULL"
        val telCel = if (data.telefono_celular != null) "'${data.telefono_celular}'" else "NULL"
        val email = if (data.email != null) "'${data.email}'" else "NULL"

        val query = """
            UPDATE POSTULANTE SET
            ID_GENERO = ${data.id_genero}, ID_TIPO_DOCUMENTO = ${data.id_tipo_documento},
            ID_DISTRITO_DEPTO = $distritoDepto, ID_DISTRITO_MUNICIPIO = $distritoMun, ID_DISTRITO_ID = $distritoId,
            NOMBRE = '${data.nombre}', APELLIDO = '${data.apellido}',
            FECHA_NACIMIENTO = $fechaNac, NUM_DOCUMENTO = $numDoc, NUP = $nup, DIRECCION_DETALLE = $direccion,
            TELEFONO_CASA = $telCasa, TELEFONO_CELULAR = $telCel, EMAIL = $email
            WHERE ID_POSTULANTE = '${data.id_postulante}'
        """.trimIndent()
        return db.executeUpdate(query)
    }

    fun delete(id: String): Int = db.deleteById("POSTULANTE", "ID_POSTULANTE", id)

    fun getCount(): Int = db.getCount("POSTULANTE")
}
