package sv.ues.fia.eisi.bt

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import sv.ues.fia.eisi.bt.utils.LocaleHelper

class BTApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(base, LocaleHelper.getLanguage(base)))
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
