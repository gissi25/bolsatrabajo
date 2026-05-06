package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.RedSocialPostulante

class RedSocialPostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<RedSocialPostulante> {
        val results = db.executeQuery("SELECT * FROM RED_SOCIAL_POSTULANTE")
        return results.map { row ->
            RedSocialPostulante(
                id_postulante = row[0].toString(),
                id_red_social = row[1].toString().toIntOrNull() ?: 0,
                url_perfil = row[2].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun getByPostulante(postulanteId: String): List<RedSocialPostulante> {
        val results = db.executeQuery("SELECT * FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = '$postulanteId'")
        return results.map { row ->
            RedSocialPostulante(
                id_postulante = row[0].toString(),
                id_red_social = row[1].toString().toIntOrNull() ?: 0,
                url_perfil = row[2].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun search(query: String): List<RedSocialPostulante> {
        val results = db.search("RED_SOCIAL_POSTULANTE", "URL_PERFIL", query)
        return results.map { row ->
            RedSocialPostulante(
                id_postulante = row[0].toString(),
                id_red_social = row[1].toString().toIntOrNull() ?: 0,
                url_perfil = row[2].toString().takeIf { it.isNotBlank() }
            )
        }
    }

    fun insert(data: RedSocialPostulante): Long {
        val url = if (data.url_perfil != null) "'${data.url_perfil}'" else "NULL"
        val query = "INSERT INTO RED_SOCIAL_POSTULANTE (ID_POSTULANTE, ID_RED_SOCIAL, URL_PERFIL) VALUES ('${data.id_postulante}', ${data.id_red_social}, $url)"
        return db.executeInsert(query)
    }

    fun update(data: RedSocialPostulante): Int {
        val url = if (data.url_perfil != null) "'${data.url_perfil}'" else "NULL"
        val query = "UPDATE RED_SOCIAL_POSTULANTE SET URL_PERFIL = $url WHERE ID_POSTULANTE = '${data.id_postulante}' AND ID_RED_SOCIAL = ${data.id_red_social}"
        return db.executeUpdate(query)
    }

    fun delete(postulanteId: String, redSocialId: Int): Int {
        return db.executeDelete("DELETE FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = '$postulanteId' AND ID_RED_SOCIAL = $redSocialId")
    }

    fun getCount(): Int = db.getCount("RED_SOCIAL_POSTULANTE")
}
