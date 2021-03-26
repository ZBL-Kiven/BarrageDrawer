package com.test.barrage.factory

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import com.test.barrage.drawer.BarrageDrawer
import com.test.barrage.drawer.BarrageSurfaceView
import com.zj.danmaku.drawer.BaseHolder
import com.test.barrage.info.BarrageInfo
import com.zj.danmaku.BarrageRepository


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

    private fun updateFullScreenState(inMax: Boolean) {
        this.isFullMax = inMax
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
        var ballisticMap: MutableMap<Int, MutableList<BaseHolder<BarrageInfo>>>? = mutableMapOf()
        if (curMaxBallisticNum <= 0) return
        val maxBallistic = getCurBallisticNum(width, height)
        holders.forEach { h ->
            val ballistic = h.position % maxBallistic
            val bl = ballisticMap?.get(ballistic)
            val curBallistic = bl ?: mutableListOf()
            val d = h.bindData
            if (d != null) {
                if (ballistic != d.ballistic) {
                    d.ballistic = ballistic
                }
                d.top = ballistic * (d.height + ballisticInterval) + topPadding
                val timeLine = getTimeLineListener?.invoke(key) ?: return@forEach
                if (!d.stable || (d.data?.timeLine ?: 0) in timeLine - 1..timeLine + 1) curBallistic.add(h)
                else h.destroyAndIdle()
            }
            ballisticMap?.put(ballistic, curBallistic)
        }
        ballisticMap?.values?.forEach { v ->
            val last = v.lastOrNull { !it.bindData.stable }?.bindData
            val next = v.firstOrNull { it.bindData.stable }?.bindData ?: return@forEach
            val lEnd = width - ((last?.start ?: 0f) + (last?.width ?: 0f))
            next.stable = lEnd < next.lastStart
        }
        ballisticMap?.clear()
        ballisticMap = null
    }

    fun getHolderData(paint: Paint, width: Int, height: Int, holder: BaseHolder<BarrageInfo>): BarrageInfo? {
        return BarrageInfo().apply {
            val timeLine = (getTimeLineListener?.invoke(key))?.toInt() ?: return null
            val barrageData = BarrageRepository.pollBarrage(timeLine, key)
            barrageData ?: return null
            this.start = width * 1f
            val rect = Rect()
            paint.getTextBounds(barrageData.content, 0, barrageData.content.length, rect)
            this.width = rect.width() + 0.5f
            this.height = rect.height() + 0.5f
            this.data = barrageData
            this.lastStart = 100f
            if (height > 0) {
                curMaxBallisticNum = ((height - ballisticInterval - topPadding) / (this.height + ballisticInterval).coerceAtLeast(1f)).toInt()
            }
        }
    }

    private fun getCurBallisticNum(width: Int, height: Int): Int {
        return (if (isFullMax) 3 else 5).coerceAtMost(curMaxBallisticNum)
    }
}