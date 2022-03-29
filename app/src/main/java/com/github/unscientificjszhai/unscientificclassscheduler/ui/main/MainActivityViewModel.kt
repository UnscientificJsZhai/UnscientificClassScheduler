package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.fragments.CourseListFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * MainActivity的ViewModel。也用于[CourseListFragment]。
 *
 * 存放了主页状态数据和数据库中的数据。
 *
 * @param dao 课程访问用Dao。
 * @param application 应用级Context。用于获取当前选中课程表，不保存引用。
 * @param eventsOperator 日历事件操作器。
 * @see MainActivity
 * @author UnscientificJsZhai
 */
@HiltViewModel
internal class MainActivityViewModel @Inject constructor(
    dao: CourseDao,
    application: SchedulerApplication,
    private val eventsOperator: EventsOperator,
    private val deleteOperator: CourseDeleter
) : ViewModel() {

    var courseList: LiveData<List<CourseWithClassTimes>> =
        dao.getLiveCourses(application.nowTableID)

    /**
     * MainActivity和CourseDetailActivity操作删除课程。
     *
     * @param context MainActivity的实例，用于获取Application的引用和打开数据库等。
     * @param courseWithClassTimes 要删除的课程对象。
     * @param useCalendar 是否使用日历。
     */
    suspend fun deleteCourse(
        context: Activity,
        courseWithClassTimes: CourseWithClassTimes,
        useCalendar: Boolean
    ) {
        deleteOperator.deleteCourse(context, courseWithClassTimes, useCalendar)
    }

    /**
     * 撤销删除操作。
     *
     * @param context MainActivity的实例，用于获取Application的引用和打开数据库等。
     * @param courseWithClassTimes 要删除的课程对象。
     */
    suspend fun undoDeleteCourse(
        context: Activity,
        courseWithClassTimes: CourseWithClassTimes,
        useCalendar: Boolean
    ) {
        val application = (context.application) as SchedulerApplication
        val courseTable by application
        withContext(Dispatchers.IO) {
            if (useCalendar) {
                // 清理之前的关联日历项，并重新添加回日历
                courseWithClassTimes.course.associatedEventsId.clear()
                eventsOperator.addEvent(context, courseTable, courseWithClassTimes)
            }
            // 重新添加回数据库
            val database = application.getCourseDatabase()
            val courseDao = database.courseDao()
            val classTimeDao = database.classTimeDao()
            courseDao.insertCourse(courseWithClassTimes.course)
            courseWithClassTimes.classTimes.forEach {
                classTimeDao.insertClassTime(it)
            }
        }
    }

    /**
     * 课程列表是否为空。
     *
     * @return 如果为空则返回true。
     */
    fun isListEmpty(): Boolean {
        val courseList = this.courseList.value
        return courseList?.isEmpty() ?: true
    }

    /**
     * 主界面是否只显示今天的课程。
     */
    var showTodayOnly = false
}