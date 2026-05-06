package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.ExperienciaLaboral

class ExperienciaLaboralDao(private val db: ConnectionHelper) {

    fun getAll(): List<ExperienciaLaboral> {
        val results = db.executeQuery("SELECT * FROM EXPERIENCIA_LABORAL ORDER BY FECHA_INICIO DESC")
        return results.map { row ->
            ExperienciaLaboral(
                id_postulante = row[0].toString(),
                nit = row[1].toString(),
                id_experiencia = row[2].toString(),
                puesto_trabajo = row[3].toString().takeIf { it.isNotBlank() },
                fecha_inicio = row[4].toString().takeIf { it.isNotBlank() },
                fecha_fin = row[5].toString().takeIf { it.isNotBlank() },
                descp_experiencia_laboral = row[6].toString().takeIf { it.isNotBlank() },
                contacto_referencia = row[7].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByPostulante(postulanteId: String): List<ExperienciaLaboral> {
        val results = db.executeQuery("SELECT * FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = '$postulanteId' ORDER BY FECHA_INICIO DESC")
        return results.map { row ->
            ExperienciaLaboral(
                id_postulante = row[0].toString(),
                nit = row[1].toString(),
                id_experiencia = row[2].toString(),
                puesto_trabajo = row[3].toString().takeIf { it.isNotBlank() },
                fecha_inicio = row[4].toString().takeIf { it.isNotBlank() },
                fecha_fin = row[5].toString().takeIf { it.isNotBlank() },
                descp_experiencia_laboral = row[6].toString().takeIf { it.isNotBlank() },
                contacto_referencia = row[7].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<ExperienciaLaboral> {
        val results = db.search("EXPERIENCIA_LABORAL", "PUESTO_TRABAJO", query)
        return results.map { row ->
            ExperienciaLaboral(
                id_postulante = row[0].toString(),
                nit = row[1].toString(),
                id_experiencia = row[2].toString(),
                puesto_trabajo = row[3].toString().takeIf { it.isNotBlank() },
                fecha_inicio = row[4].toString().takeIf { it.isNotBlank() },
                fecha_fin = row[5].toString().takeIf { it.isNotBlank() },
                descp_experiencia_laboral = row[6].toString().takeIf { it.isNotBlank() },
                contacto_referencia = row[7].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: ExperienciaLaboral): Long {
        val fechaIni = if (data.fecha_inicio != null) "'${data.fecha_inicio}'" else "NULL"
        val fechaFin = if (data.fecha_fin != null) "'${data.fecha_fin}'" else "NULL"
        val desc = if (data.descp_experiencia_laboral != null) "'${data.descp_experiencia_laboral}'" else "NULL"
        val contacto = if (data.contacto_referencia != null) "'${data.contacto_referencia}'" else "NULL"
        val query = "INSERT INTO EXPERIENCIA_LABORAL (ID_POSTULANTE, NIT, ID_EXPERIENCIA, PUESTO_TRABAJO, FECHA_INICIO, FECHA_FIN, DESCP_EXPERIENCIA_LABORAL, CONTACTO_REFERENCIA) VALUES ('${data.id_postulante}', '${data.nit}', '${data.id_experiencia}', '${data.puesto_trabajo}', $fechaIni, $fechaFin, $desc, $contacto)"
        return db.executeInsert(query)
    }

    fun update(data: ExperienciaLaboral): Int {
        val fechaIni = if (data.fecha_inicio != null) "'${data.fecha_inicio}'" else "NULL"
        val fechaFin = if (data.fecha_fin != null) "'${data.fecha_fin}'" else "NULL"
        val desc = if (data.descp_experiencia_laboral != null) "'${data.descp_experiencia_laboral}'" else "NULL"
        val contacto = if (data.contacto_referencia != null) "'${data.contacto_referencia}'" else "NULL"
        val query = "UPDATE EXPERIENCIA_LABORAL SET PUESTO_TRABAJO = '${data.puesto_trabajo}', FECHA_INICIO = $fechaIni, FECHA_FIN = $fechaFin, DESCP_EXPERIENCIA_LABORAL = $desc, CONTACTO_REFERENCIA = $contacto WHERE ID_POSTULANTE = '${data.id_postulante}' AND NIT = '${data.nit}' AND ID_EXPERIENCIA = '${data.id_experiencia}'"
        return db.executeUpdate(query)
    }

    fun delete(postulanteId: String, nit: String, experienciaId: String): Int {
        return db.executeDelete("DELETE FROM EXPERIENCIA_LABORAL WHERE ID_POSTULANTE = '$postulanteId' AND NIT = '$nit' AND ID_EXPERIENCIA = '$experienciaId'")
    }

    fun getCount(): Int = db.getCount("EXPERIENCIA_LABORAL")
}
