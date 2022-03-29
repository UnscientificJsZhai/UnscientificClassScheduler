package com.github.unscientificjszhai.unscientificclassscheduler.data.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.util.*
import kotlin.reflect.KProperty

/**
 * 数据类，用于封装课程表信息。
 *
 * @param id 课程表的ID。
 * @param name 课程表的名称。
 * @param classesPerDay 每天的课程数。
 * @param maxWeeks 学期教学周数。
 * @param timeTable 学期上课时间安排表。
 * @param startDate 学期开始日。
 * @param weekStart 教学周开始日。true为周一，false为周日。
 * @author UnscientificJsZhai
 */
@Entity(tableName = CourseTable.TABLE_NAME)
@TypeConverters(TimetableTypeConverter::class)
data class CourseTable(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    var name: String,
    @ColumnInfo(name = "class_per_day") var classesPerDay: Int,
    @ColumnInfo(name = "max_weeks") var maxWeeks: Int,
    @ColumnInfo(name = "time_table") var timeTable: Array<String>,
    @ColumnInfo(name = "start_date") var startDate: Calendar,
    @ColumnInfo(name = "calendar_id") var calendarID: Long?,
    @ColumnInfo(name = "week_start") var weekStart: Boolean,
) : Serializable {

    companion object {

        /**
         * 表的名字。
         */
        const val TABLE_NAME = "course_table"

        /**
         * 每天的最大课程数量。
         */
        const val MAX_CLASS_PER_DAY = 15

        /**
         * 默认每天的课程数量。
         */
        const val DEFAULT_CLASS_PER_DAY = 13

        /**
         * 默认学期教学周数。最大值见下。
         *
         * @see ClassTime.MAX_STORAGE_SIZE
         */
        const val DEFAULT_MAX_WEEKS = 18

        /**
         * 获得默认的时间表对象。
         */
        fun defaultTimeTable() = arrayOf(
            "08300915",
            "09251010",
            "10301115",
            "11251210",
            "12201305",
            "13051350",
            "14001445",
            "14551540",
            "16001645",
            "16551740",
            "19001945",
            "19552040",
            "20402125",
            "00000000",
            "00000000"
        )

        /**
         * 从Json中解析出一个课程表对象。
         *
         * @return 生成的CourseTable对象。
         * @exception JSONException 当Json解析出错时抛出此错误。
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun parseJson(jsonString: String): CourseTable {
            val jsonObject = JSONObject(jsonString)
            val converter = TimetableTypeConverter()
            val name = jsonObject.getString("name")
            val timeTable = converter.getTimeTable(jsonObject.getString("timeTable"))
            val courseTable = CourseTable(name, timeTable)
            courseTable.classesPerDay = jsonObject.getInt("classPerDay")
            courseTable.maxWeeks = jsonObject.getInt("maxWeeks")
            courseTable.startDate = converter.getStartDate(jsonObject.getString("startDate"))

            if (jsonObject.has("weekStart")) {
                courseTable.weekStart = jsonObject.getBoolean("weekStart")
            } else {
                courseTable.weekStart = false
            }

            return courseTable
        }
    }

    /**
     * 为[CourseTable]提供属性委托功能。
     */
    interface Getter {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): CourseTable
    }

    /**
     * 默认构造函数。
     *
     * @param name 课程表标题。
     * @param timeTable 上课时间表。可选参数。
     */
    constructor(name: String, timeTable: Array<String> = defaultTimeTable()) : this(
        id = null,
        name = name,
        classesPerDay = DEFAULT_CLASS_PER_DAY,
        maxWeeks = DEFAULT_MAX_WEEKS,
        timeTable = timeTable,
        startDate = Calendar.getInstance(),
        calendarID = null,
        weekStart = true
    )

    /**
     * 获得指定节次的上下课时间的方法。
     *
     * @param which 节次。第一节课即为1。
     */
    @Deprecated("暂时用不到的方法")
    fun getTime(which: Int): FormattedTime {
        if (which > this.classesPerDay || which < 1) {
            throw IndexOutOfBoundsException()
        } else {
            return FormattedTime(timeTable[which - 1])
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CourseTable

        if (id != other.id) return false
        if (name != other.name) return false
        if (classesPerDay != other.classesPerDay) return false
        if (!timeTable.contentEquals(other.timeTable)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + classesPerDay
        result = 31 * result + timeTable.contentHashCode()
        return result
    }

    /**
     * 有时学期开始日不是一周的开始日。处理方法是无视这种情况。所有的学期开始日都会被转换为周日。
     * 这个方法可以完成转换。
     *
     * @return 转换后的学期开始日。
     */
    @Deprecated("暂时用不到的方法")
    fun getStartDateInSunday(): Calendar {
        var startDate = this.startDate.clone() as Calendar
        while (startDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            startDate = startDate.yesterday()
        }
        return startDate
    }

    /**
     * 得到前一天的Calendar对象。
     *
     * @return 前一天的Calendar对象。
     */
    private fun Calendar.yesterday(): Calendar {
        if (this.get(Calendar.DATE) == 1) {
            if (this.get(Calendar.MONTH) == Calendar.JANUARY) {
                this.set(this.get(Calendar.YEAR - 1), Calendar.DECEMBER, 31)
            } else {
                val month = this.get(Calendar.MONTH) - 1
                this.set(Calendar.MONTH, month)
                this.set(
                    Calendar.DATE, when (month) {
                        1, 3, 5, 7, 8, 10, 12 -> 31
                        4, 6, 9, 11 -> 30
                        else -> {
                            val year = this.get(Calendar.YEAR)
                            if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) {
                                29
                            } else {
                                28
                            }
                        }
                    }
                )
            }
        } else {
            this.set(Calendar.DATE, this.get(Calendar.DATE) - 1)
        }
        return this
    }

    /**
     * 生成Json字符串。
     *
     * @return 生成的字符串。
     */
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        val converter = TimetableTypeConverter()
        jsonObject.put("name", name)
        jsonObject.put("classPerDay", classesPerDay)
        jsonObject.put("maxWeeks", maxWeeks)
        jsonObject.put("timeTable", converter.setTimeTable(timeTable))
        jsonObject.put("startDate", converter.setStartDate(startDate))
        jsonObject.put("weekStart", weekStart)
        return jsonObject
    }
}