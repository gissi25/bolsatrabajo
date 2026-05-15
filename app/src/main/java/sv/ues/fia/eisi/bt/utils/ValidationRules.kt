package sv.ues.fia.eisi.bt.utils

import android.content.Context
import sv.ues.fia.eisi.bt.R
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
        val friendlyNameResId: Int
    )

    fun getRulesForTable(tableName: String): Map<String, FieldRule> {
        return when (tableName) {
            "USUARIO" -> mapOf(
                "USERNAME" to FieldRule(field = "USERNAME", required = true, minLength = 3, friendlyNameResId = R.string.friendly_usuario),
                "PASSWORD" to FieldRule(field = "PASSWORD", required = true, minLength = 8, friendlyNameResId = R.string.friendly_contrasena),
                "ROL" to FieldRule(field = "ROL", required = true, friendlyNameResId = R.string.friendly_rol)
            )
            "POSTULANTE" -> mapOf(
                "ID_POSTULANTE" to FieldRule(field = "ID_POSTULANTE", required = true, pattern = "^[A-Z]{2}\\d{5}$", friendlyNameResId = R.string.friendly_codigo_postulante, maxLength = 7),
                "NOMBRE" to FieldRule(field = "NOMBRE", required = true, friendlyNameResId = R.string.friendly_nombre),
                "APELLIDO" to FieldRule(field = "APELLIDO", required = true, friendlyNameResId = R.string.friendly_apellido),
                "NUM_DOCUMENTO" to FieldRule(field = "NUM_DOCUMENTO", required = true, friendlyNameResId = R.string.friendly_numero_documento),
                "EMAIL" to FieldRule(field = "EMAIL", required = true, pattern = "^[^@]+@[^@]+\\.[^@]+$", friendlyNameResId = R.string.friendly_correo_electronico),
                "FECHA_NACIMIENTO" to FieldRule(field = "FECHA_NACIMIENTO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_nacimiento),
                "ID_GENERO" to FieldRule(field = "ID_GENERO", required = true, friendlyNameResId = R.string.friendly_nombre_genero),
                "ID_TIPO_DOCUMENTO" to FieldRule(field = "ID_TIPO_DOCUMENTO", required = true, friendlyNameResId = R.string.hint_tipo_documento),
                "ID_GRADO_ACADEMICO" to FieldRule(field = "ID_GRADO_ACADEMICO", required = true, friendlyNameResId = R.string.friendly_nombre_grado),
                "NUP" to FieldRule(field = "NUP", required = true, friendlyNameResId = R.string.friendly_nup),
                "DIRECCION_DETALLE" to FieldRule(field = "DIRECCION_DETALLE", required = true, friendlyNameResId = R.string.friendly_direccion),
                "TELEFONO_CASA" to FieldRule(field = "TELEFONO_CASA", required = true, friendlyNameResId = R.string.friendly_telefono_casa),
                "TELEFONO_CELULAR" to FieldRule(field = "TELEFONO_CELULAR", required = true, friendlyNameResId = R.string.friendly_telefono_celular)
            )
            "EMPRESA" -> mapOf(
                "NIT" to FieldRule(field = "NIT", required = true, minLength = 14, maxLength = 14, pattern = "^\\d{14}$", friendlyNameResId = R.string.friendly_nit),
                "NOMBRE_EMPRESA" to FieldRule(field = "NOMBRE_EMPRESA", required = true, friendlyNameResId = R.string.friendly_nombre_empresa),
                "CONTACTO_DIRECTO" to FieldRule(field = "CONTACTO_DIRECTO", required = true, minLength = 9, maxLength = 9, pattern = "^\\d{4}-\\d{4}$", friendlyNameResId = R.string.friendly_contacto_directo)
            )
            "OFERTA_TRABAJO" -> mapOf(
                "ID_OFERTA" to FieldRule(field = "ID_OFERTA", required = true, pattern = "^OF\\d{2,}$", friendlyNameResId = R.string.friendly_codigo_oferta, maxLength = 10),
                "TITULO_PUESTO" to FieldRule(field = "TITULO_PUESTO", required = true, friendlyNameResId = R.string.friendly_titulo_puesto),
                "FECHA_PUBLICACION" to FieldRule(field = "FECHA_PUBLICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_publicacion),
                "FECHA_CADUCIDAD" to FieldRule(field = "FECHA_CADUCIDAD", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_caducidad),
                "EXPERIENCIA_ANIOS" to FieldRule(field = "EXPERIENCIA_ANIOS", required = true, friendlyNameResId = R.string.friendly_anios_experiencia),
                "EDAD_MINIMA" to FieldRule(field = "EDAD_MINIMA", min = 18, max = 100, friendlyNameResId = R.string.friendly_edad_minima),
                "EDAD_MAXIMA" to FieldRule(field = "EDAD_MAXIMA", min = 16, max = 100, friendlyNameResId = R.string.friendly_edad_maxima),
                "DESCRIPCION_OFERTA_TRABAJO" to FieldRule(field = "DESCRIPCION_OFERTA_TRABAJO", required = true, friendlyNameResId = R.string.friendly_descripcion_oferta)
            )
            "CATEGORIA_HABILIDAD" -> mapOf(
                "NOMBRE_CATEGORIA" to FieldRule(field = "NOMBRE_CATEGORIA", required = true, friendlyNameResId = R.string.friendly_nombre_categoria)
            )
            "GENERO" -> mapOf(
                "NOMBRE_GENERO" to FieldRule(field = "NOMBRE_GENERO", required = true, friendlyNameResId = R.string.friendly_nombre_genero)
            )
            "TIPO_DOCUMENTO" -> mapOf(
                "NOMBRE_TIPO" to FieldRule(field = "NOMBRE_TIPO", required = true, friendlyNameResId = R.string.friendly_nombre_tipo)
            )
            "DEPARTAMENTO" -> mapOf(
                "NOMBRE_DEPARTAMENTO" to FieldRule(field = "NOMBRE_DEPARTAMENTO", required = true, friendlyNameResId = R.string.friendly_nombre_departamento)
            )
            "MUNICIPIO" -> mapOf(
                "ID_MUNICIPIO" to FieldRule(field = "ID_MUNICIPIO", required = true, min = 1, max = 9999, friendlyNameResId = R.string.friendly_codigo_municipio),
                "NOMBRE_MUNICIPIO" to FieldRule(field = "NOMBRE_MUNICIPIO", required = true, friendlyNameResId = R.string.friendly_nombre_municipio)
            )
            "DISTRITO" -> mapOf(
                "ID_DISTRITO" to FieldRule(field = "ID_DISTRITO", required = true, min = 1, max = 9999, friendlyNameResId = R.string.friendly_codigo_distrito),
                "NOMBRE_DISTRITO" to FieldRule(field = "NOMBRE_DISTRITO", required = true, friendlyNameResId = R.string.friendly_nombre_distrito)
            )
            "INSTITUCION" -> mapOf(
                "ID_INSTITUCION" to FieldRule(field = "ID_INSTITUCION", required = true, pattern = "^INS\\d{3,}$", friendlyNameResId = R.string.friendly_codigo_institucion, maxLength = 20),
                "NOMBRE_INSTITUCION" to FieldRule(field = "NOMBRE_INSTITUCION", required = true, friendlyNameResId = R.string.friendly_nombre_institucion)
            )
            "GRADO_ACADEMICO" -> mapOf(
                "NOMBRE_GRADO" to FieldRule(field = "NOMBRE_GRADO", required = true, friendlyNameResId = R.string.friendly_nombre_grado)
            )
            "RED_SOCIAL" -> mapOf(
                "NOMBRE_RED" to FieldRule(field = "NOMBRE_RED", required = true, friendlyNameResId = R.string.friendly_nombre_red_social)
            )
            "HABILIDAD" -> mapOf(
                "ID_CATEGORIA_HABILIDAD" to FieldRule(field = "ID_CATEGORIA_HABILIDAD", required = true, friendlyNameResId = R.string.friendly_categoria),
                "ID_HABILIDAD" to FieldRule(field = "ID_HABILIDAD", required = true, pattern = "^H\\d{2,}$", friendlyNameResId = R.string.friendly_codigo_habilidad, maxLength = 10),
                "NOMBRE_HABILIDAD" to FieldRule(field = "NOMBRE_HABILIDAD", required = true, friendlyNameResId = R.string.friendly_nombre_habilidad)
            )
            "HABILIDAD_POSTULANTE" -> mapOf(
                "NIVEL_DESTREZA" to FieldRule(field = "NIVEL_DESTREZA", required = true, friendlyNameResId = R.string.friendly_nivel_destreza)
            )
            "CERTIFICACION" -> mapOf(
                "ID_CERTIFICACION" to FieldRule(field = "ID_CERTIFICACION", required = true, pattern = "^C\\d{3,}$", friendlyNameResId = R.string.friendly_codigo_certificacion, maxLength = 10),
                "NOMBRE_CERTIFICACION" to FieldRule(field = "NOMBRE_CERTIFICACION", required = true, friendlyNameResId = R.string.friendly_nombre_certificacion),
                "FECHA_CERTIFICACION" to FieldRule(field = "FECHA_CERTIFICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_certificacion),
                "FECHA_INICIO" to FieldRule(field = "FECHA_INICIO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_inicio),
                "FECHA_FIN" to FieldRule(field = "FECHA_FIN", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_fin)
            )
            "TIPO_CERTIFICACION" -> mapOf(
                "NOMBRE_TIPO" to FieldRule(field = "NOMBRE_TIPO", required = true, friendlyNameResId = R.string.friendly_nombre_tipo)
            )
            "EXPERIENCIA_LABORAL" -> mapOf(
                "ID_EXPERIENCIA" to FieldRule(field = "ID_EXPERIENCIA", required = true, pattern = "^EL\\d{2,}$", friendlyNameResId = R.string.friendly_codigo_experiencia, maxLength = 10),
                "PUESTO_TRABAJO" to FieldRule(field = "PUESTO_TRABAJO", required = true, friendlyNameResId = R.string.friendly_puesto_trabajo),
                "FECHA_INICIO" to FieldRule(field = "FECHA_INICIO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_inicio),
                "FECHA_FIN" to FieldRule(field = "FECHA_FIN", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_fin),
                "DESCP_EXPERIENCIA_LABORAL" to FieldRule(field = "DESCP_EXPERIENCIA_LABORAL", required = true, friendlyNameResId = R.string.friendly_descripcion_experiencia),
                "CONTACTO_REFERENCIA" to FieldRule(field = "CONTACTO_REFERENCIA", required = true, friendlyNameResId = R.string.friendly_contacto_referencia)
            )
            "FORMACION_ACADEMICA" -> mapOf(
                "ID_FORMACION" to FieldRule(field = "ID_FORMACION", required = true, pattern = "^FOA\\d{3,}$", friendlyNameResId = R.string.friendly_codigo_formacion, maxLength = 10),
                "TITULO_OBTENIDO" to FieldRule(field = "TITULO_OBTENIDO", required = true, friendlyNameResId = R.string.friendly_titulo_obtenido),
                "FECHA_INICIO" to FieldRule(field = "FECHA_INICIO", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_inicio),
                "FECHA_FIN" to FieldRule(field = "FECHA_FIN", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_fin),
                "FECHA_OBTENCION" to FieldRule(field = "FECHA_OBTENCION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_obtencion)
            )
            "OFERTA_ACADEMICA" -> mapOf(
                "ID_OFERTA_ACADEMICA" to FieldRule(field = "ID_OFERTA_ACADEMICA", required = true, pattern = "^OFA\\d{2,}$", friendlyNameResId = R.string.friendly_codigo_oferta_academica, maxLength = 10)
            )
            "POSTULACION" -> mapOf(
                "ID_POSTULACION" to FieldRule(field = "ID_POSTULACION", required = true, pattern = "^POS\\d{3,}$", friendlyNameResId = R.string.friendly_codigo_postulacion, maxLength = 10),
                "FECHA_APLICACION" to FieldRule(field = "FECHA_APLICACION", required = true, pattern = "^\\d{4}-\\d{2}-\\d{2}$", friendlyNameResId = R.string.friendly_fecha_aplicacion),
                "ESTADO_PROCESO" to FieldRule(field = "ESTADO_PROCESO", required = true, friendlyNameResId = R.string.friendly_estado_proceso)
            )
            "DETALLE_REQUISITO" -> mapOf(
                "ID_DETALLE" to FieldRule(field = "ID_DETALLE", required = true, pattern = "^D\\d{1,}$", friendlyNameResId = R.string.friendly_codigo_detalle, maxLength = 10),
                "DESCRIPCION_REQUISITO" to FieldRule(field = "DESCRIPCION_REQUISITO", required = true, friendlyNameResId = R.string.friendly_descripcion_requisito)
            )
            "RED_SOCIAL_POSTULANTE" -> mapOf(
                "URL_PERFIL" to FieldRule(field = "URL_PERFIL", required = true, pattern = "^https?://.*", friendlyNameResId = R.string.friendly_url_perfil)
            )
            else -> emptyMap()
        }
    }

    fun validate(context: Context, tableName: String, column: String, value: String): String? {
        val rules = getRulesForTable(tableName)[column] ?: return null
        val trimmed = value.trim()
        val name = context.getString(rules.friendlyNameResId)

        if (rules.required && trimmed.isEmpty()) {
            return context.getString(R.string.obligatorio, name)
        }

        if (trimmed.isEmpty()) return null

        if (rules.minLength > 0 && trimmed.length < rules.minLength) {
            return context.getString(R.string.minimo_caracteres, name, rules.minLength)
        }

        if (rules.maxLength > 0 && trimmed.length > rules.maxLength) {
            return context.getString(R.string.maximo_caracteres, name, rules.maxLength)
        }

        if (rules.pattern != null && !Regex(rules.pattern).matches(trimmed)) {
            return context.getString(R.string.formato_invalido, name)
        }

        val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val today = utcFormat.format(Date())

        val futureDateCols = setOf(
            "CERTIFICACION" to "FECHA_CERTIFICACION",
            "CERTIFICACION" to "FECHA_INICIO",
            "CERTIFICACION" to "FECHA_FIN",
            "EXPERIENCIA_LABORAL" to "FECHA_FIN",
            "POSTULACION" to "FECHA_APLICACION",
            "OFERTA_TRABAJO" to "FECHA_PUBLICACION",
            "FORMACION_ACADEMICA" to "FECHA_OBTENCION",
            "FORMACION_ACADEMICA" to "FECHA_INICIO",
            "FORMACION_ACADEMICA" to "FECHA_FIN"
        )
        if (Pair(tableName, column) in futureDateCols && trimmed > today) {
            return context.getString(R.string.fecha_futura, name)
        }

        if (rules.min != null) {
            val num = trimmed.toIntOrNull()
            if (num == null) return context.getString(R.string.debe_ser_numero, name)
            if (num < rules.min) return context.getString(R.string.debe_ser_mayor_igual, name, rules.min)
        }

        if (rules.max != null) {
            val num = trimmed.toIntOrNull()
            if (num != null && num > rules.max) return context.getString(R.string.debe_ser_menor_igual, name, rules.max)
        }

        return null
    }

}
