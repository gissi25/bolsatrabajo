package sv.ues.fia.eisi.bt.utils

object ValidationRules {

    data class FieldRule(
        val field: String,
        val required: Boolean = false,
        val minLength: Int = 0,
        val maxLength: Int = 0,
        val pattern: String? = null,
        val min: Int? = null,
        val max: Int? = null,
        val friendlyName: String
    )

    fun getRulesForTable(tableName: String): Map<String, FieldRule> {
        return when (tableName) {
            "USUARIO" -> mapOf(
                "USERNAME" to FieldRule(field = "USERNAME", required = true, minLength = 3, friendlyName = "Usuario"),
                "PASSWORD" to FieldRule(field = "PASSWORD", required = true, minLength = 8, friendlyName = "Contraseña"),
                "ROL" to FieldRule(field = "ROL", required = true, friendlyName = "Rol")
            )
            "POSTULANTE" -> mapOf(
                "NOMBRE" to FieldRule(field = "NOMBRE", required = true, friendlyName = "Nombre"),
                "APELLIDO" to FieldRule(field = "APELLIDO", required = true, friendlyName = "Apellido"),
                "NUM_DOCUMENTO" to FieldRule(field = "NUM_DOCUMENTO", required = true, friendlyName = "Número de documento"),
                "EMAIL" to FieldRule(field = "EMAIL", required = true, pattern = "^[^@]+@[^@]+\\.[^@]+$", friendlyName = "Correo electrónico"),
                "FECHA_NACIMIENTO" to FieldRule(field = "FECHA_NACIMIENTO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de nacimiento"),
                "ID_GENERO" to FieldRule(field = "ID_GENERO", required = true, friendlyName = "Género"),
                "ID_TIPO_DOCUMENTO" to FieldRule(field = "ID_TIPO_DOCUMENTO", required = true, friendlyName = "Tipo de documento")
            )
            "EMPRESA" -> mapOf(
                "NOMBRE_EMPRESA" to FieldRule(field = "NOMBRE_EMPRESA", required = true, friendlyName = "Nombre de empresa"),
                "NIT" to FieldRule(field = "NIT", required = true, minLength = 14, maxLength = 14, pattern = "^\\d{14}$", friendlyName = "NIT"),
                "CONTACTO_DIRECTO" to FieldRule(field = "CONTACTO_DIRECTO", required = true, minLength = 9, maxLength = 9, pattern = "^\\d{4}-\\d{4}$", friendlyName = "Contacto directo"),
                "ID_DISTRITO" to FieldRule(field = "ID_DISTRITO", required = true, friendlyName = "Distrito")
            )
            "OFERTA_TRABAJO" -> mapOf(
                "TITULO_PUESTO" to FieldRule(field = "TITULO_PUESTO", required = true, friendlyName = "Título del puesto"),
                "FECHA_PUBLICACION" to FieldRule(field = "FECHA_PUBLICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de publicación"),
                "FECHA_CADUCIDAD" to FieldRule(field = "FECHA_CADUCIDAD", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de caducidad"),
                "EDAD_MINIMA" to FieldRule(field = "EDAD_MINIMA", min = 16, max = 100, friendlyName = "Edad mínima"),
                "EDAD_MAXIMA" to FieldRule(field = "EDAD_MAXIMA", min = 16, max = 100, friendlyName = "Edad máxima"),
                "ID_EMPRESA" to FieldRule(field = "ID_EMPRESA", required = true, friendlyName = "Empresa")
            )
            "CATEGORIA_HABILIDAD" -> mapOf(
                "NOMBRE_CATEGORIA" to FieldRule(field = "NOMBRE_CATEGORIA", required = true, friendlyName = "Nombre de categoría")
            )
            "GENERO" -> mapOf(
                "NOMBRE_GENERO" to FieldRule(field = "NOMBRE_GENERO", required = true, friendlyName = "Nombre de género")
            )
            "TIPO_DOCUMENTO" -> mapOf(
                "NOMBRE_TIPO" to FieldRule(field = "NOMBRE_TIPO", required = true, friendlyName = "Nombre de tipo")
            )
            "DEPARTAMENTO" -> mapOf(
                "NOMBRE_DEPARTAMENTO" to FieldRule(field = "NOMBRE_DEPARTAMENTO", required = true, friendlyName = "Nombre de departamento")
            )
            "MUNICIPIO" -> mapOf(
                "NOMBRE_MUNICIPIO" to FieldRule(field = "NOMBRE_MUNICIPIO", required = true, friendlyName = "Nombre de municipio"),
                "ID_DEPARTAMENTO" to FieldRule(field = "ID_DEPARTAMENTO", required = true, friendlyName = "Departamento")
            )
            "DISTRITO" -> mapOf(
                "NOMBRE_DISTRITO" to FieldRule(field = "NOMBRE_DISTRITO", required = true, friendlyName = "Nombre de distrito"),
                "ID_MUNICIPIO" to FieldRule(field = "ID_MUNICIPIO", required = true, friendlyName = "Municipio")
            )
            "INSTITUCION" -> mapOf(
                "NOMBRE_INSTITUCION" to FieldRule(field = "NOMBRE_INSTITUCION", required = true, friendlyName = "Nombre de institución")
            )
            "GRADO_ACADEMICO" -> mapOf(
                "NOMBRE_GRADO" to FieldRule(field = "NOMBRE_GRADO", required = true, friendlyName = "Nombre de grado")
            )
            "RED_SOCIAL" -> mapOf(
                "NOMBRE_RED" to FieldRule(field = "NOMBRE_RED", required = true, friendlyName = "Nombre de red social")
            )
            "HABILIDAD" -> mapOf(
                "NOMBRE_HABILIDAD" to FieldRule(field = "NOMBRE_HABILIDAD", required = true, friendlyName = "Nombre de habilidad"),
                "ID_CATEGORIA_HABILIDAD" to FieldRule(field = "ID_CATEGORIA_HABILIDAD", required = true, friendlyName = "Categoría")
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "NIVEL_DESTREZA" to FieldRule(field = "NIVEL_DESTREZA", required = true, min = 1, max = 3, friendlyName = "Nivel de destreza"),
                "ID_HABILIDAD" to FieldRule(field = "ID_HABILIDAD", required = true, friendlyName = "Habilidad"),
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante")
            )
            "CERTIFICACION" -> mapOf(
                "NOMBRE_CERTIFICACION" to FieldRule(field = "NOMBRE_CERTIFICACION", required = true, friendlyName = "Nombre de certificación"),
                "CODIGO_CERTIFICACION" to FieldRule(field = "CODIGO_CERTIFICACION", required = true, minLength = 14, maxLength = 14, pattern = "^\\d{14}$", friendlyName = "Código de certificación"),
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante"),
                "ID_INSTITUCION" to FieldRule(field = "ID_INSTITUCION", required = true, friendlyName = "Institución")
            )
            "EXPERIENCIA_LABORAL" -> mapOf(
                "PUESTO_TRABAJO" to FieldRule(field = "PUESTO_TRABAJO", required = true, friendlyName = "Puesto de trabajo"),
                "FECHA_INICIO" to FieldRule(field = "FECHA_INICIO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de inicio"),
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante")
            )
            "FORMACION_ACADEMICA" -> mapOf(
                "TITULO_OBTENIDO" to FieldRule(field = "TITULO_OBTENIDO", required = true, friendlyName = "Título obtenido"),
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante")
            )
            "OFERTA_ACADEMICA" -> mapOf(
                "ID_INSTITUCION" to FieldRule(field = "ID_INSTITUCION", required = true, friendlyName = "Institución"),
                "ID_GRADO_ACADEMICO" to FieldRule(field = "ID_GRADO_ACADEMICO", required = true, friendlyName = "Grado académico")
            )
            "POSTULACION" -> mapOf(
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante"),
                "ID_EMPRESA" to FieldRule(field = "ID_EMPRESA", required = true, friendlyName = "Empresa"),
                "ID_OFERTA" to FieldRule(field = "ID_OFERTA", required = true, friendlyName = "Oferta")
            )
            "DETALLE_REQUISITO" -> mapOf(
                "DESCRIPCION_REQUISITO" to FieldRule(field = "DESCRIPCION_REQUISITO", required = true, friendlyName = "Descripción del requisito")
            )
            "RED_SOCIAL_POSTULANTE" -> mapOf(
                "URL_PERFIL" to FieldRule(field = "URL_PERFIL", required = true, pattern = "^https?://.*", friendlyName = "URL del perfil"),
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, friendlyName = "Postulante"),
                "ID_RED_SOCIAL" to FieldRule(field = "ID_RED_SOCIAL", required = true, friendlyName = "Red social")
            )
            else -> emptyMap()
        }
    }

