package sv.ues.fia.eisi.bt.data.local.entities

data class FormacionAcademica(
    val id_formacion: String,
    val id_postulante: String,
    val id_oferta_academica: String?,
    val titulo_obtenido: String,
    val fecha_obtencion: String?
)
