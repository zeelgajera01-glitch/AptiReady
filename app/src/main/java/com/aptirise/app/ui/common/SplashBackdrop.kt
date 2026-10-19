package com.aptirise.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.aptirise.app.R

class SplashBackdrop @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(ContextCompat.getColor(context, R.color.splash_background))
        paint.color = ContextCompat.getColor(context, R.color.splash_wave)
        val p = Path().apply { moveTo(0f,h*.69f); cubicTo(w*.18f,h*.61f,w*.38f,h*.62f,w*.55f,h*.75f); cubicTo(w*.72f,h*.85f,w*.9f,h*.88f,w,h*.89f); lineTo(w,h); lineTo(0f,h); close() }
        c.drawPath(p,paint)
        paint.alpha=120
        val p2 = Path().apply { moveTo(0f,h*.75f); cubicTo(w*.36f,h*.62f,w*.73f,h*.72f,w,h*.81f); lineTo(w,h); lineTo(0f,h); close() }
        c.drawPath(p2,paint)
        paint.alpha=130; c.drawCircle(-w*.08f,h*.3f,w*.4f,paint)
        paint.color=ContextCompat.getColor(context,R.color.splash_wave_accent)
        paint.alpha=110
        val glint=Path().apply { moveTo(w*.78f,h*.16f);lineTo(w,h*.05f);lineTo(w,h*.1f);lineTo(w*.87f,h*.16f);close() }
        c.drawPath(glint,paint); paint.alpha=255
    }
}
