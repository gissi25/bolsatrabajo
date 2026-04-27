package sv.ues.fia.eisi.bt.utils

object InputMaskUtils {

    const val DUI_LENGTH = 9
    const val NUP_LENGTH = 14
    const val PASAPORTE_LENGTH = 9
    const val MIN_PASSWORD = 8
    const val EXPERIENCIA_MIN = 0
    const val EXPERIENCIA_MAX = 50
    const val EDAD_MIN = 16
    const val EDAD_MAX = 100
    const val TELEFONO_LENGTH = 8

    fun formatDUI(text: String): String {
        if (text.contains("-")) return text
        val digits = text.filter { it.isDigit() }
        if (digits.length == 9) {
            return "${digits.substring(0, 8)}-${digits.substring(8)}"
        }
        return text
    }

    fun formatNIT(text: String): String {
        if (text.contains("-")) return text
        val digits = text.filter { it.isDigit() }
        return when {
            digits.length <= 4 -> digits
            digits.length <= 10 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
            digits.length <= 13 -> "${digits.substring(0, 4)}-${digits.substring(4, 10)}-${digits.substring(10)}"
            digits.length <= 15 -> "${digits.substring(0, 4)}-${digits.substring(4, 10)}-${digits.substring(10, 13)}-${digits.substring(13)}"
            else -> digits
        }
    }

    fun formatTelefono(text: String): String {
        val digits = text.filter { it.isDigit() }
        return when (digits.length) {
            in 0..4 -> digits
            else -> "${digits.substring(0, 4)}-${digits.substring(4, minOf(8, digits.length))}"
        }
    }

    fun validateRango(value: String, min: Int, max: Int, nombre: String): String? {
        val n = value.toIntOrNull()
        return when {
            n == null -> "$nombre debe ser un número"
            n < min || n > max -> "$nombre debe estar entre $min y $max"
            else -> null
        }
    }

    fun validatePassword(value: String): String? {
        return if (value.length < MIN_PASSWORD) "Mínimo $MIN_PASSWORD caracteres" else null
    }

    fun validateEmail(value: String): String? {
        return if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches())
            "Correo electrónico inválido" else null
    }

    fun validateURL(value: String): String? {
        return if (!value.startsWith("http://") && !value.startsWith("https://"))
            "URL debe comenzar con http:// o https://" else null
    }

    fun validateFecha(value: String): String? {
        return if (!value.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
            "Formato: AAAA-MM-DD" else null
    }
}
