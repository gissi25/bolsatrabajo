package sv.ues.fia.eisi.bt.data.local.entities

data class Usuario(
    val id_usuario: Int,
    val username: String,
    val password: String,
    val rol: String
)