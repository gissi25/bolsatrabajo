package sv.ues.fia.eisi.bt.data.local.entities

data class Empresa(
    val id_empresa: Int,
    val id_distrito: Int,
    val nombre_empresa: String,
    val contacto_directo: String,
    val nit: String
)