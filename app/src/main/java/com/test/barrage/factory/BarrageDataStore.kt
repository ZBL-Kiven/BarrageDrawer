package com.test.barrage.factory

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.test.barrage.drawer.BarrageDrawer
import com.test.barrage.drawer.BarrageSurfaceView
import com.test.barrage.info.BarrageInfo
import com.zj.danmaku.drawer.BaseHolder
import com.zj.danmaku.drawer.DrawerSurfaceView
import java.lang.Exception
import kotlin.math.max
import kotlin.random.Random


@Suppress("unused")
object BarrageDataStore {

    private var curMaxBallisticNum = 0
    private const val ballisticInterval = 50
    private const val topPadding = 20
    private var isFullMax = false
    private var getTimeLineListener: ((key: String) -> Long)? = null
    private var key: String = ""
    private var isPerDrawerRunning = false

    private var drawer: BarrageDrawer? = null
    private var barrageSurfaceView: BarrageSurfaceView? = null

    private fun start(context: Context) {
        if (drawer == null) drawer = BarrageDrawer.Drawer(context)
        if (barrageSurfaceView == null) {
            barrageSurfaceView = BarrageSurfaceView(context)
            barrageSurfaceView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        barrageSurfaceView?.setDrawer("default_barrage_drawer", drawer)
    }

    fun updateFullScreenState(inMax: Boolean) {
        isFullMax = inMax
    }

    fun start(
        context: Context,
        key: String,
        getTimeLineListener: ((key: String) -> Long),
        l: DrawerSurfaceView.RemoveFormParentListener
    ): BarrageSurfaceView? {
        BarrageDataStore.getTimeLineListener = getTimeLineListener
        if (BarrageDataStore.key != key) {
            BarrageDataStore.key = key
        }
        start(context)
        barrageSurfaceView?.setOnRemoveFormParentListener(l)
        return barrageSurfaceView
    }

    fun resume() {
        barrageSurfaceView?.onResume()
        drawer?.hidden = false
    }

    fun pause() {
        barrageSurfaceView?.onPause()
    }

    fun interrupt() {
        drawer?.hidden = true
    }

    fun stop() {
        barrageSurfaceView?.stop()
        BarrageRepository.release()
    }

    fun destroy() {
        drawer = null
        getTimeLineListener = null
        barrageSurfaceView?.onDestroy()
        barrageSurfaceView = null
    }

    fun updateDrawers(width: Int, height: Int, holders: List<BaseHolder<BarrageInfo>>) {
        if (isPerDrawerRunning) return
        isPerDrawerRunning = true
        try {
            var ballisticMap: MutableMap<Int, MutableList<BaseHolder<BarrageInfo>>>? =
                mutableMapOf()
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
                    if (timeLine < 0) return@updateDrawers
                    val dataTimeLine = d.data?.timeLine ?: 0
                    if (!d.stable || dataTimeLine >= timeLine || d.data?.isSelf() == true) curBallistic.add(
                        h
                    )
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
                val nextData = (if (nextIndex in 0..v.lastIndex) v[nextIndex].bindData else null)
                    ?: return@forEach
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

    fun getHolderData(
        paint: Paint,
        width: Int,
        height: Int,
        holder: BarrageDrawer.BarrageHolder
    ): BarrageInfo? {
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
            curMaxBallisticNum =
                ((height - ballisticInterval - topPadding) / (bInfo.height + ballisticInterval).coerceAtLeast(
                    1f
                )).toInt()
        }
        return bInfo
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getCurBallisticNum(width: Int, height: Int): Int {
        return (if (isFullMax) 3 else 5).coerceAtMost(curMaxBallisticNum)
    }
}