package com.zj.danmaku

import android.util.Log
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
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
    private val mBarrageQueue: MutableList<Barrage> = Collections.synchronizedList(ArrayList<Barrage>())

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
     * The last index which the cache sorted barrage window skid over.
     */
    private var mSortedIndex = 0

    /**
     * The time line which it's barrage list had been loaded in cache queue [mBarrageQueue].
     */
    private var mCachedTimeEnd = 0


    var mockedTime = 2
    private fun loadBarrage(token: String, start: Int, end: Int) {
        mToken = token
        mCachedTimeEnd = end
        for (i in 0 until 150) {
            val barrage = Barrage()
            mockedTime = mTimeLine + Random.nextInt(5)
            barrage.timeLine = mockedTime
            barrage.userId = Random.nextInt(100)
            barrage.priority = Random.nextInt(1)
            barrage.content = "content : ${barrage.timeLine}"
            mBarrageQueue.add(barrage)
        }
        if (mSortedBarrageQueue.size < CACHE_SORTED_BARRAGE_COUNT) {
            fillSortedBarrageQueue()
            Log.e("luzheng", "loadBarrage -> fillSortedBarrageQueue: $mSortedBarrageQueue    @@@@$mTimeLine")
        }
    }

    private fun fillSortedBarrageQueue() {
        val firstIndex = mSortedIndex
        if (firstIndex < 0) return
        val fillCount = CACHE_SORTED_BARRAGE_COUNT - mSortedBarrageQueue.size
        mSortedIndex = (firstIndex + fillCount).coerceAtMost(mBarrageQueue.size)
        for (i in firstIndex until mSortedIndex) {
            val barrage = mBarrageQueue[i]
            if (!mSortedBarrageQueue.contains(barrage)) mSortedBarrageQueue.offer(barrage)
        }
        Collections.sort(mSortedBarrageQueue, Comparator { barrage1, barrage2 ->
            return@Comparator when (val timeOffset = barrage1.timeLine - barrage2.timeLine) {
                0 -> (barrage2.priority - barrage1.priority)
                else -> timeOffset
            }
        })
        mSortedBarrageQueue.iterator().apply {
            while (hasNext()) {
                if (next().timeLine < mTimeLine) {
                    remove()
                }
            }
        }
    }

    fun commitBarrage(content: String) {
        synchronized(mSortedBarrageQueue) {
            val originalFirst = mSortedBarrageQueue.peek()
            val barrage = Barrage()
            barrage.content = content
            barrage.priority = Barrage.PRIORITY_LOCAL_SEND
            //        barrage.userId = LoginUtils.userId
            barrage.userId = 1
            barrage.timeLine = mTimeLine + 150
            mSortedBarrageQueue.addFirst(barrage)
            originalFirst?.let {
                val originalFirstIndex = mBarrageQueue.indexOfFirst { originalFirst == it }
                if (originalFirstIndex != -1) {
                    mBarrageQueue.add(originalFirstIndex + 1, barrage)
                } else {
                    mBarrageQueue.add(barrage)
                }
            }
            //            Log.e("luzheng", "commitBarrage: $mSortedBarrageQueue ")
        }
    }

    @Synchronized
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
        return mSortedBarrageQueue.poll().apply {
            fillSortedBarrageQueue()
            //            Log.e("luzheng", "pollBarrage -> fillSortedBarrageQueue: $mSortedBarrageQueue  ~~~~~$mTimeLine   @@@@$mCachedTimeEnd")
        }
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
            return "$timeLine"
        }

        fun isSelf(): Boolean {
            return userId == 1
        }
    }
}