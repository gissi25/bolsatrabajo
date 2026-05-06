package sv.ues.fia.eisi.bt.data.local.entities

data class Empresa(
    val nit: String,
    val id_distrito_depto: Int,
    val id_distrito_municipio: Int,
    val id_distrito_id: Int,
    val nombre_empresa: String,
    val contacto_directo: String
)
