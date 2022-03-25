package com.github.unscientificjszhai.unscientficclassscheduler.data.course

import androidx.room.TypeConverter

/**
 * 将[Course]类中存储关联的日历事件的数组转化为可存储在Room数据库中的格式的TypeConverter类。
 *
 * @see Course
 * @author UnscientificJsZhai
 */
class CourseEventsConverter {

    @TypeConverter
    fun getIDs(value: String): ArrayList<Long> {
        val list = ArrayList<Long>()
        if (value.isNotBlank()) {
            for (s in value.split(',')) {
                list.add(s.toLong())
            }
        }
        return list
    }

    @TypeConverter
    fun setIDs(value: ArrayList<Long>): String {
        val str = StringBuilder()
        for (index in value.indices) {
            if (index == 0) {
                str.append(value[index])
            } else {
                str.append(",").append(value[index])
            }
        }
        return str.toString()
    }
}