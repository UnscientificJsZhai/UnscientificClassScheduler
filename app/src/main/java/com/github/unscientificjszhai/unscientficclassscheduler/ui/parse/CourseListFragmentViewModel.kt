package com.github.unscientificjszhai.unscientficclassscheduler.ui.parse

import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用于CourseListFragment的ViewModel。
 *
 * @see CourseListFragment
 * @author UnscientificJsZhai
 */
internal class CourseListFragmentViewModel : ViewModel() {

    lateinit var courseList: List<CourseWithClassTimes>

    /**
     * 将从教务系统解析到的数据导入当前课程表。
     *
     * @param application 用于导入和获取数据库对象的上下文。
     * @return 未能成功添加的课程列表。或者null如果本身就没有解析到的数据。
     */
    suspend fun save(application: TimeManagerApplication): List<String>? {
        if (this.courseList.isEmpty()) {
            return null
        }
        val exceptionList = ArrayList<String>()
        withContext(Dispatchers.IO) {
            //导入到当前的课程表中
            val courseDatabase = application.getCourseDatabase()
            val courseTable by application
            val courseDao = courseDatabase.courseDao()
            val classTimesDao = courseDatabase.classTimeDao()

            for (courseWithClassTime in this@CourseListFragmentViewModel.courseList) {
                if (!Course.checkLegitimacy(courseWithClassTime, courseTable)) {
                    exceptionList.add(courseWithClassTime.course.title)
                    continue
                }
                val course = courseWithClassTime.course
                EventsOperator.addEvent(application, courseTable, courseWithClassTime)
                val courseId = courseDao.insertCourse(course)
                for (classTime in courseWithClassTime.classTimes) {
                    classTime.courseId = courseId
                    classTimesDao.insertClassTime(classTime)
                }
            }
        }

        return exceptionList
    }
}