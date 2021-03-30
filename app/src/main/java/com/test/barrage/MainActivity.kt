package com.test.barrage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.test.barrage.drawer.BarrageSurfaceView
import com.test.barrage.factory.BarrageDataStore
import com.test.barrage.factory.BarrageRepository
import com.zj.danmaku.drawer.DrawerSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var testContainer: FrameLayout
    private lateinit var rmListener: DrawerSurfaceView.RemoveFormParentListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.mContainer)
        testContainer = findViewById(R.id.mTestContainer)
        rmListener = DrawerSurfaceView.RemoveFormParentListener {
            Handler(Looper.getMainLooper()).post {
                testContainer.removeView(container)
            }
        }
    }

    fun clickToStart(v: View) {
        val barrageView = startBarrage() ?: return
        val needAdd = (barrageView.parent as? ViewGroup)?.let {
            if (it != container) {
                it.removeView(barrageView);true
            } else false
        } ?: true
        if (needAdd) container.addView(barrageView, FrameLayout.LayoutParams(-1, -1)) else BarrageDataStore.resume()
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
        BarrageRepository.commitBarrage("I just submitted the bullet screen")
        BarrageDataStore.resume()
    }

    fun removeAndAdd(view: View) {
        testContainer.removeView(container)
        //        BarrageDataStore.destroy()
    }

    private var timeLineMocker = System.currentTimeMillis()
    private val onProgressGet = { _: String ->
        val cur = (System.currentTimeMillis() - timeLineMocker) / 1000
        cur
    }


    private fun startBarrage(): BarrageSurfaceView? {
        timeLineMocker = System.currentTimeMillis()
        return BarrageDataStore.start(this, "11111", onProgressGet, rmListener)
    }
}