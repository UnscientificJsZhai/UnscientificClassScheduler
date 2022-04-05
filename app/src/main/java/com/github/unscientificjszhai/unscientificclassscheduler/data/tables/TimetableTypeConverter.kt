package com.github.unscientificjszhai.unscientificclassscheduler.data.tables

import androidx.room.TypeConverter
import java.util.*

/**
 * TypeConverter类，用于将[CourseTable]类中的记录具体上课时间的StringArray转化为String来存储。
 *
 * @see CourseTable
 * @author UnscientificJsZhai
 */
class TimetableTypeConverter {

    @TypeConverter
    fun getTimeTable(value: String): Array<String> {
        return value.split(',').toTypedArray()
    }

    @TypeConverter
    fun setTimeTable(value: Array<String>): String {
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

    @TypeConverter
    fun getStartDate(value: String): Calendar {
        val calendar = Calendar.getInstance()
        val year: Int = value.subSequence(0, 4).toString().toInt()
        // 在Calendar类中Month字段0表示1月
        val month: Int = value.subSequence(4, 6).toString().toInt() - 1
        val day: Int = value.subSequence(6, 8).toString().toInt()
        calendar.clear()
        calendar.set(year, month, day)
        return calendar
    }

    @TypeConverter
    fun setStartDate(value: Calendar): String {
        val year = value.get(Calendar.YEAR).toString()
        val m: Int = value.get(Calendar.MONTH) + 1
        val d = value.get(Calendar.DAY_OF_MONTH)

        val month: String = if (m < 10) {
            "0$m"
        } else {
            m.toString()
        }

        val day = if (d < 10) {
            "0$d"
        } else {
            d.toString()
        }

        return year + month + day
    }
}