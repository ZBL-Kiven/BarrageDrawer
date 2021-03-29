package com.test.barrage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.test.barrage.drawer.BarrageSurfaceView
import com.test.barrage.factory.BarrageDataStore
import com.test.barrage.factory.BarrageRepository

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.mContainer)
    }

    fun clickToStart(v: View) {
        val barrageView = startBarrage() ?: return
        val needAdd = (barrageView.parent as? ViewGroup)?.let {
            if (it != container) {
                it.removeView(barrageView);true
            } else false
        } ?: true
        if (needAdd) container.addView(barrageView, FrameLayout.LayoutParams(-2, 400)) else BarrageDataStore.resume()
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
        BarrageDataStore.destroy()
        clickToStart(view)
    }

    private val timeLineMocker = System.currentTimeMillis()
    private val onProgressGet = { _: String ->
        (System.currentTimeMillis() - timeLineMocker) / 1000
    }


    private fun startBarrage(): BarrageSurfaceView? {
        return BarrageDataStore.start(this, "11111", onProgressGet)
    }
}