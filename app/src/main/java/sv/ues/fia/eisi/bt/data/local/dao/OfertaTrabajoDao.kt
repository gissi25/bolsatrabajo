package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.OfertaTrabajo

class OfertaTrabajoDao(private val db: ConnectionHelper) {

    fun getAll(): List<OfertaTrabajo> {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO ORDER BY FECHA_PUBLICACION DESC")
        return results.map { row ->
            OfertaTrabajo(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_grado_academico = row[2].toString().toIntOrNull(),
                titulo_puesto = row[3].toString(),
                fecha_publicacion = row[4].toString().takeIf { it.isNotBlank() },
                fecha_caducidad = row[5].toString().takeIf { it.isNotBlank() },
                experiencia_anios = row[6].toString().toIntOrNull(),
                edad_minima = row[7].toString().toIntOrNull(),
                edad_maxima = row[8].toString().toIntOrNull(),
                descripcion_oferta_trabajo = row[9].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByNitAndOferta(nit: String, ofertaId: String): OfertaTrabajo? {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND ID_OFERTA = '$ofertaId'")
        return results.firstOrNull()?.let { row ->
            OfertaTrabajo(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_grado_academico = row[2].toString().toIntOrNull(),
                titulo_puesto = row[3].toString(),
                fecha_publicacion = row[4].toString().takeIf { it.isNotBlank() },
                fecha_caducidad = row[5].toString().takeIf { it.isNotBlank() },
                experiencia_anios = row[6].toString().toIntOrNull(),
                edad_minima = row[7].toString().toIntOrNull(),
                edad_maxima = row[8].toString().toIntOrNull(),
                descripcion_oferta_trabajo = row[9].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByEmpresa(nit: String): List<OfertaTrabajo> {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO WHERE NIT = '$nit' ORDER BY FECHA_PUBLICACION DESC")
        return results.map { row ->
            OfertaTrabajo(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_grado_academico = row[2].toString().toIntOrNull(),
                titulo_puesto = row[3].toString(),
                fecha_publicacion = row[4].toString().takeIf { it.isNotBlank() },
                fecha_caducidad = row[5].toString().takeIf { it.isNotBlank() },
                experiencia_anios = row[6].toString().toIntOrNull(),
                edad_minima = row[7].toString().toIntOrNull(),
                edad_maxima = row[8].toString().toIntOrNull(),
                descripcion_oferta_trabajo = row[9].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<OfertaTrabajo> {
        val results = db.search("OFERTA_TRABAJO", "TITULO_PUESTO", query)
        return results.map { row ->
            OfertaTrabajo(
                nit = row[0].toString(),
                id_oferta = row[1].toString(),
                id_grado_academico = row[2].toString().toIntOrNull(),
                titulo_puesto = row[3].toString(),
                fecha_publicacion = row[4].toString().takeIf { it.isNotBlank() },
                fecha_caducidad = row[5].toString().takeIf { it.isNotBlank() },
                experiencia_anios = row[6].toString().toIntOrNull(),
                edad_minima = row[7].toString().toIntOrNull(),
                edad_maxima = row[8].toString().toIntOrNull(),
                descripcion_oferta_trabajo = row[9].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: OfertaTrabajo): Long {
        val gradoId = data.id_grado_academico?.toString() ?: "NULL"
        val fechaPub = if (data.fecha_publicacion != null) "'${data.fecha_publicacion}'" else "NULL"
        val fechaCad = if (data.fecha_caducidad != null) "'${data.fecha_caducidad}'" else "NULL"
        val expAnios = data.experiencia_anios?.toString() ?: "NULL"
        val edadMin = data.edad_minima?.toString() ?: "NULL"
        val edadMax = data.edad_maxima?.toString() ?: "NULL"
        val desc = if (data.descripcion_oferta_trabajo != null) "'${data.descripcion_oferta_trabajo}'" else "NULL"
        val query = "INSERT INTO OFERTA_TRABAJO (NIT, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO) VALUES ('${data.nit}', '${data.id_oferta}', $gradoId, '${data.titulo_puesto}', $fechaPub, $fechaCad, $expAnios, $edadMin, $edadMax, $desc)"
        return db.executeInsert(query)
    }

    fun update(data: OfertaTrabajo): Int {
        val gradoId = data.id_grado_academico?.toString() ?: "NULL"
        val fechaPub = if (data.fecha_publicacion != null) "'${data.fecha_publicacion}'" else "NULL"
        val fechaCad = if (data.fecha_caducidad != null) "'${data.fecha_caducidad}'" else "NULL"
        val expAnios = data.experiencia_anios?.toString() ?: "NULL"
        val edadMin = data.edad_minima?.toString() ?: "NULL"
        val edadMax = data.edad_maxima?.toString() ?: "NULL"
        val desc = if (data.descripcion_oferta_trabajo != null) "'${data.descripcion_oferta_trabajo}'" else "NULL"
        val query = "UPDATE OFERTA_TRABAJO SET ID_GRADO_ACADEMICO = $gradoId, TITULO_PUESTO = '${data.titulo_puesto}', FECHA_PUBLICACION = $fechaPub, FECHA_CADUCIDAD = $fechaCad, EXPERIENCIA_ANIOS = $expAnios, EDAD_MINIMA = $edadMin, EDAD_MAXIMA = $edadMax, DESCRIPCION_OFERTA_TRABAJO = $desc WHERE NIT = '${data.nit}' AND ID_OFERTA = '${data.id_oferta}'"
        return db.executeUpdate(query)
    }

    fun delete(nit: String, ofertaId: String): Int {
        return db.executeDelete("DELETE FROM OFERTA_TRABAJO WHERE NIT = '$nit' AND ID_OFERTA = '$ofertaId'")
    }

    fun getCount(): Int = db.getCount("OFERTA_TRABAJO")
}
