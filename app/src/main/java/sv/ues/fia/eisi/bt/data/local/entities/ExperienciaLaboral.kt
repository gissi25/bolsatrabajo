package sv.ues.fia.eisi.bt.data.local.entities

data class ExperienciaLaboral(
    val id_postulante: String,
    val nit: String,
    val id_experiencia: String,
    val puesto_trabajo: String?,
    val fecha_inicio: String?,
    val fecha_fin: String?,
    val descp_experiencia_laboral: String?,
    val contacto_referencia: String?
)
