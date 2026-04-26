package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.OfertaTrabajo

class OfertaTrabajoDao(private val db: ConnectionHelper) {

    fun getAll(): List<OfertaTrabajo> {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO ORDER BY FECHA_PUBLICACION DESC")
        return results.map { row ->
            OfertaTrabajo(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_grado_academico = row[2] as? Int,
                titulo_puesto = row[3] as String,
                fecha_publicacion = row[4] as? String,
                fecha_caducidad = row[5] as? String,
                experiencia_anios = row[6] as? Int,
                edad_minima = row[7] as? Int,
                edad_maxima = row[8] as? Int,
                descripcion_oferta_trabajo = row[9] as? String
            )
        }
    }

    fun getById(empresaId: Int, ofertaId: Int): OfertaTrabajo? {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId")
        return results.firstOrNull()?.let { row ->
            OfertaTrabajo(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_grado_academico = row[2] as? Int,
                titulo_puesto = row[3] as String,
                fecha_publicacion = row[4] as? String,
                fecha_caducidad = row[5] as? String,
                experiencia_anios = row[6] as? Int,
                edad_minima = row[7] as? Int,
                edad_maxima = row[8] as? Int,
                descripcion_oferta_trabajo = row[9] as? String
            )
        }
    }

    fun getByEmpresa(empresaId: Int): List<OfertaTrabajo> {
        val results = db.executeQuery("SELECT * FROM OFERTA_TRABAJO WHERE ID_EMPRESA = $empresaId ORDER BY FECHA_PUBLICACION DESC")
        return results.map { row ->
            OfertaTrabajo(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_grado_academico = row[2] as? Int,
                titulo_puesto = row[3] as String,
                fecha_publicacion = row[4] as? String,
                fecha_caducidad = row[5] as? String,
                experiencia_anios = row[6] as? Int,
                edad_minima = row[7] as? Int,
                edad_maxima = row[8] as? Int,
                descripcion_oferta_trabajo = row[9] as? String
            )
        }
    }

    fun search(query: String): List<OfertaTrabajo> {
        val results = db.search("OFERTA_TRABAJO", "TITULO_PUESTO", query)
        return results.map { row ->
            OfertaTrabajo(
                id_empresa = row[0] as Int,
                id_oferta = row[1] as Int,
                id_grado_academico = row[2] as? Int,
                titulo_puesto = row[3] as String,
                fecha_publicacion = row[4] as? String,
                fecha_caducidad = row[5] as? String,
                experiencia_anios = row[6] as? Int,
                edad_minima = row[7] as? Int,
                edad_maxima = row[8] as? Int,
                descripcion_oferta_trabajo = row[9] as? String
            )
        }
    }

    fun insert(data: OfertaTrabajo): Long {
        val gradoId = data.id_grado_academico ?: "NULL"
        val fechaPub = if (data.fecha_publicacion != null) "'${data.fecha_publicacion}'" else "NULL"
        val fechaCad = if (data.fecha_caducidad != null) "'${data.fecha_caducidad}'" else "NULL"
        val expAnios = data.experiencia_anios ?: "NULL"
        val edadMin = data.edad_minima ?: "NULL"
        val edadMax = data.edad_maxima ?: "NULL"
        val desc = if (data.descripcion_oferta_trabajo != null) "'${data.descripcion_oferta_trabajo}'" else "NULL"
        val query = "INSERT INTO OFERTA_TRABAJO (ID_EMPRESA, ID_OFERTA, ID_GRADO_ACADEMICO, TITULO_PUESTO, FECHA_PUBLICACION, FECHA_CADUCIDAD, EXPERIENCIA_ANIOS, EDAD_MINIMA, EDAD_MAXIMA, DESCRIPCION_OFERTA_TRABAJO) VALUES (${data.id_empresa}, ${data.id_oferta}, $gradoId, '${data.titulo_puesto}', $fechaPub, $fechaCad, $expAnios, $edadMin, $edadMax, $desc)"
        return db.executeInsert(query)
    }

    fun update(data: OfertaTrabajo): Int {
        val gradoId = data.id_grado_academico ?: "NULL"
        val fechaPub = if (data.fecha_publicacion != null) "'${data.fecha_publicacion}'" else "NULL"
        val fechaCad = if (data.fecha_caducidad != null) "'${data.fecha_caducidad}'" else "NULL"
        val expAnios = data.experiencia_anios ?: "NULL"
        val edadMin = data.edad_minima ?: "NULL"
        val edadMax = data.edad_maxima ?: "NULL"
        val desc = if (data.descripcion_oferta_trabajo != null) "'${data.descripcion_oferta_trabajo}'" else "NULL"
        val query = "UPDATE OFERTA_TRABAJO SET ID_GRADO_ACADEMICO = $gradoId, TITULO_PUESTO = '${data.titulo_puesto}', FECHA_PUBLICACION = $fechaPub, FECHA_CADUCIDAD = $fechaCad, EXPERIENCIA_ANIOS = $expAnios, EDAD_MINIMA = $edadMin, EDAD_MAXIMA = $edadMax, DESCRIPCION_OFERTA_TRABAJO = $desc WHERE ID_EMPRESA = ${data.id_empresa} AND ID_OFERTA = ${data.id_oferta}"
        return db.executeUpdate(query)
    }

    fun delete(empresaId: Int, ofertaId: Int): Int {
        return db.executeDelete("DELETE FROM OFERTA_TRABAJO WHERE ID_EMPRESA = $empresaId AND ID_OFERTA = $ofertaId")
    }

    fun getCount(): Int = db.getCount("OFERTA_TRABAJO")
}