package com.test.barrage.factory

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewGroup
import com.test.barrage.drawer.BarrageDrawer
import com.test.barrage.drawer.BarrageSurfaceView
import com.test.barrage.info.BarrageDataInfo
import com.zj.danmaku.drawer.BaseHolder
import com.test.barrage.info.BarrageInfo
import com.zj.danmaku.BarrageRepository
import java.util.*

object BarrageDataStore {

    private var curMaxBallisticNum = 0
    private const val ballisticInterval = 50
    private const val topPadding = 20
    private var isFullMax = false
    private var getTimeLineListener: ((key: String) -> Long)? = null
    private var key: String = ""

    private var drawer: BarrageDrawer? = null
    private var barrageSurfaceView: BarrageSurfaceView? = null

    private fun getBarrageView(context: Context, required: Boolean = true): BarrageSurfaceView? {
        if (required && drawer == null) drawer = BarrageDrawer.Drawer(context)
        if (required && barrageSurfaceView == null) {
            barrageSurfaceView = BarrageSurfaceView(context)
        }
        barrageSurfaceView?.setDrawer("default_barrage_drawer", drawer)
        return barrageSurfaceView
    }

    fun start(context: Context, key: String, getTimeLineListener: ((key: String) -> Long)): BarrageSurfaceView? {
        this.getTimeLineListener = getTimeLineListener
        this.key = key
        val barrageSurfaceView = getBarrageView(context)
        barrageSurfaceView?.onResume()
        return barrageSurfaceView
    }

    fun resume() {
        barrageSurfaceView?.onResume()
    }

    fun pause() {
        barrageSurfaceView?.onPause()
    }

    fun stop() {
        getTimeLineListener = null
        barrageSurfaceView?.setDrawer("stop", null)
    }

    fun destroy() {
        drawer = null
        (barrageSurfaceView?.parent as? ViewGroup)?.removeView(barrageSurfaceView)
        barrageSurfaceView?.onDestroy()
        barrageSurfaceView = null
    }

    fun updateDrawers(width: Int, height: Int, holders: List<BaseHolder<BarrageInfo>>) {
        if (curMaxBallisticNum <= 0) return
        holders.sortedByDescending { it.bindData?.data?.timeLine ?: 0 }
        val maxBallistic = getCurBallisticNum(width, height)
        holders.forEachIndexed { index, h ->
            h.bindData?.let { d ->
                val ballistic = h.position % maxBallistic
                if (d.top <= 0f || d.ballistic !in 0..maxBallistic) {
                    d.ballistic = ballistic
                    d.top = ballistic * (d.height + ballisticInterval) + topPadding
                }
                if (d.start >= width) {

                }
                d.ratio = 1.0f

            }
            //            baseHolder.bindData?.let { d ->
            //                if (d.top <= 0) {
            //                    d.top = height
            //                }
            //            }
        }
    }

    fun getHolderData(paint: Paint, width: Int, height: Int, holder: BaseHolder<BarrageInfo>): BarrageInfo? {
        val barrageData = BarrageRepository.pollBarrage((getTimeLineListener?.invoke(key)?.div(1000))?.toInt()
                ?: return null, key)
        return if ((Random().nextFloat() * 300).toInt() == 108) {
            BarrageInfo().apply {
                barrageData ?: return null
                this.start = width + 1.0f
                val rect = Rect()
                paint.getTextBounds(barrageData.content, 0, barrageData.content.length, rect)
                this.width = rect.width() + 0.5f
                this.height = rect.height() + 0.5f
                this.data = barrageData
                if (height > 0) {
                    curMaxBallisticNum = ((height - ballisticInterval - topPadding) / (this.height + ballisticInterval).coerceAtLeast(1f)).toInt()
                }
            }
        } else null
    }

    private fun getCurBallisticNum(width: Int, height: Int): Int {
        return (if (isFullMax) 3 else 5).coerceAtMost(curMaxBallisticNum)
    }
}