package sv.ues.fia.eisi.bt.utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import sv.ues.fia.eisi.bt.R

object StyledToast {

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            var activity: Activity? = null
            
            if (context is Activity) {
                activity = context
            }

            val isDarkMode = activity?.let {
                (it.resources.configuration.uiMode and 
                 android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                 android.content.res.Configuration.UI_MODE_NIGHT_YES
            } ?: false

            val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null, false)
            val textView = layout.findViewById<TextView>(R.id.toast_text)
            textView.text = message
            
            if (isDarkMode) {
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                textView.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
            } else {
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_light))
                textView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            Toast(context).apply {
                setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 200)
                this.duration = duration
            }.apply {
                @Suppress("DEPRECATION")
                view = layout
            }.show()
        } catch (e: Exception) {
            Toast.makeText(context, message, duration).show()
        }
    }
}