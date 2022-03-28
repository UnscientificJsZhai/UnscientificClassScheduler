package com.github.unscientificjszhai.unscientficclassscheduler.ui.main

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.fragments.CourseListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MainActivity的ViewModel。也用于[CourseListFragment]。
 *
 * 存放了主页状态数据和数据库中的数据。
 *
 * @param courseList 从RoomDatabase中获取的全部课程的LiveData对象。
 * @see MainActivity
 * @author UnscientificJsZhai
 */
internal class MainActivityViewModel(var courseList: LiveData<List<CourseWithClassTimes>>) :
    ViewModel() {

    companion object {

        /**
         * MainActivity和CourseDetailActivity操作删除课程。
         *
         * @param context MainActivity的实例，用于获取Application的引用和打开数据库等。
         * @param courseWithClassTimes 要删除的课程对象。
         */
        suspend fun deleteCourse(
            context: Activity,
            courseWithClassTimes: CourseWithClassTimes,
            useCalendar: Boolean
        ) {
            val application = (context.application) as SchedulerApplication
            withContext(Dispatchers.IO) {
                // 从日历中删除。
                if (useCalendar) {
                    val courseTable = application.courseTable!!
                    EventsOperator.deleteEvent(
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
                    EventsOperator.addEvent(context, courseTable, courseWithClassTimes)
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
     * 创建MainActivity的ViewModel的Factory。
     *
     * @param dao 一个Dao对象，用于初始化ViewModel时传入LiveData的参数
     */
    class Factory(private val dao: CourseDao, private val application: SchedulerApplication) :
        ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(dao.getLiveCourses(application.nowTableID)) as T
        }
    }

    /**
     * 主界面是否只显示今天的课程。
     */
    var showTodayOnly = false
}