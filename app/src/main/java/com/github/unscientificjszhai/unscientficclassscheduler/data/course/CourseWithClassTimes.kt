package com.github.unscientificjszhai.unscientficclassscheduler.data.course

import androidx.room.Embedded
import androidx.room.Relation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * [Course]和[ClassTime]之间的一对多关系。
 *
 * @param course Course对象。
 * @param classTimes Course对象关联的ClassTime的列表。
 * @author UnscientificJsZhai
 */
data class CourseWithClassTimes(
    @Embedded val course: Course,
    @Relation(parentColumn = "id", entityColumn = "course_id")
    var classTimes: List<ClassTime>
) : Serializable {

    constructor(template: CourseWithClassTimes) : this(template.course.copy(), template.classTimes)

    companion object {

        /**
         * 从Json中解析出一个课程组合对象。
         *
         * @param jsonString JSON字符串。
         * @return 生成的CourseWithClassTimes对象。
         * @exception JSONException 当Json解析出错时抛出此错误。
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun parseJson(jsonString: String): CourseWithClassTimes {
            val jsonObject = JSONObject(jsonString)
            // 暂时使用0替代表ID。
            val course = Course.parseJson(jsonObject.getString("course"), 0)
            val classTimes = ArrayList<ClassTime>()
            val jsonArrayOfClassTime = JSONArray(jsonObject.getString("classTimes"))
            for (index in 0 until jsonArrayOfClassTime.length()) {
                val classTimeJsonString = jsonArrayOfClassTime.getString(index)
                classTimes.add(ClassTime.parseJson(classTimeJsonString))
            }
            return CourseWithClassTimes(course, classTimes)
        }
    }
}