package com.github.unscientificjszhai.unscientficclassscheduler.data.course

import androidx.room.*
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.FormattedTime
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * 课程时间。
 *
 * @param id 主键。
 * @param courseId 外键。
 * @param week 上课周数，位图存储，需要调用[getWeekData]和[setWeekData]方法来查改。
 * @param whichDay 周几。0代表周日，6代表周六。
 * @param start 从第几节课开始上。
 * @param end 上到第几节课。
 * @param teacherName 教师姓名。
 * @param location 上课地点。
 * @author UnscientificJsZhai
 */
@Entity(
    tableName = ClassTime.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["id"],
        childColumns = ["course_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["course_id"])]
)
data class ClassTime(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "course_id") var courseId: Long?,
    var week: Int,
    @ColumnInfo(name = "which_day") var whichDay: Int,
    var start: Int,
    var end: Int,
    @ColumnInfo(name = "teacher_name") var teacherName: String,
    var location: String
) : Serializable {

    companion object {
        /**
         * 表的名字。
         */
        const val TABLE_NAME = "time"

        /**
         * 周数的最大存储位数。
         */
        const val MAX_STORAGE_SIZE = 30

        /**
         * 从Json中解析出一个上课时间对象。
         *
         * @return 生成的ClassTime对象。
         * @exception JSONException 当Json解析出错时抛出此错误。
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun parseJson(jsonString: String): ClassTime {
            val jsonObject = JSONObject(jsonString)
            val classTime = ClassTime()
            classTime.week = jsonObject.getInt("week")
            classTime.whichDay = jsonObject.getInt("whichDay")
            classTime.start = jsonObject.getInt("start")
            classTime.end = jsonObject.getInt("end")
            classTime.teacherName = jsonObject.getString("teacherName")
            classTime.location = jsonObject.getString("location")
            return classTime
        }
    }

    /**
     * 创建一个新的对象的方法。
     */
    constructor() : this(
        null,
        null,
        0,
        0,
        1,
        2,
        "",
        ""
    )

    /**
     * 从上一个对象处拷贝数据到新对象。不包括主键和外键。
     *
     * @param template 上一个对象。
     */
    constructor(template: ClassTime) : this(
        null,
        null,
        template.week,
        template.whichDay,
        template.start,
        template.end,
        template.teacherName,
        template.location
    )

    /**
     * 指定Course的ID时创建新对象的方法
     *
     * @param courseId 目标Course对象的Id。
     */
    constructor(courseId: Long) : this(
        null,
        courseId,
        0,
        0,
        1,
        2,
        "",
        ""
    )

    /**
     * 检查数据的合法性。新创建出来的对象的数据一定不合法，所以经过修改后的数据需要检查
     * 以确定合法性。
     *
     * @return 是否合法。是则返回true，否则false
     */
    fun isLegitimacy(courseTable: CourseTable): Boolean {
        if (this.whichDay > 6) return false
        if (this.whichDay < 0) return false
        if (this.start < 1) return false
        if (this.end > courseTable.classesPerDay) return false
        return if (end < start) false else this.week != 0
    }

    /**
     * 给周数设置数据的方法。
     *
     * @param weekNumber 第几周。
     * @param set        是否选中。
     * @throws IndexOutOfBoundsException 如果超过最大范围就会抛出此错误。
     */
    fun setWeekData(weekNumber: Int, set: Boolean) {
        if (weekNumber < 1 || weekNumber > MAX_STORAGE_SIZE) {
            throw IndexOutOfBoundsException()
        }

        val weekNumberToBit = 1 shl (weekNumber - 1)
        if (set) {
            this.week = this.week or weekNumberToBit
        } else {
            this.week = this.week and (weekNumberToBit.inv())
        }
    }

    /**
     * 获取数据的方法。
     *
     * @param weekNumber 第几周。
     * @return 是否选中。
     */
    fun getWeekData(weekNumber: Int): Boolean {
        if (weekNumber < 1 || weekNumber > MAX_STORAGE_SIZE) {
            throw IndexOutOfBoundsException()
        }

        val weekNumberToBit = 1 shl (weekNumber - 1)
        return (this.week and weekNumberToBit) != 0
    }

    /**
     * 获得表示这节课这个上课时段的FormattedTime对象。
     *
     * @param courseTable 课程表对象，用于参照。
     * @return 合成的[FormattedTime]对象。
     */
    fun getFormattedTime(courseTable: CourseTable) = FormattedTime(
        FormattedTime(courseTable.timeTable[this.start - 1]),
        FormattedTime(courseTable.timeTable[this.end - 1])
    )
}