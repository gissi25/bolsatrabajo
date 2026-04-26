package sv.ues.fia.eisi.bt.data.local.entities

data class OfertaTrabajo(
    val id_empresa: Int,
    val id_oferta: Int,
    val id_grado_academico: Int?,
    val titulo_puesto: String,
    val fecha_publicacion: String?,
    val fecha_caducidad: String?,
    val experiencia_anios: Int?,
    val edad_minima: Int?,
    val edad_maxima: Int?,
    val descripcion_oferta_trabajo: String?
)