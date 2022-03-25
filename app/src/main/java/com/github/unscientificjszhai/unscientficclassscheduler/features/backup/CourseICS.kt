package com.github.unscientificjszhai.unscientficclassscheduler.features.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.FormattedTime
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.ProgressDialog
import com.github.unscientificjszhai.unscientficclassscheduler.util.with0
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * 生成Course对象的ICS格式字符串的类。
 *
 * @param courseList 课程的集合。
 * @param courseTable 课程表对象，用于获取时间表。
 * @author UnscientificJsZhai
 */
class CourseICS(
    private val courseList: List<CourseWithClassTimes>,
    private val courseTable: CourseTable,
) {

    companion object {

        /**
         * 生成用于启动导出ics过程的Intent。
         *
         * @param courseTable 将要导出的课程表对象，用于获取标题以决定文件名。
         * @return 生成的Intent。以这个Intent为参数调用[Activity.startActivityForResult]或者[ActivityResultLauncher.launch]方法，
         * 启动系统文件管理器选择文件存储。
         */
        @JvmStatic
        fun getExportIcsIntent(courseTable: CourseTable) =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/calendar"
                putExtra(Intent.EXTRA_TITLE, "${courseTable.name}.ics")
            }
    }

    /**
     * 按连续上课周分割好的上课时间。会把每个ClassTime对象从每个上课周数的断点处分割开，保存成不同的对象。
     *
     * @param root 原ClassTime的弱引用。
     * @param start 开始周数。
     * @param end 结束周数。
     */
    inner class SplitClassTime(
        private val root: WeakReference<ClassTime>,
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
         * 获取这个分段的DTSTART和DTEND文本。
         *
         * @param courseTable 用于获取上课时间表的courseTable对象。
         * @param classTime 用于获取周几上课的classTime对象。
         * @param isStart 决定返回的文本是DTSTART还是DTEND。
         * @return 参数isStart是True则返回DTSTART，FALSE则返回DTEND。
         */
        private fun getTime(
            courseTable: CourseTable,
            classTime: ClassTime,
            isStart: Boolean
        ): String {
            val calendarObject = courseTable.startDate.clone() as Calendar
            calendarObject.set(
                Calendar.WEEK_OF_YEAR,
                calendarObject.get(Calendar.WEEK_OF_YEAR) + (start - 1)
            )
            calendarObject.set(Calendar.DAY_OF_WEEK, classTime.whichDay + 1)
            if (isStart) {
                val formattedStartTime = FormattedTime(courseTable.timeTable[classTime.start - 1])
                calendarObject.set(Calendar.HOUR_OF_DAY, formattedStartTime.startH)
                calendarObject.set(Calendar.MINUTE, formattedStartTime.startM)
            } else {
                val formattedEndTime = FormattedTime(courseTable.timeTable[classTime.end - 1])
                calendarObject.set(Calendar.HOUR_OF_DAY, formattedEndTime.endH)
                calendarObject.set(Calendar.MINUTE, formattedEndTime.endM)
            }

            // 以上是周日为一周开始的情况，当周一为一周开始时，进行以下操作平移周日。
            if (courseTable.weekStart && calendarObject.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                // 向后平移7天。
                calendarObject.set(
                    Calendar.WEEK_OF_YEAR,
                    calendarObject.get(Calendar.WEEK_OF_YEAR) + 1
                )
            }

            calendarObject.get(Calendar.HOUR_OF_DAY) // 调用Calendar的complete()方法应用刚才的更新
            calendarObject.timeZone = TimeZone.getTimeZone("UTC")

            return "${calendarObject.get(Calendar.YEAR)}" +
                    (calendarObject.get(Calendar.MONTH) + 1).with0() +
                    calendarObject.get(Calendar.DAY_OF_MONTH).with0() +
                    "T" + calendarObject.get(Calendar.HOUR_OF_DAY).with0() +
                    calendarObject.get(Calendar.MINUTE).with0() +
                    "00Z"
        }

        /**
         * 获取这个分段的RRULE文本。
         *
         * @param courseTable 用于获取上课时间表的courseTable对象。
         * @param classTime 用于获取周几上课的classTime对象。
         * @return 生成的RRULE字符串。
         */
        private fun getRRule(courseTable: CourseTable, classTime: ClassTime): String {
            val calendarObject = courseTable.startDate.clone() as Calendar
            calendarObject.set(
                Calendar.WEEK_OF_YEAR,
                calendarObject.get(Calendar.WEEK_OF_YEAR) + (end - 1)
            )
            calendarObject.set(Calendar.DAY_OF_WEEK, classTime.whichDay + 1)
            val formattedStartTime = FormattedTime(courseTable.timeTable[classTime.start - 1])
            calendarObject.set(Calendar.HOUR_OF_DAY, formattedStartTime.startH)
            calendarObject.set(Calendar.MINUTE, formattedStartTime.startM)
            calendarObject.get(Calendar.HOUR_OF_DAY)// 调用Calendar的complete()方法应用刚才的更新

            // 以上是周日为一周开始的情况，当周一为一周开始时，进行以下操作平移周日。
            if (courseTable.weekStart && calendarObject.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                // 向后平移7天。
                calendarObject.set(
                    Calendar.WEEK_OF_YEAR,
                    calendarObject.get(Calendar.WEEK_OF_YEAR) + 1
                )
            }

            val until = "${calendarObject.get(Calendar.YEAR)}" +
                    (calendarObject.get(Calendar.MONTH) + 1).with0() +
                    calendarObject.get(Calendar.DAY_OF_MONTH).with0() +
                    "T" + calendarObject.get(Calendar.HOUR_OF_DAY).with0() +
                    calendarObject.get(Calendar.MINUTE).with0() +
                    "00Z"

            return "FREQ=WEEKLY;UNTIL=$until;INTERVAL=1"
        }

        /**
         * 生成ics字符段。生成的部分是VEVENT部分从头到尾。
         *
         * @param builder 用于构建字符串的StringBuilder，将在尾部继续添加。
         * @param course 本次时间段的Course对象，用于获取标题等数据。
         * @return 参数传入的StringBuilder对象。
         */
        fun vEventIcsString(builder: StringBuilder, course: Course): StringBuilder {
            val classTime: ClassTime = root.get()!!

            val dtStart =
                getTime(courseTable, classTime, true)
            val dtEnd = getTime(courseTable, classTime, false)
            val timeStampUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val timeStampString = "${timeStampUTC.get(Calendar.YEAR)}" +
                    (timeStampUTC.get(Calendar.MONTH) + 1).with0() +
                    timeStampUTC.get(Calendar.DAY_OF_MONTH).with0() +
                    "T" + timeStampUTC.get(Calendar.HOUR_OF_DAY).with0() +
                    timeStampUTC.get(Calendar.MINUTE).with0() +
                    "00Z"

            builder.append("BEGIN:VEVENT\n")
                .append("DTSTAMP:$timeStampString\n")
                .append("UID:TimeManager-${UUID.randomUUID()}\n")
                .append("SUMMARY:${course.title}\n")
                .append("DTSTART:$dtStart\n")
                .append("DTEND:$dtEnd\n")
                .append("RRULE:${getRRule(courseTable, classTime)}\n")

            if (classTime.location.isNotBlank()) {
                builder.append("LOCATION:${classTime.location}\n")
            }
            if (classTime.teacherName.isNotBlank()) {
                builder.apply {
                    append("DESCRIPTION:${classTime.teacherName}")
                    if (classTime.location.isNotBlank()) {
                        append(" @${classTime.location}\n")
                    } else {
                        append("\n")
                    }
                }
            }

            builder.append("BEGIN:VALARM\nACTION:DISPLAY\nTRIGGER;RELATED=START:-PT15M\n")
                .append("DESCRIPTION:${course.title}\n")
                .append("END:VALARM\nEND:VEVENT\n")

            return builder
        }

        override fun toString() = "$start to $end"
    }

    /**
     * 把一个ClassTime分割成多个对象。每个对象对应日历中一个日程事件。分割是为了向日历添加这些事件。
     *
     * @return 分割后的对象的列表。通过遍历这个列表来添加事件。
     */
    private fun ClassTime.split(): List<SplitClassTime> {
        val list = ArrayList<SplitClassTime>()
        var index = 1
        val range = courseTable.maxWeeks

        do {
            if (getWeekData(index)) {
                val element = SplitClassTime(this, index)
                if (index < range) {
                    // 如果当前指向的不是最后一个
                    for (subIndex in index + 1..range) {
                        if (!getWeekData(subIndex)) {
                            element.end = subIndex - 1
                            list.add(element)
                            index = subIndex + 1
                            break
                        } else if (subIndex >= range) {
                            // 进入此分支，则最后一周已经有效
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

    override fun toString(): String {
        if (this.courseList.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()

        builder.append(
            "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//UnscientificJsZhai//TimeManager//EN\n"
        )

        for (courseWithClassTimes in this.courseList) {
            for (classTime in courseWithClassTimes.classTimes) {
                for (splitTime in classTime.split()) {
                    splitTime.vEventIcsString(builder, courseWithClassTimes.course)
                }
            }
        }

        builder.append("END:VCALENDAR\n")

        return builder.toString()
    }

    /**
     * 将ics格式的字符串写入文件。在处理过程中，会在窗口上显示一个Dialog。
     * 这个方法应该在[Activity.onActivityResult]中被调用，
     * 或者在[AppCompatActivity.registerForActivityResult]中注册。
     *
     * @param context 进行备份操作的上下文，因为要显示Dialog，仅接受Activity。
     * @param uri 备份文件的uri，需要可以被写入。
     */
    @UiThread
    fun writeToFile(context: Activity, uri: Uri) {
        val progressDialog = ProgressDialog(context)
        progressDialog.show()
        thread(start = true) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                outputStream!!.write(this.toString().toByteArray(StandardCharsets.UTF_8))
                outputStream.close()
            } catch (e: IOException) {
                context.runOnUiThread {
                    Toast.makeText(
                        context,
                        R.string.activity_Settings_FailToBackup,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            progressDialog.postDismiss()
        }
    }
}