package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.content.Context
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程删除器。将课程删除操作这一同时涉及数据库操作和日历操作的逻辑封装到一个对象中，
 * 并使用Hilt来管理依赖关系。
 *
 * @param eventsOperator 事件操作器。
 * @author UnscientificJsZhai
 * @since 1.3.0
 */
@Singleton
class CourseDeleter @Inject constructor(private val eventsOperator: EventsOperator) {

    /**
     * MainActivity和CourseDetailActivity操作删除课程。
     *
     * @param context 上下文，用于获取Application的引用和打开数据库等。
     * @param courseWithClassTimes 要删除的课程对象。
     * @param useCalendar 是否使用日历。
     */
    suspend fun deleteCourse(
        context: Context,
        courseWithClassTimes: CourseWithClassTimes,
        useCalendar: Boolean
    ) {
        val application = (context.applicationContext) as SchedulerApplication
        withContext(Dispatchers.IO) {
            // 从日历中删除。
            if (useCalendar) {
                val courseTable = application.courseTable!!
                eventsOperator.deleteEvent(
                    context,
                    courseTable,
                    courseWithClassTimes
                )
            }

            // 从数据库中删除。
            val database = application.getCourseDatabase()
            val courseDao = database.courseDao()
            val classTimeDao = database.classTimeDao()
            courseDao.deleteCourse(courseWithClassTimes.course)
            classTimeDao.deleteClassTimes(courseWithClassTimes.classTimes)
        }
    }
}