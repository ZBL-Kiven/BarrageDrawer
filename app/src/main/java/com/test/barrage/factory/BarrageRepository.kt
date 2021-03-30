package com.test.barrage.factory

import android.util.Log
import java.util.*
import kotlin.Comparator
import kotlin.random.Random

/**

 * Author: luzheng

 * Date: 2021/3/23 6:23 PM

 * Description:

 */
object BarrageRepository {

    /**
     * If current time line arrived the point of cache size minus [PREFETCH_THRESHOLD], load next page barrages into cache.
     */
    private const val PREFETCH_THRESHOLD = 5

    /**
     * The range of video time to load corresponding barrages.
     */
    private const val PAGE_SIZE = 10

    /**
     * Buffer queue sorted by time and priority for surface draw.
     */
    private const val CACHE_SORTED_BARRAGE_COUNT = 30

    /**
     * The whole barrage buffer which contains the recent and soon (range [mCachedTimeEnd] to [mCachedTimeEnd] + [PAGE_SIZE]) barrages
     */
    private val mBarrageQueue: LinkedList<Barrage> = LinkedList()

    /**
     * The sorted barrage buffer for surface poll barrage to draw.
     */
    private var mSortedBarrageQueue: LinkedList<Barrage> = LinkedList()

    /**
     * The token for video uniqueness check.
     */
    private var mToken: String = ""

    /**
     * The current video play time point.
     * Unit: second
     */
    private var mTimeLine: Int = 0

    /**
     * The time line which it's barrage list had been loaded in cache queue [mBarrageQueue].
     */
    private var mCachedTimeEnd = 0

    private var mockedTime = 0
        get() {
            return field++
        }

    private fun loadBarrage(token: String, start: Int, end: Int) {
        mToken = token
        mCachedTimeEnd = end
        mBarrageQueue.iterator().apply {
            while (hasNext()) {
                val barrage = next()
                if ((barrage.timeLine) < mTimeLine) {
                    remove()
                } else {
                    break
                }
            }
        }
        // TODO: 2021/3/30 网络请求返回后的线程中锁住 mBarrageQueue添加数据
        for (i in 0 until 150) {
            val barrage = Barrage()
            barrage.timeLine = mockedTime
            barrage.userId = Random.nextInt(1000000)
            barrage.priority = Random.nextInt(1)
            barrage.content = "${barrage.userId}==${barrage.timeLine}"
            mBarrageQueue.add(barrage)
        }
        if (mSortedBarrageQueue.size < CACHE_SORTED_BARRAGE_COUNT) {
            fillSortedBarrageQueue()
            Log.e(
                "luzheng",
                "loadBarrage -> fillSortedBarrageQueue: $mSortedBarrageQueue    @@@@$mTimeLine"
            )
        }
    }

    private fun fillSortedBarrageQueue() {
        val fillCount = CACHE_SORTED_BARRAGE_COUNT - mSortedBarrageQueue.size
        for (i in 0 until fillCount.coerceAtMost(mBarrageQueue.size)) {
            val barrage = mBarrageQueue.poll()
            barrage?.let {
                if (!mSortedBarrageQueue.contains(it)) {
                    mSortedBarrageQueue.offer(barrage)
                }
            }
        }
        Collections.sort(mSortedBarrageQueue, Comparator { barrage1, barrage2 ->
            return@Comparator when (val timeOffset = barrage1.timeLine - barrage2.timeLine) {
                0 -> (barrage2.priority - barrage1.priority)
                else -> timeOffset
            }
        })
        mSortedBarrageQueue.iterator().apply {
            while (hasNext()) {
                val barrage = next()
                if ((barrage.timeLine) < mTimeLine) {
                    remove()
                } else {
                    break
                }
            }
        }
    }

    fun commitBarrage(content: String) {
        synchronized(mSortedBarrageQueue) {
            val barrage = Barrage()
            barrage.content = content
            barrage.priority = Barrage.PRIORITY_LOCAL_SEND
            //        barrage.userId = LoginUtils.userId
            barrage.userId = 1
            barrage.timeLine = mTimeLine
            mSortedBarrageQueue.addFirst(barrage)
        }
    }

    fun pollBarrage(timeLine: Int, token: String): Barrage? {
        if (token != mToken) {
            mSortedBarrageQueue.clear()
            loadBarrage(token, timeLine, timeLine + PAGE_SIZE)
            return null
        }
        mTimeLine = timeLine
        if (mTimeLine >= mCachedTimeEnd - PREFETCH_THRESHOLD) {
            loadBarrage(token, mCachedTimeEnd, mCachedTimeEnd + PAGE_SIZE)
        }
        return mSortedBarrageQueue.poll()?.apply {
            fillSortedBarrageQueue()
            if (isSelf()) Log.e(
                "luzheng",
                "pollBarrage -> fillSortedBarrageQueue: $mSortedBarrageQueue  ~~~~~$mTimeLine   @@@@$mCachedTimeEnd"
            )
        }
    }

    fun release() {
        mSortedBarrageQueue.clear()
        mBarrageQueue.clear()
        mToken = ""
        mTimeLine = 0
        mCachedTimeEnd = 0
    }

    class Barrage {

        companion object {
            const val PRIORITY_NORMAL = 0
            const val PRIORITY_LOCAL_SEND = 1
        }

        /**
         * 弹幕id
         */
        var id: Int = 0

        /**
         * 弹幕发送时视频的播放时间点
         */
        var timeLine: Int = 0

        /**
         * 弹幕内容
         */
        var content: String = ""

        /**
         * 弹幕发送人id
         */
        var userId: Int = 0

        /**
         * 非接口字段
         * 弹幕优先级
         */
        var priority: Int = PRIORITY_NORMAL

        override fun toString(): String {
            return "$userId==$timeLine"
        }

        fun isSelf(): Boolean {
            return userId == 1
        }
    }
}