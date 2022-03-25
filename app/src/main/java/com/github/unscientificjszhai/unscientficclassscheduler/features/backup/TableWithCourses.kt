package com.github.unscientificjszhai.unscientficclassscheduler.features.backup

import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * 用于备份和恢复功能的封装类。在导入导出备份和生成ICS中使用。
 *
 * @see CourseTable
 * @see CourseWithClassTimes
 * @author UnscientificJsZhai
 */
class TableWithCourses(val courseTable: CourseTable, val courses: List<CourseWithClassTimes>) :
    Serializable {

    companion object {

        /**
         * 从Json中解析出一个组合表对象。
         *
         * @return 生成的TableWithCourse对象。
         * @exception JSONException 当Json解析出错时抛出此错误。
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun parseJson(jsonString: String): TableWithCourses {
            val jsonObject = JSONObject(jsonString)
            val courseTable = CourseTable.parseJson(jsonObject.getString("courseTable"))
            val courses = ArrayList<CourseWithClassTimes>()
            val jsonArrayOfCourses = JSONArray(jsonObject.getString("courses"))
            for (index in 0 until jsonArrayOfCourses.length()) {
                val courseJsonString = jsonArrayOfCourses.getString(index)
                courses.add(CourseWithClassTimes.parseJson(courseJsonString))
            }
            return TableWithCourses(courseTable, courses)
        }
    }

    /**
     * 生成Json字符串。
     *
     * @return 生成的字符串。
     */
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("courseTable", courseTable.toJson())
        val jsonArray = JSONArray()
        courses.forEach {
            jsonArray.put(it.toJson())
        }
        jsonObject.put("courses", jsonArray)
        return jsonObject
    }
}