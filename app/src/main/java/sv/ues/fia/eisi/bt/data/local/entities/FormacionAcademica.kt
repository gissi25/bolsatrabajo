package sv.ues.fia.eisi.bt.data.local.entities

data class FormacionAcademica(
    val id_formacion: Int,
    val id_postulante: Int,
    val id_oferta_academica: Int?,
    val titulo_obtenido: String,
    val fecha_obtencion: String?
)