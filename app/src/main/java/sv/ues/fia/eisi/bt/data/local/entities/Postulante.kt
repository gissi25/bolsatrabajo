package sv.ues.fia.eisi.bt.data.local.entities

data class Postulante(
    val id_postulante: Int,
    val id_usuario: Int?,
    val id_genero: Int,
    val id_distrito: Int?,
    val id_tipo_documento: Int,
    val nombre: String,
    val apellido: String,
    val fecha_nacimiento: String?,
    val num_documento: String?,
    val nup: String?,
    val direccion_detalle: String?,
    val telefono_casa: String?,
    val telefono_celular: String?,
    val email: String?
)