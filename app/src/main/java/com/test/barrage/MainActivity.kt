package com.test.barrage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.test.barrage.factory.BarrageDataStore
import com.test.barrage.drawer.BarrageDrawer
import com.zj.danmaku.BarrageRepository
import com.zj.danmaku.drawer.DrawerSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.mContainer)
    }

    fun clickToStart(v: View) {
        val timeLineMocker = System.currentTimeMillis()
        val barrageView = BarrageDataStore.start(this, "1111") {
            val tm = System.currentTimeMillis() - timeLineMocker
            return@start tm / 1000
        }
        val needAdd = (barrageView?.parent as? ViewGroup)?.let {
            if (it != container) {
                it.removeView(barrageView);true
            } else false
        } ?: true
        if (needAdd) container.addView(barrageView, FrameLayout.LayoutParams(-1, -1))
    }

    fun clickToStop(v: View) {
        BarrageDataStore.stop()
    }

    override fun onResume() {
        super.onResume()
        BarrageDataStore.resume()
    }

    override fun onPause() {
        BarrageDataStore.pause()
        super.onPause()
    }

    override fun onDestroy() {
        BarrageDataStore.destroy()
        super.onDestroy()
    }

    fun clickToCommit(view: View) {
        BarrageDataStore.pause()
        BarrageRepository.commitBarrage("我是刚才提交的弹幕")
        BarrageDataStore.resume()
    }
}