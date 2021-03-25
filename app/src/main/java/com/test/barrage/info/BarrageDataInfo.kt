package com.test.barrage.info

import kotlin.random.Random


class BarrageDataInfo {

    companion object {
        var forTestTxt: Int = 0
            get() {
                return field++
            }
        var timeLineTest: Long = 0
            get() {
                field += Random.nextInt(1000)
                return field
            }
    }

    var uid: String = ""
    var text: String = ""
    var timeLine = timeLineTest

    fun isSelf(): Boolean {
        return uid == "1"
    }
}