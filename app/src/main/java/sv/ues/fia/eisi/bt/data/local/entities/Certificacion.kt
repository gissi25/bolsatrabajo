package sv.ues.fia.eisi.bt.data.local.entities

data class Certificacion(
    val id_postulante: Int,
    val id_certificacion: Int,
    val id_institucion: Int,
    val nombre_certificacion: String,
    val codigo_certificacion: String?,
    val fecha_certificacion: String?
)