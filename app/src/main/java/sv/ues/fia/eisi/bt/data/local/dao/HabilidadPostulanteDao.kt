package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.HabilidadPostulante

class HabilidadPostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<HabilidadPostulante> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD_POSTULANTE")
        return results.map { row ->
            HabilidadPostulante(
                id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
                id_habilidad = row[1].toString(),
                id_postulante = row[2].toString(),
                nivel_destreza = row[3].toString().toIntOrNull()
            )
        }
    }

    fun getByPostulante(postulanteId: String): List<HabilidadPostulante> {
        val results = db.executeQuery("SELECT * FROM HABILIDAD_POSTULANTE WHERE ID_POSTULANTE = '$postulanteId'")
        return results.map { row ->
            HabilidadPostulante(
                id_categoria_habilidad = row[0].toString().toIntOrNull() ?: 0,
                id_habilidad = row[1].toString(),
                id_postulante = row[2].toString(),
                nivel_destreza = row[3].toString().toIntOrNull()
            )
        }
    }

    fun insert(data: HabilidadPostulante): Long {
        val nivel = data.nivel_destreza?.toString() ?: "NULL"
        val query = "INSERT INTO HABILIDAD_POSTULANTE (ID_CATEGORIA_HABILIDAD, ID_HABILIDAD, ID_POSTULANTE, NIVEL_DESTREZA) VALUES (${data.id_categoria_habilidad}, '${data.id_habilidad}', '${data.id_postulante}', $nivel)"
        return db.executeInsert(query)
    }

    fun update(data: HabilidadPostulante): Int {
        val nivel = data.nivel_destreza?.toString() ?: "NULL"
        val query = "UPDATE HABILIDAD_POSTULANTE SET NIVEL_DESTREZA = $nivel WHERE ID_CATEGORIA_HABILIDAD = ${data.id_categoria_habilidad} AND ID_HABILIDAD = '${data.id_habilidad}' AND ID_POSTULANTE = '${data.id_postulante}'"
        return db.executeUpdate(query)
    }

    fun delete(categoriaId: Int, habilidadId: String, postulanteId: String): Int {
        return db.executeDelete("DELETE FROM HABILIDAD_POSTULANTE WHERE ID_CATEGORIA_HABILIDAD = $categoriaId AND ID_HABILIDAD = '$habilidadId' AND ID_POSTULANTE = '$postulanteId'")
    }

    fun getCount(): Int = db.getCount("HABILIDAD_POSTULANTE")
}
