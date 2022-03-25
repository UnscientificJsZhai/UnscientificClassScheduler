package com.github.unscientificjszhai.unscientficclassscheduler.data.tables

import java.util.*

/**
 * 时间段封装类型。
 *
 * @throws NumberFormatException 传入的字符串不是纯数字时抛出此异常。
 * @throws IndexOutOfBoundsException 传入的字符串长度小于8时抛出此异常。
 * @author UnscientificJsZhai
 */
class FormattedTime {

    companion object {

        /**
         * 获取两节课之间的时间间隔，单位：分钟。
         *
         * @param start 开始。
         * @param end 结束。
         * @return 以分钟为单位的时间间隔。
         */
        @JvmStatic
        fun duration(start: FormattedTime, end: FormattedTime) =
            (end.endH - start.startH) * 60 + end.endM - start.startM

        /**
         * 表示给定时间在这个时间段之前。
         */
        const val BEFORE = -1

        /**
         * 表示给定时间在这个时间段之中。
         */
        const val DURING = 0

        /**
         * 表示给定时间在这个时间段之后。
         */
        const val AFTER = 2

    }

    var startH: Int
        set(value) {
            if (value in 0..23) {
                field = value
            }
        }
    var startM: Int
        set(value) {
            if (value in 0..59) {
                field = value
            }
        }
    var endH: Int
        set(value) {
            if (value in 0..23) {
                field = value
            }
        }
    var endM: Int
        set(value) {
            if (value in 0..59) {
                field = value
            }
        }

    /**
     * 通过给定8位只有数字的字符串，初始化对象。
     *
     * @param string 输入的字符串，长度至少为8。每一位都必须是数字。
     */
    constructor(string: String) {
        startH = String(charArrayOf(string[0], string[1])).toInt()
        startM = String(charArrayOf(string[2], string[3])).toInt()
        endH = String(charArrayOf(string[4], string[5])).toInt()
        endM = String(charArrayOf(string[6], string[7])).toInt()

        if (startH !in 0..23 || startM !in 0..59 || endH !in 0..23 || endM !in 0..59) {
            throw RuntimeException()
        }
    }

    /**
     * 通过多节课的上下课时间合成一个总的上下课时间。
     *
     * @param start 开始的那节课的时间。
     * @param end 结束的那节课的时间。
     */
    constructor(start: FormattedTime, end: FormattedTime) {
        startH = start.startH
        startM = start.startM
        endH = end.endH
        endM = end.endM
    }

    /**
     * 获取持续时间。
     *
     * @return 结束时间减开始时间，单位分钟。
     */
    fun duration() = if (startH > endH) {
        0
    } else if (startH == endH && startM > endM) {
        0
    } else {
        60 * (endH - startH) + endM - startM
    }

    /**
     * 根据初始时间和间隔设置结束时间。
     *
     * @param duration 间隔，单位分钟。
     */
    fun autoSetEndTime(duration: Int) {
        var hour = this.startH
        var min = this.startM
        min += duration
        while (min >= 60) {
            min -= 60
            hour += 1
        }
        while (hour >= 24) {
            hour -= 24
        }
        this.endH = hour
        this.endM = min
    }

    /**
     * 判断给定的事件是否在这个时间段内。
     *
     * @param time 给定的用[Calendar]类表示。
     * @return 是否在时间段内。
     * @see BEFORE
     * @see DURING
     * @see AFTER
     */
    fun isDuring(time: Calendar): Int {
        val timeFormat = ((time.get(Calendar.HOUR_OF_DAY)) * 60) + time.get(Calendar.MINUTE)
        val startFormat = startH * 60 + startM
        val endFormat = endH * 60 + endM

        return when {
            timeFormat < startFormat -> {
                BEFORE
            }
            timeFormat >= endFormat -> {
                AFTER
            }
            else -> {
                DURING
            }
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        arrayOf(this.startH, this.startM, this.endH, this.endM).forEach { number ->
            if (number < 10) {
                stringBuilder.append(0)
            }
            stringBuilder.append(number)
        }
        return stringBuilder.toString()
    }

    override fun equals(other: Any?) = if (other is FormattedTime) {
        this.toString() == other.toString()
    } else {
        false
    }

    override fun hashCode() = this.toString().hashCode()
}