package sv.ues.fia.eisi.bt.data.local.entities

data class ExperienciaLaboral(
    val id_postulante: Int,
    val id_experiencia: Int,
    val id_empresa: Int?,
    val puesto_trabajo: String,
    val fecha_inicio: String?,
    val fecha_fin: String?,
    val des_exp_laboral: String?,
    val contacto_referencia: String?
)