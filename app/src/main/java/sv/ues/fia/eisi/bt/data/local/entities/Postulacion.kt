package sv.ues.fia.eisi.bt.data.local.entities

data class Postulacion(
    val id_empresa: Int,
    val id_oferta: Int,
    val id_postulante: Int,
    val id_postulacion: Int,
    val fecha_aplicacion: String?,
    val estado_proceso: String?
)