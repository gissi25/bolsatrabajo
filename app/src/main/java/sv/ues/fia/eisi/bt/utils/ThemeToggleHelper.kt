package sv.ues.fia.eisi.bt.utils

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import sv.ues.fia.eisi.bt.R

object ThemeToggleHelper {

    fun toggle(activity: Activity) {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        activity.recreate()
    }

    fun getIconRes(): Int {
        return if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            R.drawable.sol
        } else {
            R.drawable.luna
        }
    }

    fun getLogoutIconRes(): Int {
        return if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            R.drawable.cerrar_sesion_c
        } else {
            R.drawable.cerrar_sesion_o
        }
    }

    fun getInsertIconRes(): Int {
        return if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            R.drawable.ic_insertar_datos_c
        } else {
            R.drawable.ic_insertar_datos_o
        }
    }
}
