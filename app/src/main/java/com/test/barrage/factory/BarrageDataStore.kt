package com.test.barrage.factory

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import com.test.barrage.drawer.BarrageDrawer
import com.test.barrage.drawer.BarrageSurfaceView
import com.zj.danmaku.drawer.BaseHolder
import com.test.barrage.info.BarrageInfo
import java.lang.Exception
import kotlin.math.max
import kotlin.random.Random


object BarrageDataStore {

    private var curMaxBallisticNum = 0
    private const val ballisticInterval = 50
    private const val topPadding = 20
    private var isFullMax = false
    private var getTimeLineListener: ((key: String) -> Long)? = null
    private var key: String = ""
    private var isPerDrawerRunning = false
    private val interceptor = AccelerateInterpolator(1.5f)

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
        BarrageRepository.release()
    }

    fun destroy() {
        drawer = null
        (barrageSurfaceView?.parent as? ViewGroup)?.removeView(barrageSurfaceView)
        barrageSurfaceView?.onDestroy()
        barrageSurfaceView = null
    }

    fun updateDrawers(width: Int, height: Int, holders: List<BaseHolder<BarrageInfo>>) {
        if (isPerDrawerRunning) return
        isPerDrawerRunning = true
        try {
            var ballisticMap: MutableMap<Int, MutableList<BaseHolder<BarrageInfo>>>? = mutableMapOf()
            if (curMaxBallisticNum <= 0) return
            val maxBallistic = getCurBallisticNum(width, height)
            holders.forEach { h ->
                //group by ballistics
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
                    val dataTimeLine = d.data?.timeLine ?: 0
                    if (!d.stable || dataTimeLine >= timeLine || d.data?.isSelf() == true) curBallistic.add(h)
                    else h.destroyAndIdle()
                }
                ballisticMap?.put(ballistic, curBallistic)
            }
            ballisticMap?.forEach { (_, v) ->
                //build barrage with simple ballistic
                v.sortBy { it.bindData?.data?.timeLine ?: 1 }
                val lastIndex = v.indexOfLast { !it.bindData.stable }
                val nextIndex = v.indexOfFirst {
                    {
                        val b1 = it.bindData?.data
                        if (b1 == null || it.bindData?.stable == false) false else b1.isSelf()
                    }.invoke() || it.bindData.stable
                }
                if (nextIndex <= lastIndex) return@forEach
                val lastData = if (lastIndex in 0..v.lastIndex) v[lastIndex].bindData else null
                val nextData = (if (nextIndex in 0..v.lastIndex) v[nextIndex].bindData else null) ?: return@forEach
                if (lastData == null) nextData.stable = false
                else {
                    val lEnd = width - (lastData.start + lastData.width)
                    nextData.stable = lEnd <= nextData.lastStart
                }
            }
            ballisticMap?.clear()
            ballisticMap = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isPerDrawerRunning = false
        }
    }

    fun getHolderData(paint: Paint, width: Int, height: Int, holder: BarrageDrawer.BarrageHolder): BarrageInfo? {
        if (isPerDrawerRunning) return null
        val bInfo = BarrageInfo()
        val timeLine = (getTimeLineListener?.invoke(key))?.toInt() ?: return null
        val barrageData = BarrageRepository.pollBarrage(timeLine + 3, key)
        barrageData ?: return null
        bInfo.start = width * 1f + holder.randomStart
        val rect = Rect()
        paint.getTextBounds(barrageData.content, 0, barrageData.content.length, rect)
        bInfo.width = rect.width() + 0.5f
        bInfo.height = rect.height() + 0.5f
        bInfo.data = barrageData
        val biw = width / 8f
        val lengthRatio = (max(0f, rect.width() - biw) * 1.0f / (width / 2.0f)) * 0.5f
        val br = 0.85f + Random.nextFloat() * 0.15f + lengthRatio
        bInfo.ratio = br
        bInfo.lastStart = Random.nextFloat() * 100f + 200f
        if (height > 0) {
            curMaxBallisticNum = ((height - ballisticInterval - topPadding) / (bInfo.height + ballisticInterval).coerceAtLeast(1f)).toInt()
        }
        return bInfo
    }

    private fun getCurBallisticNum(width: Int, height: Int): Int {
        return (if (isFullMax) 3 else 5).coerceAtMost(curMaxBallisticNum)
    }
}