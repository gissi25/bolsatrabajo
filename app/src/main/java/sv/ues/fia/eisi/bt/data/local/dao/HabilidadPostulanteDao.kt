package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.HabilidadPostulante

class HabilidadPostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<HabilidadPostulante> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD_POSTULANTE")
        return results.map { row ->
            HabilidadPostulante(
                id_habilidad = row[0] as Int,
                id_postulante = row[1] as Int,
                id_habilidad_postulante = row[2] as Int,
                nivel_destreza = row[3] as? Int
            )
        }
    }

    fun getById(habilidadId: Int, postulanteId: Int, habilidadPostId: Int): HabilidadPostulante? {
        val results = db.executeQuery("SELECT * FROM HABILIDAD_POSTULANTE WHERE ID_HABILIDAD = $habilidadId AND ID_POSTULANTE = $postulanteId AND ID_HABILIDAD_POSTULANTE = $habilidadPostId")
        return results.firstOrNull()?.let { row ->
            HabilidadPostulante(
                id_habilidad = row[0] as Int,
                id_postulante = row[1] as Int,
                id_habilidad_postulante = row[2] as Int,
                nivel_destreza = row[3] as? Int
            )
        }
    }

    fun getByPostulante(postulanteId: Int): List<HabilidadPostulante> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = $postulanteId")
        return results.map { row ->
            HabilidadPostulante(
                id_habilidad = row[0] as Int,
                id_postulante = row[1] as Int,
                id_habilidad_postulante = row[2] as Int,
                nivel_destreza = row[3] as? Int
            )
        }
    }

    fun insert(data: HabilidadPostulante): Long {
        val nivel = data.nivel_destreza ?: "NULL"
        val query = "INSERT INTO HABILIDAD_POSTULANTE (ID_HABILIDAD, ID_POSTULANTE, ID_HABILIDAD_POSTULANTE, NIVEL_DESTREZA) VALUES (${data.id_habilidad}, ${data.id_postulante}, ${data.id_habilidad_postulante}, $nivel)"
        return db.executeInsert(query)
    }

    fun update(data: HabilidadPostulante): Int {
        val nivel = data.nivel_destreza ?: "NULL"
        val query = "UPDATE HABILIDAD_POSTULANTE SET NIVEL_DESTREZA = $nivel WHERE ID_HABILIDAD = ${data.id_habilidad} AND ID_POSTULANTE = ${data.id_postulante} AND ID_HABILIDAD_POSTULANTE = ${data.id_habilidad_postulante}"
        return db.executeUpdate(query)
    }

    fun delete(habilidadId: Int, postulanteId: Int, habilidadPostId: Int): Int {
        return db.executeDelete("DELETE FROM HABILIDAD_POSTULANTE WHERE ID_HABILIDAD = $habilidadId AND ID_POSTULANTE = $postulanteId AND ID_HABILIDAD_POSTULANTE = $habilidadPostId")
    }

    fun getCount(): Int = db.getCount("HABILIDAD_POSTULANTE")
}