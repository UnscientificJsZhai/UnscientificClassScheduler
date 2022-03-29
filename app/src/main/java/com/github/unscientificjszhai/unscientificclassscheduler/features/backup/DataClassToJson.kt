@file:JvmName("DataClassToJson")

package com.github.unscientificjszhai.unscientificclassscheduler.features.backup

import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import org.json.JSONArray
import org.json.JSONObject

/**
 * 生成上课时间的Json字符串。
 *
 * @receiver 上课时间数据类对象。
 * @return 生成的字符串。
 * @author UnscientificJsZhai
 */
private fun ClassTime.toJson(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("week", week)
    jsonObject.put("whichDay", whichDay)
    jsonObject.put("start", start)
    jsonObject.put("end", end)
    jsonObject.put("teacherName", teacherName)
    jsonObject.put("location", location)
    return jsonObject
}

/**
 * 生成课程的Json字符串。
 *
 * @receiver 课程的数据类对象。
 * @return 生成的字符串。
 * @author UnscientificJsZhai
 */
private fun Course.toJson(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("title", title)
    jsonObject.put("credit", credit)
    jsonObject.put("remarks", remarks)
    return jsonObject
}

/**
 * 生成课程和上课时间的Json字符串。
 *
 * @receiver 课程和上课时间的Json字符串。
 * @return 生成的字符串。
 * @author UnscientificJsZhai
 */
internal fun CourseWithClassTimes.toJson(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("course", course.toJson())
    val jsonArray = JSONArray()
    classTimes.forEach {
        jsonArray.put(it.toJson())
    }
    jsonObject.put("classTimes", jsonArray)
    return jsonObject
}