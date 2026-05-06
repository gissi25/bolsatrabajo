package sv.ues.fia.eisi.bt.data.local.entities

data class Certificacion(
    val id_certificacion: String,
    val id_institucion: String,
    val id_postulante: String,
    val nombre_certificacion: String,
    val fecha_certificacion: String?
)
