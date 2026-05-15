package sv.ues.fia.eisi.bt

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import sv.ues.fia.eisi.bt.utils.LocaleHelper
import java.util.Locale

class BTApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(base, LocaleHelper.getLanguage(base)))
    }

    override fun getResources(): Resources {
        val config = Configuration(super.getResources().configuration)
        val lang = LocaleHelper.getLanguage(this)
        if (lang.isNotEmpty()) {
            val locale = Locale.forLanguageTag(lang)
            if (config.locales.get(0) != locale) {
                config.setLocale(locale)
                return createConfigurationContext(config).resources
            }
        }
        return super.getResources()
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
