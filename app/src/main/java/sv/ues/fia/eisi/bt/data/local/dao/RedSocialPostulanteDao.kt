package sv.ues.fia.eisi.bt.data.local.dao

import sv.ues.fia.eisi.bt.data.local.ConnectionHelper
import sv.ues.fia.eisi.bt.data.local.entities.RedSocialPostulante

class RedSocialPostulanteDao(private val db: ConnectionHelper) {

    fun getAll(): List<RedSocialPostulante> {
        val results = db.executeQuery("SELECT * FROM RED_SOCIAL_POSTULANTE")
        return results.map { row ->
            RedSocialPostulante(
                id_red_postualnte = row[0] as Int,
                id_postulante = row[1] as? Int,
                id_red_social = row[2] as? Int,
                url_perfil = row[3] as? String
            )
        }
    }

    fun getById(id: Int): RedSocialPostulante? {
        val results = db.getById("RED_SOCIAL_POSTULANTE", "ID_RED_POSTUALNTE", id)
        return results.firstOrNull()?.let { row ->
            RedSocialPostulante(
                id_red_postualnte = row[0] as Int,
                id_postulante = row[1] as? Int,
                id_red_social = row[2] as? Int,
                url_perfil = row[3] as? String
            )
        }
    }

    fun getByPostulante(postulanteId: Int): List<RedSocialPostulante> {
        val results = db.executeQuery("SELECT * FROM RED_SOCIAL_POSTULANTE WHERE ID_POSTULANTE = $postulanteId")
        return results.map { row ->
            RedSocialPostulante(
                id_red_postualnte = row[0] as Int,
                id_postulante = row[1] as? Int,
                id_red_social = row[2] as? Int,
                url_perfil = row[3] as? String
            )
        }
    }

    fun search(query: String): List<RedSocialPostulante> {
        val results = db.search("RED_SOCIAL_POSTULANTE", "URL_PERFIL", query)
        return results.map { row ->
            RedSocialPostulante(
                id_red_postualnte = row[0] as Int,
                id_postulante = row[1] as? Int,
                id_red_social = row[2] as? Int,
                url_perfil = row[3] as? String
            )
        }
    }

    fun insert(data: RedSocialPostulante): Long {
        val postulanteId = data.id_postulante ?: "NULL"
        val redId = data.id_red_social ?: "NULL"
        val url = if (data.url_perfil != null) "'${data.url_perfil}'" else "NULL"
        val query = "INSERT INTO RED_SOCIAL_POSTULANTE (ID_RED_POSTUALNTE, ID_POSTULANTE, ID_RED_SOCIAL, URL_PERFIL) VALUES (${data.id_red_postualnte}, $postulanteId, $redId, $url)"
        return db.executeInsert(query)
    }

    fun update(data: RedSocialPostulante): Int {
        val postulanteId = data.id_postulante ?: "NULL"
        val redId = data.id_red_social ?: "NULL"
        val url = if (data.url_perfil != null) "'${data.url_perfil}'" else "NULL"
        val query = "UPDATE RED_SOCIAL_POSTULANTE SET ID_POSTULANTE = $postulanteId, ID_RED_SOCIAL = $redId, URL_PERFIL = $url WHERE ID_RED_POSTUALNTE = ${data.id_red_postualnte}"
        return db.executeUpdate(query)
    }

    fun delete(id: Int): Int = db.deleteById("RED_SOCIAL_POSTULANTE", "ID_RED_POSTUALNTE", id)

    fun getCount(): Int = db.getCount("RED_SOCIAL_POSTULANTE")
}