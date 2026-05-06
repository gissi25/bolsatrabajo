package sv.ues.fia.eisi.bt.data.local.entities

data class Habilidad(
    val id_habilidad: String,
    val id_categoria_habilidad: Int,
    val nombre_habilidad: String,
    val nombre_categoria: String? = null
)
