package sv.ues.fia.eisi.bt.utils

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import sv.ues.fia.eisi.bt.R

object StyledToast {

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null, false)
            val textView = layout.findViewById<TextView>(R.id.toast_text)
            textView.text = message
            
            val typedValue = android.util.TypedValue()
            context.theme?.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            val bgColor = if (typedValue.resourceId != 0) {
                ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            layout.setBackgroundColor(bgColor)
            
            context.theme?.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val textColor = if (typedValue.resourceId != 0) {
                ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            textView.setTextColor(textColor)

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