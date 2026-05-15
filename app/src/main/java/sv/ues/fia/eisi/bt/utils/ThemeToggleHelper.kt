package sv.ues.fia.eisi.bt.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import sv.ues.fia.eisi.bt.R

object ThemeToggleHelper {

    fun toggle(activity: Activity) {
        val compat = activity as? AppCompatActivity ?: return
        val currentMode = compat.delegate.localNightMode
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        compat.delegate.localNightMode = newMode
    }

    fun getIconRes(context: Context): Int {
        return if (isNightMode(context)) R.drawable.sol else R.drawable.luna
    }

    fun getLogoutIconRes(context: Context): Int {
        return if (isNightMode(context)) R.drawable.cerrar_sesion_c else R.drawable.cerrar_sesion_o
    }

    fun getInsertIconRes(context: Context): Int {
        return if (isNightMode(context)) R.drawable.ic_insertar_datos_c else R.drawable.ic_insertar_datos_o
    }

    fun getWorldIconRes(context: Context): Int {
        return if (isNightMode(context)) R.drawable.ic_world_b else R.drawable.ic_world_n
    }

    private fun isNightMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
