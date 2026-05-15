package sv.ues.fia.eisi.bt.utils

import android.content.Context
import sv.ues.fia.eisi.bt.R

object InputMaskUtils {

    const val DUI_LENGTH = 9
    const val NUP_LENGTH = 12
    const val MIN_PASSWORD = 8
    const val TELEFONO_LENGTH = 8
    const val NIT_LENGTH_SIMPLE = 14

    fun formatDUI(text: String): String {
        val digits = text.filter { it.isDigit() }
        val d = if (digits.length > 9) digits.substring(0, 9) else digits
        
        return if (d.length == 9) {
            "${d.substring(0, 8)}-${d.substring(8)}"
        } else {
            d
        }
    }

    fun formatNIT(text: String): String {
        val digits = text.filter { it.isDigit() }
        val d = if (digits.length > 14) digits.substring(0, 14) else digits
        
        return when {
            d.length <= 4 -> d
            d.length <= 10 -> "${d.substring(0, 4)}-${d.substring(4)}"
            d.length <= 13 -> "${d.substring(0, 4)}-${d.substring(4, 10)}-${d.substring(10)}"
            else -> "${d.substring(0, 4)}-${d.substring(4, 10)}-${d.substring(10, 13)}-${d.substring(13)}"
        }
    }

    fun formatTelefono(text: String): String {
        val digits = text.filter { it.isDigit() }
        val d = if (digits.length > 8) digits.substring(0, 8) else digits
        return when {
            d.length <= 4 -> d
            else -> "${d.substring(0, 4)}-${d.substring(4)}"
        }
    }

    fun formatNitSimple(text: String): String {
        val digits = text.filter { it.isDigit() }
        return if (digits.length > 14) digits.substring(0, 14) else digits
    }

    fun validatePassword(context: Context, value: String): String? {
        return if (value.length < MIN_PASSWORD) context.getString(R.string.validation_password_min, MIN_PASSWORD) else null
    }

    fun validateEmail(context: Context, value: String): String? {
        return if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches())
            context.getString(R.string.validation_email_invalid) else null
    }

    fun validateFecha(context: Context, value: String): String? {
        return if (!value.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
            context.getString(R.string.validation_date_format) else null
    }
}

fun String.removeAccents(): String {
    return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("[\\u0300-\\u036f]"), "")
}
