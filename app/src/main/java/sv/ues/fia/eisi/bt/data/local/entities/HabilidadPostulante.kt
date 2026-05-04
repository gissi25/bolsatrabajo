package sv.ues.fia.eisi.bt.data.local.entities

data class HabilidadPostulante(
    val id_habilidad: Int,
    val id_postulante: Int,
    val id_habilidad_postulante: Int,
    val nivel_destreza: String?
)