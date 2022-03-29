package com.github.unscientificjszhai.unscientficclassscheduler.features.parse

import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificcourseparser.core.export.CoursesJson
import com.github.unscientificjszhai.unscientificcourseparser.core.data.ClassTime as SourceClassTime
import com.github.unscientificjszhai.unscientificcourseparser.core.data.Course as SourceCourse

/**
 * 从课程表导入的课程，使用此类进行数据转换。
 *
 * @param coursesSource 源。
 * @author UnscientificJsZhai
 */
class ParserTypeConverter(private val coursesSource: List<SourceCourse>) :
    List<SourceCourse> by coursesSource {

    companion object {

        /**
         * 从Json字符串读取并转换成课程信息。
         *
         * @param jsonString Json字符串。
         * @return 生成的课程信息转换对象。
         */
        @JvmStatic
        fun fromJson(jsonString: String): ParserTypeConverter {
            return ParserTypeConverter(CoursesJson.jsonToCourse(jsonString))
        }
    }

    /**
     * 生成转换后的对象。
     *
     * @return 转换后的对象列表。合法性未检验。
     */
    fun generateConvertedCourse(): List<CourseWithClassTimes> {
        val courseWithClassTimes = ArrayList<CourseWithClassTimes>()
        for (sourceCourse in this.coursesSource) {
            val course = Course(0)
            course.title = sourceCourse.title
            course.remarks = sourceCourse.remark
            course.credit = sourceCourse.credit
            val classTimes = ArrayList<ClassTime>()

            for (sourceClassTime in sourceCourse.classTimes) {
                val classTime = ClassTime()
                classTime.start = sourceClassTime.from
                classTime.end = sourceClassTime.to
                classTime.location = sourceClassTime.location
                classTime.teacherName = sourceClassTime.teacher
                classTime.whichDay = sourceClassTime.day
                for (week in sourceClassTime.startWeek..sourceClassTime.endWeek) {
                    if (week % 2 == 0 && sourceClassTime.scheduleMode != SourceClassTime.SCHEDULE_MODE_ODD) {
                        classTime.setWeekData(week, true)
                    } else if (week % 2 == 1 && sourceClassTime.scheduleMode != SourceClassTime.SCHEDULE_MODE_EVEN) {
                        classTime.setWeekData(week, true)
                    }
                }
                classTimes.add(classTime)
            }
            courseWithClassTimes.add(CourseWithClassTimes(course, classTimes))
        }

        return courseWithClassTimes
    }
}