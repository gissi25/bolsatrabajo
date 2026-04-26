package sv.ues.fia.eisi.bt.data.local.entities

data class DetalleRequisito(
    val id_detalle: Int,
    val id_empresa: Int?,
    val id_oferta: Int?,
    val descripcion_requisito: String
)