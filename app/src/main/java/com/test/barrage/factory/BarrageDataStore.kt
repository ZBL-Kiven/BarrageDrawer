package com.test.barrage.factory

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewGroup
import com.test.barrage.drawer.BarrageDrawer
import com.test.barrage.drawer.BarrageSurfaceView
import com.zj.danmaku.drawer.BaseHolder
import com.test.barrage.info.BarrageInfo
import com.zj.danmaku.BarrageRepository
import kotlin.random.Random


object BarrageDataStore {

    private var curMaxBallisticNum = 0
    private const val ballisticInterval = 50
    private const val topPadding = 20
    private var isFullMax = false
    private var getTimeLineListener: ((key: String) -> Long)? = null
    private var key: String = ""
    private var ballisticMap = mutableMapOf<Int, MutableList<BaseHolder<BarrageInfo>>>()

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
        if (curMaxBallisticNum <= 0) return
        val maxBallistic = getCurBallisticNum(width, height)
        val timeLine = getTimeLineListener?.invoke(key) ?: return
        holders.forEach { h ->
            val ballistic = h.position % maxBallistic
            val bl = ballisticMap[ballistic]
            val curBallistic = bl ?: mutableListOf()
            if (bl.isNullOrEmpty()) ballisticMap[ballistic] = curBallistic
            if (h.bindData != null) curBallistic.add(h)
            if (curBallistic.isNotEmpty()) curBallistic.sortBy { it.bindData?.data?.timeLine ?: 0 }
            h.bindData?.let { d ->
                val tl = d.data?.timeLine
                if (tl != null && tl <= timeLine) {
                    if (ballistic != d.ballistic) {
                        d.ballistic = ballistic
                        d.top = ballistic * (d.height + ballisticInterval) + topPadding
                    }
                }
            }
        }
        ballisticMap.values.forEach { v ->
            val first = v.groupBy {
                val cur = it.bindData ?: return@groupBy false
                cur.start + cur.width > 0 && cur.start <= width
            }
            if (first.isNullOrEmpty() || first[true].isNullOrEmpty()) v.firstOrNull()?.bindData?.stable = false
            else {
                first[true]?.forEach { it.bindData?.stable = false }
                first[true]?.lastOrNull()?.bindData?.let { last ->
                    val lastEnd = width - (last.start + last.width)
                    val firstStable = first[false]?.firstOrNull()
                    val bindData = firstStable?.bindData
                    if (bindData != null && lastEnd >= bindData.lastStart) {
                        bindData.stable = false
                    }
                }
            }
        }
        ballisticMap.clear()
    }

    fun getHolderData(paint: Paint, width: Int, height: Int, holder: BaseHolder<BarrageInfo>): BarrageInfo? {
        return BarrageInfo().apply {
            val barrageData = BarrageRepository.pollBarrage((getTimeLineListener?.invoke(key)?.div(1000))?.toInt() ?: return null, key)
            barrageData ?: return null
            this.start = width + Random.nextFloat() * 300f
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