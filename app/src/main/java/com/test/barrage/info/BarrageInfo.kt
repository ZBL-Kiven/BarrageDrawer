package com.test.barrage.info

import com.test.barrage.factory.BarrageRepository

class BarrageInfo {
    var start: Float = 0f
    var top: Float = 0f
    var ballistic: Int = -1
    var width: Float = 0f
    var height: Float = 0f
    var step: Int = 5
    var lastStart: Float = 0f
    var data: BarrageRepository.Barrage? = null
    var stable: Boolean = true
    var ratio: Float = 1.0f
        get() {
            return if (stable) 0.0f else field
        }
}