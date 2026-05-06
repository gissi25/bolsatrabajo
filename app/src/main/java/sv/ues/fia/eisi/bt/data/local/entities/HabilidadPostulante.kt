package sv.ues.fia.eisi.bt.data.local.entities

data class HabilidadPostulante(
    val id_categoria_habilidad: Int,
    val id_habilidad: String,
    val id_postulante: String,
    val nivel_destreza: Int?
)
