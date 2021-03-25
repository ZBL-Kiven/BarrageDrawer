package com.test.barrage.graphics

import android.graphics.Color
import android.graphics.Paint

object UTL {

    fun transformPaintNormal(paint: Paint) {
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.color = Color.WHITE
        paint.setShadowLayer(5f, 3f, 3f, Color.RED)
    }
}