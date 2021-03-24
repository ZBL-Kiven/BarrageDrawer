package com.zj.danmaku

import android.content.Context
import android.graphics.Canvas
import com.zj.danmaku.drawer.BaseDrawer
import com.zj.danmaku.drawer.BaseHolder
import com.zj.danmaku.drawer.HoldersInfo
import com.zj.danmaku.info.BarrageInfo

/**
 * Used to draw barrage
 * */
abstract class BarrageDrawer<T>(context: Context) : BaseDrawer(context) {

    abstract fun getHolderData(holder: BaseHolder<BarrageInfo<T>>): BarrageInfo<T>
    abstract fun updateFrame(info: BarrageInfo<T>, canvas: Canvas?, width: Int, height: Int, changedAlpha: Float)

    final override fun getHolders(): MutableList<HoldersInfo<BarrageInfo<T>>> {
        val barrageHolder = object : HoldersInfo<BarrageInfo<T>>(25) {

            override fun getHolderType(position: Int): BaseHolder<BarrageInfo<T>> {
                return BarrageHolder(position)
            }

            override fun getHolderData(holder: BaseHolder<BarrageInfo<T>>): BarrageInfo<T> {
                return this@BarrageDrawer.getHolderData(holder)
            }
        }
        return arrayListOf(barrageHolder)
    }

    inner class BarrageHolder(position: Int) : BaseHolder<BarrageInfo<T>>(position) {

        override fun updateFrame(canvas: Canvas?, width: Int, height: Int, changedAlpha: Float) {
            this@BarrageDrawer.updateFrame(bindData, canvas, width, height, changedAlpha)
        }
    }
}