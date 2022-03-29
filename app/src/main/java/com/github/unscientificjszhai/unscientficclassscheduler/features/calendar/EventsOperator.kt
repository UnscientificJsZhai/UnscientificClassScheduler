package com.github.unscientificjszhai.unscientficclassscheduler.features.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.FormattedTime
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 事件操作工具对象。对所有事件的操作都在这里完成。
 *
 * @see CalendarOperator
 * @author UnscientificJsZhai
 */
@Singleton
class EventsOperator @Inject constructor() {

    companion object {

        private const val TAG = "EventOperator"
    }

    /**
     * 按连续上课周分割好的上课时间。会把每个ClassTime对象从每个上课周数的断点处分割开，保存成不同的对象。
     *
     * @param root 原ClassTime的弱引用。
     * @param start 开始周数。
     * @param end 结束周数。
     */
    private data class SplitClassTime(
        val root: WeakReference<ClassTime>,
        var start: Int,
        var end: Int
    ) {

        /**
         * 最主要使用的构造函数，在构造函数中生成弱引用。
         * 以此构造函数生成的分段对象，长度是1，需要再更新[end]。不更新即表示这个分段的长度就是1。
         *
         * @param root 原ClassTime对象。
         * @param start 开始周数。
         */
        constructor(root: ClassTime, start: Int) : this(WeakReference(root), start, start)

        /**
         * 通过ClassTime对象中的数据确定向ContentValues中插入的关于日期时间的数据。
         *
         * @param values 待编辑的ContentValues对象。
         * @return 参数中的对象。
         */
        fun setTimeValue(values: ContentValues, courseTable: CourseTable) = values.apply {

            val classTime: ClassTime = root.get()!!

            val startTime = courseTable.startDate.clone() as Calendar
            startTime.set(Calendar.WEEK_OF_YEAR, startTime.get(Calendar.WEEK_OF_YEAR) + (start - 1))
            startTime.set(Calendar.DAY_OF_WEEK, classTime.whichDay + 1)
            val formattedStartTime = FormattedTime(courseTable.timeTable[classTime.start - 1])
            startTime.set(Calendar.HOUR_OF_DAY, formattedStartTime.startH)
            startTime.set(Calendar.MINUTE, formattedStartTime.startM)

            //以上是周日为一周开始的情况，当周一为一周开始时，进行以下操作平移周日。
            if (courseTable.weekStart && startTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                //向后平移7天。
                startTime.set(Calendar.WEEK_OF_YEAR, startTime.get(Calendar.WEEK_OF_YEAR) + 1)
            }

            this.put(CalendarContract.Events.DTSTART, startTime.timeInMillis)

            val formattedEndTime = FormattedTime(courseTable.timeTable[classTime.end - 1])
            val durationText =
                "P${FormattedTime.duration(formattedStartTime, formattedEndTime) * 60}S"
            put(CalendarContract.Events.DURATION, durationText)

            put(
                CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=${end - start + 1};WKST=${
                    when (startTime.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> "SU"
                        Calendar.MONDAY -> "MO"
                        Calendar.TUESDAY -> "TU"
                        Calendar.WEDNESDAY -> "WE"
                        Calendar.THURSDAY -> "TH"
                        Calendar.FRIDAY -> "FR"
                        else -> "SA"
                    }
                }"
            )
        }

        override fun toString() = "$start to $end"
    }

    /**
     * 把一个课程添加到日历中。根据[ClassTime]的时间，每个时间段的每段连续上课的几周是一个Event项目。
     * 但是规律性不连续（比如双周上课）的，不是一个连续的Event项目。
     * 调用时需要注意，此方法还会修改Course中的数据且未保存。需要手动保存。
     *
     * @param context 插入操作的上下文。
     * @param courseTable 要插入的课程项所属的课程表。
     * @param event 课程和上课时间。
     */
    @WorkerThread
    fun addEvent(
        context: Context,
        courseTable: CourseTable,
        event: CourseWithClassTimes
    ): CourseWithClassTimes {
        val calendarID = courseTable.calendarID
        if (calendarID != null) {
            event.course.associatedEventsId = ArrayList() //清空之前的关联
            for (classTime in event.classTimes) {
                for (splitClassTime in classTime.split(courseTable.maxWeeks)) {
                    val values = ContentValues().apply {
                        put(CalendarContract.Events.TITLE, event.course.title)
                        put(CalendarContract.Events.CALENDAR_ID, calendarID)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        put(CalendarContract.Events.DESCRIPTION, classTime.getCalendarDescription())

                        splitClassTime.setTimeValue(this, courseTable)
                    }

                    //插入并获取ID
                    val lastPathSegment = context.contentResolver.insert(
                        CalendarContract.Events.CONTENT_URI,
                        values
                    )?.lastPathSegment

                    if (lastPathSegment != null) {
                        val eventID = lastPathSegment.toLong()
                        event.course.associatedEventsId.add(eventID)

                        //添加提醒时间
                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(context)
                        val time = sharedPreferences.getString("remindTime", "15")
                        val remindValues = ContentValues().apply {
                            put(CalendarContract.Reminders.MINUTES, time)
                            put(CalendarContract.Reminders.EVENT_ID, eventID)
                            put(
                                CalendarContract.Reminders.METHOD,
                                CalendarContract.Reminders.METHOD_ALERT
                            )
                        }
                        context.contentResolver.insert(
                            CalendarContract.Reminders.CONTENT_URI,
                            remindValues
                        )
                    } else {
                        Log.e(TAG, "addEvent: Event ID is null!")
                    }
                }
            }
        }
        return event
    }

