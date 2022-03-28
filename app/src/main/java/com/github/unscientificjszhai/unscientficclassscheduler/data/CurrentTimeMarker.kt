package com.github.unscientificjszhai.unscientficclassscheduler.data

import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.FormattedTime
import java.lang.ref.WeakReference
import java.util.*
import kotlin.reflect.KProperty

/**
 * 标注现在所处时间的标记类，也提供一些基于当前时间的实用功能，比如给课程排序。
 *
 * @param courseTable 课程表对象。
 * @author UnscientificJsZhai
 */
class CurrentTimeMarker(private var courseTable: CourseTable) {

    /**
     * 为[CurrentTimeMarker]提供属性委托功能。
     */
    interface Getter {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): CurrentTimeMarker
    }

    /**
     * 用于排序比较多个已经确认在同一天有效的ClassTime对象谁先谁后的比较用包装类。
     *
     * @param classTime 比较的对象。
     * @param courseWithClassTimes 比较对象对应的Course整体。
     */
    private inner class ClassTimeCompareOperator(
        val classTime: ClassTime,
        val courseWithClassTimes: CourseWithClassTimes
    ) : Comparable<ClassTimeCompareOperator> {

        override operator fun compareTo(other: ClassTimeCompareOperator): Int {
            val thisNumerical = classTime.start * 100 + classTime.end
            val otherNumerical = other.classTime.start * 100 + other.classTime.end
            return thisNumerical - otherNumerical
        }
    }

    /**
     * 计算当前日期是学期的第几周。
     * 会获取当前时间作为第二个参数进行计算。
     *
     * @return 如果一个星期的星期二被设定为这个学期的开始日，那么这个星期的星期一到星期六都返回1。如果学期尚未开始，返回0。
     */
    fun getWeekNumber(): Int {
        val startDate = this.courseTable.startDate
        val nowDate = Calendar.getInstance()
        val absoluteDateDistance = getDateDistance(
            startDate,
            nowDate
        ) + startDate.get(Calendar.DAY_OF_WEEK) - if (courseTable.weekStart) { // 一周开始日
            2
        } else {
            1
        }

        return if (absoluteDateDistance > -1) {
            1 + absoluteDateDistance / 7
        } else 0
    }

    /**
     * 返回当前在第几节课。0.5意味着在今天所有课上课之前。1.5意味着已经下了第一节课但是第二节课还没有上课。
     *
     * @param courseTable 获取时间表。
     */
    fun nowLessonNumber(courseTable: CourseTable): Double {
        val timeTable = courseTable.timeTable
        val now = Calendar.getInstance()
        var answer = 0.0

        for (time in timeTable) {
            val formattedTime = FormattedTime(time)
            when (formattedTime.isDuring(now)) {
                FormattedTime.BEFORE -> {
                    answer += 0.5
                    break
                }
                FormattedTime.DURING -> {
                    answer += 1
                    break
                }
                FormattedTime.AFTER -> {
                    answer += 1
                }
                else -> {
                    throw RuntimeException("Return Type Error")
                }
            }
        }

        return answer
    }

    /**
     * 计算两天的日期差距。
     *
     * @param from 开始日期。
     * @param to 结束日期。
     * @return 日期差，整数。
     */
    private fun getDateDistance(from: Calendar, to: Calendar): Int {
        val day1 = from.get(Calendar.DAY_OF_YEAR)
        val day2 = to.get(Calendar.DAY_OF_YEAR)

        val year1 = from.get(Calendar.YEAR)
        val year2 = to.get(Calendar.YEAR)

        return if (year1 == year2) {
            day2 - day1
        } else {
            var timeDistance = 0
            for (i in year1 until year2) {
                timeDistance += if (i % 4 == 0 && i % 100 != 0 || i % 400 == 0) {
                    366
                } else {
                    365
                }
            }
            timeDistance + day2 - day1
        }
    }

    /**
     * 获取今天有课的课程列表。
     *
     * @param originalList 完整的列表。
     * @return 新的列表，只包括今天上的课程。
     */
    fun getTodayCourseList(originalList: List<CourseWithClassTimes>): ArrayList<CourseWithClassTimes> {
        val weekNumber = this.getWeekNumber()
        if (weekNumber == 0) {
            // 学期未开始的时候返回空表
            return ArrayList()
        }
        val nowDate = Calendar.getInstance()
        val classTimes = ArrayList<ClassTimeCompareOperator>()
        val newList = ArrayList<CourseWithClassTimes>()

        for (courseWithClassTimes in originalList) {
            for (classTime in courseWithClassTimes.classTimes) {
                if (classTime.getWeekData(weekNumber) &&
                    (classTime.whichDay + 1) == nowDate.get(Calendar.DAY_OF_WEEK)
                ) {
                    classTimes.add(ClassTimeCompareOperator(classTime, courseWithClassTimes))
                }
            }
        }

        classTimes.sort()
        classTimes.forEach {
            val courseWithClassTimes = CourseWithClassTimes(it.courseWithClassTimes)
            courseWithClassTimes.course.specificClassTime = WeakReference(it.classTime)
            newList.add(courseWithClassTimes)
        }

        return newList
    }

    /**
     * 更新CourseTable的方法。
     *
     * @param courseTable 更改后的课程表对象。
     */
    fun setCourseTable(courseTable: CourseTable) {
        this.courseTable = courseTable
    }
}