    fun validate(tableName: String, column: String, value: String): String? {
        val rules = getRulesForTable(tableName)[column] ?: return null
        val trimmed = value.trim()

        if (rules.required && trimmed.isEmpty()) {
            return "${rules.friendlyName} es obligatorio"
        }

        if (trimmed.isEmpty()) return null

        if (rules.minLength > 0 && trimmed.length < rules.minLength) {
            return "${rules.friendlyName} debe tener al menos ${rules.minLength} caracteres"
        }

        if (rules.maxLength > 0 && trimmed.length > rules.maxLength) {
            return "${rules.friendlyName} no debe exceder ${rules.maxLength} caracteres"
        }

        if (rules.pattern != null && !Regex(rules.pattern).matches(trimmed)) {
            return "${rules.friendlyName} no tiene un formato válido"
        }

        if (rules.min != null) {
            val num = trimmed.toIntOrNull()
            if (num == null) return "${rules.friendlyName} debe ser un número"
            if (num < rules.min) return "${rules.friendlyName} debe ser mayor o igual a ${rules.min}"
        }

        if (rules.max != null) {
            val num = trimmed.toIntOrNull()
            if (num != null && num > rules.max) return "${rules.friendlyName} debe ser menor o igual a ${rules.max}"
        }

        return null
    }

    fun getAutoIncrementColumns(): Set<String> {
        return setOf(
            "ID_USUARIO", "ID_POSTULANTE", "ID_GENERO", "ID_TIPO_DOCUMENTO",
            "ID_DEPARTAMENTO", "ID_MUNICIPIO", "ID_DISTRITO", "ID_INSTITUCION",
            "ID_GRADO_ACADEMICO", "ID_RED_SOCIAL", "ID_CATEGORIA_HABILIDAD",
            "ID_HABILIDAD", "ID_EMPRESA", "ID_OFERTA_ACADEMICA", "ID_OFERTA",
            "ID_CERTIFICACION", "ID_EXPERIENCIA", "ID_FORMACION",
            "ID_HABILIDAD_POSTULANTE", "ID_POSTULACION", "ID_DETALLE",
            "ID_RED_POSTUALNTE"
        )
    }

    fun isAutoIncrement(column: String): Boolean {
        return column.uppercase() in getAutoIncrementColumns()
    }
}