    /**
     * 删除所有和这个课程相关的日历项目。
     * 调用时需要注意，此方法还会修改Course中的数据且未保存。需要手动保存。
     *
     * @param context 删除操作的上下文。
     * @param courseTable 要删除的课程项所属的课程表。
     * @param event 课程和上课时间。
     */
    @WorkerThread
    fun deleteEvent(context: Context, courseTable: CourseTable, event: CourseWithClassTimes) {
        val calendarID = courseTable.calendarID
        if (calendarID != null) {
            for (id in event.course.associatedEventsId) {
                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                context.contentResolver.delete(deleteUri, null, null)
            }
            event.course.associatedEventsId.clear()
        }
    }

    /**
     * 更新所有和这个课程相关的日历项目。
     * 调用这个方法相当于调用了[deleteEvent]和[addEvent]这两个方法。
     * 数据未保存，需要手动保存。
     *
     * @param context 更新操作的上下文。
     * @param courseTable 要更新的课程项所属的课程表。
     * @param event 课程和上课时间。
     */
    @WorkerThread
    fun updateEvent(
        context: Context,
        courseTable: CourseTable,
        event: CourseWithClassTimes
    ): CourseWithClassTimes {
        deleteEvent(context, courseTable, event)
        return addEvent(context, courseTable, event)
    }

    /**
     * 更新全部事件。适用于更改课程表属性后更新全部事件。
     * 实际上是删除了全部事件并重新添加。
     * 更新后的数据立即保存。
     *
     * @param context 更新操作的上下文。
     * @param courseTable 要更新的课程表。
     */
    @WorkerThread
    fun updateAllEvents(context: Context, courseTable: CourseTable) {
        val courseDao =
            (context.applicationContext as SchedulerApplication).getCourseDatabase().courseDao()
        val courses = courseDao.getCourses()

        for (courseWithClassTimes in courses) {
            deleteEvent(context, courseTable, courseWithClassTimes)
            addEvent(context, courseTable, courseWithClassTimes)
            courseDao.updateCourse(courseWithClassTimes.course)
        }
    }

    /**
     * 把一个ClassTime分割成多个对象。每个对象对应日历中一个日程事件。分割是为了向日历添加这些事件。
     *
     * @param range 分割查找范围。应该等于[CourseTable.maxWeeks]。
     * @return 分割后的对象的列表。通过遍历这个列表来添加事件。
     */
    private fun ClassTime.split(range: Int): List<SplitClassTime> {
        val list = ArrayList<SplitClassTime>()
        var index = 1

        do {
            if (getWeekData(index)) {
                val element = SplitClassTime(this, index)
                if (index < range) {
                    //如果当前指向的不是最后一个
                    for (subIndex in index + 1..range) {
                        if (!getWeekData(subIndex)) {
                            element.end = subIndex - 1
                            list.add(element)
                            index = subIndex + 1
                            break
                        } else if (subIndex >= range) {
                            //进入此分支，则最后一周已经有效
                            element.end = subIndex
                            list.add(element)
                            index = subIndex + 1
                        }
                    }
                } else if (index == range) {
                    list.add(SplitClassTime(this, index))
                }
            } else {
                index += 1
            }
        } while (index <= range)

        return list
    }

    /**
     * 生成每个上课时间段的用于系统日历的描述性文字。
     *
     * @return 教师姓名 @上课地点
     */
    private fun ClassTime.getCalendarDescription(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(this.teacherName)
        if (this.location.isNotBlank()) {
            stringBuilder.append(" @").append(this.location)
        }
        return stringBuilder.toString()
    }
}