package sv.ues.fia.eisi.bt.utils

import android.widget.TextView
import androidx.appcompat.widget.Toolbar

fun Toolbar.setupMarqueeTitle() {
    post {
        for (i in 0 until childCount) {
            val child = getChildAt(i) as? TextView ?: continue
            if (child.text == title) {
                child.isSelected = true
                child.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
                child.marqueeRepeatLimit = -1
                break
            }
        }
    }
}
