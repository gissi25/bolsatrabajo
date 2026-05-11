package sv.ues.fia.eisi.bt.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, pattern = "^[A-Z]{2}\\d{5}$", friendlyName = "Codigo postulante", maxLength = 7),
                "NOMBRE" to FieldRule(field = "NOMBRE", required = true, friendlyName = "Nombre"),
                "APELLIDO" to FieldRule(field = "APELLIDO", required = true, friendlyName = "Apellido"),
                "NUM_DOCUMENTO" to FieldRule(field = "NUM_DOCUMENTO", required = true, friendlyName = "Numero de documento"),
                "EMAIL" to FieldRule(field = "EMAIL", required = true, pattern = "^[^@]+@[^@]+\\.[^@]+$", friendlyName = "Correo electronico"),
                "FECHA_NACIMIENTO" to FieldRule(field = "FECHA_NACIMIENTO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de nacimiento"),
                "ID_GENERO" to FieldRule(field = "ID_GENERO", required = true, friendlyName = "Genero"),
                "ID_TIPO_DOCUMENTO" to FieldRule(field = "ID_TIPO_DOCUMENTO", required = true, friendlyName = "Tipo de documento"),
                "ID_GRADO_ACADEMICO" to FieldRule(field = "ID_GRADO_ACADEMICO", required = true, friendlyName = "Grado academico"),
                "NUP" to FieldRule(field = "NUP", required = true, friendlyName = "NUP"),
                "DIRECCION_DETALLE" to FieldRule(field = "DIRECCION_DETALLE", required = true, friendlyName = "Direccion"),
                "TELEFONO_CASA" to FieldRule(field = "TELEFONO_CASA", required = true, friendlyName = "Telefono casa"),
                "TELEFONO_CELULAR" to FieldRule(field = "TELEFONO_CELULAR", required = true, friendlyName = "Telefono celular")
            )
            "EMPRESA" -> mapOf(
                "NIT" to FieldRule(field = "NIT", required = true, minLength = 14, maxLength = 14, pattern = "^\\d{14}$", friendlyName = "NIT"),
                "NOMBRE_EMPRESA" to FieldRule(field = "NOMBRE_EMPRESA", required = true, friendlyName = "Nombre de empresa"),
                "CONTACTO_DIRECTO" to FieldRule(field = "CONTACTO_DIRECTO", required = true, minLength = 9, maxLength = 9, pattern = "^\\d{4}-\\d{4}$", friendlyName = "Contacto directo")
            )
            "OFERTA_TRABAJO" -> mapOf(
                "ID_OFERTA" to FieldRule(field = "ID_OFERTA", required = true, pattern = "^OF\\d{2,}$", friendlyName = "Codigo oferta", maxLength = 10),
                "TITULO_PUESTO" to FieldRule(field = "TITULO_PUESTO", required = true, friendlyName = "Titulo del puesto"),
                "FECHA_PUBLICACION" to FieldRule(field = "FECHA_PUBLICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de publicacion"),
                "FECHA_CADUCIDAD" to FieldRule(field = "FECHA_CADUCIDAD", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de caducidad"),
                "EXPERIENCIA_ANIOS" to FieldRule(field = "EXPERIENCIA_ANIOS", required = true, friendlyName = "Anios de experiencia"),
                "EDAD_MINIMA" to FieldRule(field = "EDAD_MINIMA", min = 18, max = 100, friendlyName = "Edad minima"),
                "EDAD_MAXIMA" to FieldRule(field = "EDAD_MAXIMA", min = 16, max = 100, friendlyName = "Edad maxima"),
                "DESCRIPCION_OFERTA_TRABAJO" to FieldRule(field = "DESCRIPCION_OFERTA_TRABAJO", required = true, friendlyName = "Descripcion de la oferta")
            )
            "CATEGORIA_HABILIDAD" -> mapOf(
                "NOMBRE_CATEGORIA" to FieldRule(field = "NOMBRE_CATEGORIA", required = true, friendlyName = "Nombre de categoria")
            )
            "GENERO" -> mapOf(
                "NOMBRE_GENERO" to FieldRule(field = "NOMBRE_GENERO", required = true, friendlyName = "Nombre de genero")
            )
            "TIPO_DOCUMENTO" -> mapOf(
                "NOMBRE_TIPO" to FieldRule(field = "NOMBRE_TIPO", required = true, friendlyName = "Nombre de tipo")
            )
            "DEPARTAMENTO" -> mapOf(
                "NOMBRE_DEPARTAMENTO" to FieldRule(field = "NOMBRE_DEPARTAMENTO", required = true, friendlyName = "Nombre de departamento")
            )
            "MUNICIPIO" -> mapOf(
                "ID_MUNICIPIO" to FieldRule(field = "ID_MUNICIPIO", required = true, friendlyName = "Codigo municipio"),
                "NOMBRE_MUNICIPIO" to FieldRule(field = "NOMBRE_MUNICIPIO", required = true, friendlyName = "Nombre de municipio")
            )
            "DISTRITO" -> mapOf(
                "ID_DISTRITO" to FieldRule(field = "ID_DISTRITO", required = true, friendlyName = "Codigo distrito"),
                "NOMBRE_DISTRITO" to FieldRule(field = "NOMBRE_DISTRITO", required = true, friendlyName = "Nombre de distrito")
            )
            "INSTITUCION" -> mapOf(
                "ID_INSTITUCION" to FieldRule(field = "ID_INSTITUCION", required = true, pattern = "^[A-Za-z]{2,}\\d{2,}$", friendlyName = "Codigo institucion", maxLength = 20),
                "NOMBRE_INSTITUCION" to FieldRule(field = "NOMBRE_INSTITUCION", required = true, friendlyName = "Nombre de institucion")
            )
            "GRADO_ACADEMICO" -> mapOf(
                "NOMBRE_GRADO" to FieldRule(field = "NOMBRE_GRADO", required = true, friendlyName = "Nombre de grado")
            )
            "RED_SOCIAL" -> mapOf(
                "NOMBRE_RED" to FieldRule(field = "NOMBRE_RED", required = true, friendlyName = "Nombre de red social")
            )
            "HABILIDAD" -> mapOf(
                "ID_CATEGORIA_HABILIDAD" to FieldRule(field = "ID_CATEGORIA_HABILIDAD", required = true, friendlyName = "Categoria"),
                "ID_HABILIDAD" to FieldRule(field = "ID_HABILIDAD", required = true, pattern = "^H\\d{2,}$", friendlyName = "Codigo habilidad", maxLength = 10),
                "NOMBRE_HABILIDAD" to FieldRule(field = "NOMBRE_HABILIDAD", required = true, friendlyName = "Nombre de habilidad")
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "NIVEL_DESTREZA" to FieldRule(field = "NIVEL_DESTREZA", required = true, friendlyName = "Nivel de destreza")
            )
            "CERTIFICACION" -> mapOf(
                "ID_CERTIFICACION" to FieldRule(field = "ID_CERTIFICACION", required = true, pattern = "^C\\d{3,}$", friendlyName = "Codigo certificacion", maxLength = 10),
                "NOMBRE_CERTIFICACION" to FieldRule(field = "NOMBRE_CERTIFICACION", required = true, friendlyName = "Nombre de certificacion"),
                "FECHA_CERTIFICACION" to FieldRule(field = "FECHA_CERTIFICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de certificacion"),
                "PERIODO" to FieldRule(field = "PERIODO", required = true, pattern = "^\\d{2}/\\d{2}/\\d{2}--\\d{2}/\\d{2}/\\d{2}$", friendlyName = "Periodo")
            )
            "TIPO_CERTIFICACION" -> mapOf(
                "NOMBRE_TIPO" to FieldRule(field = "NOMBRE_TIPO", required = true, friendlyName = "Nombre de tipo")
            )
            "EXPERIENCIA_LABORAL" -> mapOf(
                "ID_EXPERIENCIA" to FieldRule(field = "ID_EXPERIENCIA", required = true, pattern = "^EL\\d{2,}$", friendlyName = "Codigo experiencia", maxLength = 10),
                "PUESTO_TRABAJO" to FieldRule(field = "PUESTO_TRABAJO", required = true, friendlyName = "Puesto de trabajo"),
                "FECHA_INICIO" to FieldRule(field = "FECHA_INICIO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de inicio"),
                "FECHA_FIN" to FieldRule(field = "FECHA_FIN", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de fin"),
                "DESCP_EXPERIENCIA_LABORAL" to FieldRule(field = "DESCP_EXPERIENCIA_LABORAL", required = true, friendlyName = "Descripcion de experiencia"),
                "CONTACTO_REFERENCIA" to FieldRule(field = "CONTACTO_REFERENCIA", required = true, friendlyName = "Contacto de referencia")
            )
            "FORMACION_ACADEMICA" -> mapOf(
                "ID_FORMACION" to FieldRule(field = "ID_FORMACION", required = true, pattern = "^FOA\\d{3,}$", friendlyName = "Codigo formacion", maxLength = 10),
                "TITULO_OBTENIDO" to FieldRule(field = "TITULO_OBTENIDO", required = true, friendlyName = "Titulo obtenido"),
                "PERIODO" to FieldRule(field = "PERIODO", required = true, pattern = "^\\d{2}/\\d{2}/\\d{2}--\\d{2}/\\d{2}/\\d{2}$", friendlyName = "Periodo"),
                "FECHA_OBTENCION" to FieldRule(field = "FECHA_OBTENCION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de obtencion")
            )
            "OFERTA_ACADEMICA" -> mapOf(
                "ID_OFERTA_ACADEMICA" to FieldRule(field = "ID_OFERTA_ACADEMICA", required = true, pattern = "^OFA\\d{2,}$", friendlyName = "Codigo oferta academica", maxLength = 10)
            )
            "POSTULACION" -> mapOf(
                "ID_POSTULACION" to FieldRule(field = "ID_POSTULACION", required = true, pattern = "^POS\\d{3,}$", friendlyName = "Codigo postulacion", maxLength = 10),
                "FECHA_APLICACION" to FieldRule(field = "FECHA_APLICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyName = "Fecha de aplicacion"),
                "ESTADO_PROCESO" to FieldRule(field = "ESTADO_PROCESO", required = true, friendlyName = "Estado del proceso")
            )
            "DETALLE_REQUISITO" -> mapOf(
                "ID_DETALLE" to FieldRule(field = "ID_DETALLE", required = true, pattern = "^D\\d{1,}$", friendlyName = "Codigo detalle", maxLength = 10),
                "DESCRIPCION_REQUISITO" to FieldRule(field = "DESCRIPCION_REQUISITO", required = true, friendlyName = "Descripcion del requisito")
            )
            "RED_SOCIAL_POSTULANTE" -> mapOf(
                "URL_PERFIL" to FieldRule(field = "URL_PERFIL", required = true, pattern = "^https?://.*", friendlyName = "URL del perfil")
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
            return "${rules.friendlyName} no tiene un formato valido"
        }

        val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val today = utcFormat.format(Date())

        val futureDateTables = mapOf(
            "CERTIFICACION" to "FECHA_CERTIFICACION",
            "EXPERIENCIA_LABORAL" to "FECHA_FIN",
            "POSTULACION" to "FECHA_APLICACION",
            "OFERTA_TRABAJO" to "FECHA_PUBLICACION",
            "FORMACION_ACADEMICA" to "FECHA_OBTENCION"
        )
        if (futureDateTables[tableName] == column && trimmed > today) {
            return "${rules.friendlyName} no puede ser una fecha futura"
        }

        if (rules.min != null) {
            val num = trimmed.toIntOrNull()
            if (num == null) return "${rules.friendlyName} debe ser un numero"
            if (num < rules.min) return "${rules.friendlyName} debe ser mayor o igual a ${rules.min}"
        }

        if (rules.max != null) {
            val num = trimmed.toIntOrNull()
            if (num != null && num > rules.max) return "${rules.friendlyName} debe ser menor o igual a ${rules.max}"
        }

        return null
    }

}
