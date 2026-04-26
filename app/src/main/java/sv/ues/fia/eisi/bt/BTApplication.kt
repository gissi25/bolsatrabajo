package sv.ues.fia.eisi.bt

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class BTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
