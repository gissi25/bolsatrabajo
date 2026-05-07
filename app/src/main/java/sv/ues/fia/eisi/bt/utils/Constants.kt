package sv.ues.fia.eisi.bt.utils

object Constants {
    const val DATABASE_NAME = "bolsadetabajo.db"
    const val DATABASE_VERSION = 10

    const val PREFS_NAME = "bolsa_trabajo_prefs"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_USER_ROLE = "user_role"

    const val ROLE_ADMIN = "administrador"
    const val ROLE_POSTULANTE = "postulante"
    const val ROLE_EMPRESA = "gerente de empresa"

    const val TABLE_CATEGORIA_HABILIDAD = "CATEGORIA_HABILIDAD"
    const val TABLE_GENERO = "GENERO"
    const val TABLE_TIPO_DOCUMENTO = "TIPO_DOCUMENTO"
    const val TABLE_DEPARTAMENTO = "DEPARTAMENTO"
    const val TABLE_INSTITUCION = "INSTITUCION"
    const val TABLE_GRADO_ACADEMICO = "GRADO_ACADEMICO"
    const val TABLE_RED_SOCIAL = "RED_SOCIAL"
    const val TABLE_MUNICIPIO = "MUNICIPIO"
    const val TABLE_DISTRITO = "DISTRITO"
    const val TABLE_HABILIDAD = "HABILIDAD"
    const val TABLE_EMPRESA = "EMPRESA"
    const val TABLE_OFERTA_ACADEMICA = "OFERTA_ACADEMICA"
    const val TABLE_POSTULANTE = "POSTULANTE"
    const val TABLE_USUARIO = "USUARIO"
    const val TABLE_CERTIFICACION = "CERTIFICACION"
    const val TABLE_OFERTA_TRABAJO = "OFERTA_TRABAJO"
    const val TABLE_DETALLE_REQUISITO = "DETALLE_REQUISITO"
    const val TABLE_EXPERIENCIA_LABORAL = "EXPERIENCIA_LABORAL"
    const val TABLE_FORMACION_ACADEMICA = "FORMACION_ACADEMICA"
    const val TABLE_HABILIDAD_POSTULANTE = "HABILIDAD_POSTULANTE"
    const val TABLE_POSTULACION = "POSTULACION"
    const val TABLE_RED_SOCIAL_POSTULANTE = "RED_SOCIAL_POSTULANTE"

    val ALL_TABLES = listOf(
        TABLE_CATEGORIA_HABILIDAD, TABLE_GENERO, TABLE_TIPO_DOCUMENTO,
        TABLE_DEPARTAMENTO, TABLE_INSTITUCION, TABLE_GRADO_ACADEMICO,
        TABLE_RED_SOCIAL, TABLE_MUNICIPIO, TABLE_DISTRITO,
        TABLE_HABILIDAD, TABLE_EMPRESA, TABLE_OFERTA_ACADEMICA,
        TABLE_POSTULANTE, TABLE_USUARIO, TABLE_CERTIFICACION,
        TABLE_OFERTA_TRABAJO, TABLE_DETALLE_REQUISITO, TABLE_EXPERIENCIA_LABORAL,
        TABLE_FORMACION_ACADEMICA, TABLE_HABILIDAD_POSTULANTE, TABLE_POSTULACION,
        TABLE_RED_SOCIAL_POSTULANTE
    )

    const val BUNDLE_TABLE_NAME = "table_name"
    const val BUNDLE_TABLE_DATA = "table_data"
    const val BUNDLE_IS_EDIT_MODE = "is_edit_mode"
    const val BUNDLE_IS_VIEW_MODE = "is_view_mode"
    const val BUNDLE_ITEM_ID = "item_id"

    enum class AccessLevel { NONE, READ_ONLY, FULL }

