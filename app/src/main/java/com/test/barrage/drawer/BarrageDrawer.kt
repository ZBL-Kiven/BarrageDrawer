package com.test.barrage.drawer

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.graphics.contains
import com.test.barrage.factory.BarrageDataStore
import com.test.barrage.graphics.UTL
import com.test.barrage.info.BarrageInfo
import com.zj.danmaku.drawer.*

/**
 * Used to draw barrage
 * */
abstract class BarrageDrawer(context: Context) : BaseDrawer(context) {

    abstract fun getBallisticNum(width: Int, height: Int): Int
    abstract fun getHolderData(width: Int, height: Int, holder: BarrageHolder): BarrageInfo?
    abstract fun onHolderClick(v: DrawerSurfaceView, x: Int, y: Int, barrageHolder: BarrageHolder)
    abstract fun updateDrawers(width: Int, height: Int, holders: List<BarrageHolder>)
    abstract fun updateFrame(holder: BarrageHolder, canvas: Canvas?, width: Int, height: Int, changedAlpha: Float)
    abstract fun getBarragePaint(): Paint

    final override fun getHolders(): MutableList<HoldersInfo<BarrageHolder, BarrageInfo>> {
        val barrageHolder = object : HoldersInfo<BarrageHolder, BarrageInfo>(25) {

            override fun getHolderType(position: Int): BarrageHolder {
                return BarrageHolder(position)
            }

            override fun getHolderData(width: Int, height: Int, holder: BarrageHolder): BarrageInfo? {
                return this@BarrageDrawer.getHolderData(width, height, holder)
            }

            override fun updateDrawers(width: Int, height: Int, holders: List<BarrageHolder>) {
                this@BarrageDrawer.updateDrawers(width, height, holders)
            }
        }
        return arrayListOf(barrageHolder)
    }

    inner class BarrageHolder(position: Int) : BaseHolder<BarrageInfo>(position) {
        var isPausedMove = false
        private var lastTouchedPoint: PointF? = null
        override fun updateFrame(canvas: Canvas?, width: Int, height: Int, changedAlpha: Float) {
            this@BarrageDrawer.updateFrame(this, canvas, width, height, changedAlpha)
        }

        override fun bindData(data: BarrageInfo?) {
            super.bindData(data)
            isPausedMove = false
        }

        override fun isDrawInTopLayer(): Boolean {
            return isPausedMove
        }

        override fun onTouchEvent(v: DrawerSurfaceView, event: MotionEvent): Boolean {
            bindData?.let {
                val point = PointF(event.x, event.y)
                val rect = RectF(it.start, it.top, it.start + it.width, it.top + it.height)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (rect.contains(point)) {
                            lastTouchedPoint = point
                            isPausedMove = true
                            return false
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!rect.contains(point)) {
                            isPausedMove = false
                        }
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (rect.contains(point)) {
                            this@BarrageDrawer.onHolderClick(v, event.x.toInt(), event.y.toInt(), this)
                        } else {
                            isPausedMove = false
                        }
                    }
                }
            }
            return true
        }
    }

    @Suppress("unused")
    open class Drawer(context: Context) : BarrageDrawer(context) {

        private val normalPaint = Paint()
        private val pausedItemPaint = Paint().apply {
            this.color = Color.parseColor("#70000000")
            this.style = Paint.Style.FILL
            this.flags = Paint.ANTI_ALIAS_FLAG
        }

        override fun getBallisticNum(width: Int, height: Int): Int {
            return if (width > height) 3 else 5
        }

        override fun getHolderData(width: Int, height: Int, holder: BarrageHolder): BarrageInfo? {
            return BarrageDataStore.getHolderData(getBarragePaint(), width, height, holder)
        }

        override fun updateDrawers(width: Int, height: Int, holders: List<BarrageHolder>) {
            BarrageDataStore.updateDrawers(width, height, holders)
        }

        override fun updateFrame(holder: BarrageHolder, canvas: Canvas?, width: Int, height: Int, changedAlpha: Float) {
            try {
                val info = holder.bindData
                val paint = getBarragePaint()
                if (info.data?.isSelf() == true) {
                    paint.color = Color.parseColor("#fea01f")
                }
                paint.alpha = (255f * changedAlpha).toInt()
                if (holder.isDrawInTopLayer) {
                    val pah = dp2px(15f)
                    val pav = dp2px(10f)
                    val rect = RectF(info.start - pah, info.top - pav, info.start + info.width + pah, info.top + info.height + pav)
                    canvas?.drawRoundRect(rect, pah, pah, pausedItemPaint)
                }
                if (info.start > -info.width) {
                    var top = info.top
                    if (info.top >= 0) {
                        top += paint.descent() - paint.ascent() / 2f
                        canvas?.drawText(info.data?.content ?: "", info.start, top, paint)
                    }
                }
                if (!holder.isPausedMove) {
                    if (info.start <= -info.width) {
                        holder.destroyAndIdle()
                    } else info.start -= (info.step * info.ratio).toInt()
                }
            } finally {
                normalPaint.reset()
            }
        }

        override fun onHolderClick(v: DrawerSurfaceView, x: Int, y: Int, barrageHolder: BarrageHolder) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "holder clicked! ==>  ${barrageHolder.bindData.data?.content}", Toast.LENGTH_SHORT).show()
                barrageHolder.isPausedMove = false
            }
        }

        override fun getBarragePaint(): Paint {
            return normalPaint.apply {
                UTL.transformPaintNormal(normalPaint)
                this.textSize = sp2px(14f)
            }
        }
    }
}