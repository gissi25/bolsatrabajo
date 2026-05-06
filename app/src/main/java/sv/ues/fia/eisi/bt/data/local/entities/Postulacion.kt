package sv.ues.fia.eisi.bt.data.local.entities

data class Postulacion(
    val id_postulacion: String,
    val nit: String,
    val id_oferta: String,
    val id_postulante: String,
    val fecha_aplicacion: String?,
    val estado_proceso: String?
)
