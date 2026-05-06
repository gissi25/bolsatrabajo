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
    const val BUNDLE_ITEM_ID = "item_id"

    enum class AccessLevel { NONE, READ_ONLY, FULL }

    fun getRoleTables(role: String): Map<String, AccessLevel> {
        return when (role) {
            ROLE_ADMIN -> ALL_TABLES.associateWith { AccessLevel.FULL }
            ROLE_POSTULANTE -> mapOf(
                TABLE_USUARIO to AccessLevel.FULL,
                TABLE_POSTULANTE to AccessLevel.FULL,
                TABLE_EXPERIENCIA_LABORAL to AccessLevel.FULL,
                TABLE_FORMACION_ACADEMICA to AccessLevel.FULL,
                TABLE_HABILIDAD_POSTULANTE to AccessLevel.FULL,
                TABLE_CERTIFICACION to AccessLevel.FULL,
                TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.FULL,
                TABLE_POSTULACION to AccessLevel.FULL,
                TABLE_EMPRESA to AccessLevel.READ_ONLY,
                TABLE_OFERTA_TRABAJO to AccessLevel.READ_ONLY,
                TABLE_DETALLE_REQUISITO to AccessLevel.READ_ONLY,
                TABLE_GENERO to AccessLevel.READ_ONLY,
                TABLE_TIPO_DOCUMENTO to AccessLevel.READ_ONLY,
                TABLE_GRADO_ACADEMICO to AccessLevel.READ_ONLY,
                TABLE_CATEGORIA_HABILIDAD to AccessLevel.READ_ONLY,
                TABLE_HABILIDAD to AccessLevel.READ_ONLY,
                TABLE_RED_SOCIAL to AccessLevel.READ_ONLY,
                TABLE_INSTITUCION to AccessLevel.READ_ONLY,
                TABLE_OFERTA_ACADEMICA to AccessLevel.READ_ONLY,
                TABLE_DEPARTAMENTO to AccessLevel.READ_ONLY,
                TABLE_MUNICIPIO to AccessLevel.READ_ONLY,
                TABLE_DISTRITO to AccessLevel.READ_ONLY
            )
            ROLE_EMPRESA -> mapOf(
                TABLE_USUARIO to AccessLevel.FULL,
                TABLE_EMPRESA to AccessLevel.FULL,
                TABLE_OFERTA_TRABAJO to AccessLevel.FULL,
                TABLE_DETALLE_REQUISITO to AccessLevel.FULL,
                TABLE_POSTULACION to AccessLevel.FULL,
                TABLE_POSTULANTE to AccessLevel.READ_ONLY,
                TABLE_EXPERIENCIA_LABORAL to AccessLevel.READ_ONLY,
                TABLE_FORMACION_ACADEMICA to AccessLevel.READ_ONLY,
                TABLE_HABILIDAD_POSTULANTE to AccessLevel.READ_ONLY,
                TABLE_CERTIFICACION to AccessLevel.READ_ONLY,
                TABLE_RED_SOCIAL_POSTULANTE to AccessLevel.READ_ONLY,
                TABLE_GRADO_ACADEMICO to AccessLevel.READ_ONLY,
                TABLE_HABILIDAD to AccessLevel.READ_ONLY,
                TABLE_CATEGORIA_HABILIDAD to AccessLevel.READ_ONLY,
                TABLE_INSTITUCION to AccessLevel.READ_ONLY,
                TABLE_DEPARTAMENTO to AccessLevel.READ_ONLY,
                TABLE_MUNICIPIO to AccessLevel.READ_ONLY,
                TABLE_DISTRITO to AccessLevel.READ_ONLY,
                TABLE_GENERO to AccessLevel.READ_ONLY,
                TABLE_TIPO_DOCUMENTO to AccessLevel.READ_ONLY,
                TABLE_RED_SOCIAL to AccessLevel.READ_ONLY,
                TABLE_OFERTA_ACADEMICA to AccessLevel.READ_ONLY
            )
            else -> emptyMap()
        }
    }
}