    fun getColumnsForTable(tableName: String): List<String> {
        return when (tableName) {
            "USUARIO" -> listOf("ID_USUARIO", "USERNAME", "PASSWORD", "ROL")
            "POSTULANTE" -> listOf("ID_POSTULANTE", "ID_GENERO", "ID_TIPO_DOCUMENTO", "NUM_DOCUMENTO", "ID_DISTRITO_DEPTO", "ID_DISTRITO_MUNICIPIO", "ID_DISTRITO_ID", "NOMBRE", "APELLIDO", "FECHA_NACIMIENTO", "NUP", "DIRECCION_DETALLE", "TELEFONO_CASA", "TELEFONO_CELULAR", "EMAIL")
            "GENERO" -> listOf("ID_GENERO", "NOMBRE_GENERO")
            "TIPO_DOCUMENTO" -> listOf("ID_TIPO_DOCUMENTO", "NOMBRE_TIPO")
            "DEPARTAMENTO" -> listOf("ID_DEPARTAMENTO", "NOMBRE_DEPARTAMENTO")
            "MUNICIPIO" -> listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO", "NOMBRE_MUNICIPIO")
            "DISTRITO" -> listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO", "ID_DISTRITO", "NOMBRE_DISTRITO")
            "HABILIDAD" -> listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "NOMBRE_HABILIDAD")
            "CATEGORIA_HABILIDAD" -> listOf("ID_CATEGORIA_HABILIDAD", "NOMBRE_CATEGORIA")
            "EMPRESA" -> listOf("NIT", "ID_DISTRITO_DEPTO", "ID_DISTRITO_MUNICIPIO", "ID_DISTRITO_ID", "NOMBRE_EMPRESA", "CONTACTO_DIRECTO")
            "INSTITUCION" -> listOf("ID_INSTITUCION", "NOMBRE_INSTITUCION")
            "GRADO_ACADEMICO" -> listOf("ID_GRADO_ACADEMICO", "NOMBRE_GRADO")
            "RED_SOCIAL" -> listOf("ID_RED_SOCIAL", "NOMBRE_RED")
            "OFERTA_ACADEMICA" -> listOf("ID_OFERTA_ACADEMICA", "ID_GRADO_ACADEMICO", "ID_INSTITUCION")
            "OFERTA_TRABAJO" -> listOf("NIT", "ID_OFERTA", "ID_GRADO_ACADEMICO", "TITULO_PUESTO", "FECHA_PUBLICACION", "FECHA_CADUCIDAD", "EXPERIENCIA_ANIOS", "EDAD_MINIMA", "EDAD_MAXIMA", "DESCRIPCION_OFERTA_TRABAJO")
            "CERTIFICACION" -> listOf("ID_CERTIFICACION", "ID_INSTITUCION", "ID_POSTULANTE", "NOMBRE_CERTIFICACION", "FECHA_CERTIFICACION")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE", "NIT", "ID_EXPERIENCIA", "PUESTO_TRABAJO", "FECHA_INICIO", "FECHA_FIN", "DESCP_EXPERIENCIA_LABORAL", "CONTACTO_REFERENCIA")
            "FORMACION_ACADEMICA" -> listOf("ID_FORMACION", "ID_POSTULANTE", "ID_OFERTA_ACADEMICA", "TITULO_OBTENIDO", "FECHA_OBTENCION")
            "HABILIDAD_POSTULANTE" -> listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "ID_POSTULANTE", "NIVEL_DESTREZA")
            "POSTULACION" -> listOf("ID_POSTULACION", "NIT", "ID_OFERTA", "ID_POSTULANTE", "FECHA_APLICACION", "ESTADO_PROCESO")
            "DETALLE_REQUISITO" -> listOf("NIT", "ID_OFERTA", "ID_DETALLE", "DESCRIPCION_REQUISITO")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_POSTULANTE", "ID_RED_SOCIAL", "URL_PERFIL")
            else -> listOf("ID", "NOMBRE")
        }
    }

    fun getPrimaryKeyColumns(tableName: String): List<String> {
        return when (tableName) {
            "MUNICIPIO" -> listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO")
            "DISTRITO" -> listOf("ID_DEPARTAMENTO", "ID_MUNICIPIO", "ID_DISTRITO")
            "EMPRESA" -> listOf("NIT")
            "OFERTA_TRABAJO" -> listOf("NIT", "ID_OFERTA")
            "DETALLE_REQUISITO" -> listOf("NIT", "ID_OFERTA", "ID_DETALLE")
            "EXPERIENCIA_LABORAL" -> listOf("ID_POSTULANTE", "NIT", "ID_EXPERIENCIA")
            "CERTIFICACION" -> listOf("ID_CERTIFICACION", "ID_INSTITUCION", "ID_POSTULANTE")
            "FORMACION_ACADEMICA" -> listOf("ID_FORMACION", "ID_POSTULANTE")
            "HABILIDAD_POSTULANTE" -> listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD", "ID_POSTULANTE")
            "POSTULACION" -> listOf("ID_POSTULACION")
            "RED_SOCIAL_POSTULANTE" -> listOf("ID_POSTULANTE", "ID_RED_SOCIAL")
            "USUARIO" -> listOf("ID_USUARIO")
            "POSTULANTE" -> listOf("ID_POSTULANTE")
            "HABILIDAD" -> listOf("ID_CATEGORIA_HABILIDAD", "ID_HABILIDAD")
            "INSTITUCION" -> listOf("ID_INSTITUCION")
            "OFERTA_ACADEMICA" -> listOf("ID_OFERTA_ACADEMICA")
            else -> listOf(getAutoGenColumn(tableName) ?: "ID")
        }
    }

    fun getAutoGenColumn(tableName: String): String? {
        return when (tableName) {
            "GENERO" -> "ID_GENERO"
            "TIPO_DOCUMENTO" -> "ID_TIPO_DOCUMENTO"
            "DEPARTAMENTO" -> "ID_DEPARTAMENTO"
            "GRADO_ACADEMICO" -> "ID_GRADO_ACADEMICO"
            "RED_SOCIAL" -> "ID_RED_SOCIAL"
            "CATEGORIA_HABILIDAD" -> "ID_CATEGORIA_HABILIDAD"
            "USUARIO" -> "ID_USUARIO"
            else -> null
        }
    }

    fun getRoleTables(role: String): Map<String, AccessLevel> {
        return when (role) {
            ROLE_ADMIN -> ALL_TABLES.associateWith { AccessLevel.FULL }
            ROLE_POSTULANTE -> mapOf(
                TABLE_POSTULANTE to AccessLevel.FULL,
                TABLE_EXPERIENCIA_LABORAL to AccessLevel.FULL,
                TABLE_FORMACION_ACADEMICA to AccessLevel.FULL,
                TABLE_HABILIDAD_POSTULANTE to AccessLevel.FULL,
                TABLE_CERTIFICACION to AccessLevel.FULL,
                TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.FULL,
                TABLE_POSTULACION to AccessLevel.FULL,
                TABLE_EMPRESA to AccessLevel.READ_ONLY,
                TABLE_OFERTA_TRABAJO to AccessLevel.READ_ONLY,
                TABLE_DETALLE_REQUISITO to AccessLevel.READ_ONLY
            )
            ROLE_EMPRESA -> mapOf(
                TABLE_EMPRESA to AccessLevel.FULL,
                TABLE_OFERTA_TRABAJO to AccessLevel.FULL,
                TABLE_DETALLE_REQUISITO to AccessLevel.FULL,
                TABLE_POSTULACION to AccessLevel.FULL,
                TABLE_POSTULANTE to AccessLevel.READ_ONLY,
                TABLE_EXPERIENCIA_LABORAL to AccessLevel.READ_ONLY,
                TABLE_FORMACION_ACADEMICA to AccessLevel.READ_ONLY,
                TABLE_HABILIDAD_POSTULANTE to AccessLevel.READ_ONLY,
                TABLE_CERTIFICACION to AccessLevel.READ_ONLY,
                TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.READ_ONLY
            )
            else -> emptyMap()
        }
    }
}
