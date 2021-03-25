package com.test.barrage.info

import com.zj.danmaku.BarrageRepository

class BarrageInfo {
    var start: Float = 0f
    var top: Float = 0f
    var ballistic: Int = -1
    var width: Float = 0f
    var height: Float = 0f
    var ratio: Float = 1.0f
    var step: Int = 3
    var lastStart: Float = 0f
    var data: BarrageRepository.Barrage? = null
}