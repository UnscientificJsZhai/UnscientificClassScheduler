package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.CalendarOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * CurrentTableSelectorActivity的ViewModel
 *
 * @see CurrentTableSelectorActivity
 * @author UnscientificJsZhai
 */
@HiltViewModel
internal class CurrentTableSelectorActivityViewModel @Inject constructor(
    dao: CourseTableDao,
    private val calendarOperator: CalendarOperator
) : ViewModel() {

    val tableList: LiveData<List<CourseTable>> = dao.getLiveCourseTables()

    /**
     * 向数据库中添加课程表。
     *
     * @param context 执行添加操作的上下文。
     * @param courseTable 要添加的课程表对象。
     */
    suspend fun addCourseTable(context: Activity, courseTable: CourseTable) {
        val timeManagerApplication = context.application as SchedulerApplication

        withContext(Dispatchers.IO) {
            calendarOperator.createCalendarTable(context, courseTable)

            val dao =
                timeManagerApplication.getCourseDatabase().courseTableDao()
            val id = dao.insertCourseTable(courseTable)
            timeManagerApplication.updateTableID(id)
        }
    }

    /**
     * 重命名课程表。调用这个函数之前需要把要重命名的课程表对象的新名称赋值给[CourseTable.name]。
     *
     * @param context 执行添加操作的上下文。
     * @param courseTable 要重命名的课程表对象。
     */
    suspend fun renameCourseTable(context: Activity, courseTable: CourseTable) {
        val timeManagerApplication = context.application as SchedulerApplication

        withContext(Dispatchers.IO) {
            calendarOperator.updateCalendarTable(context, courseTable)

            val dao =
                timeManagerApplication.getCourseDatabase()
                    .courseTableDao()
            dao.updateCourseTable(courseTable)
        }
    }

    suspend fun deleteCourseTable(context: Activity, courseTable: CourseTable) {
        val timeManagerApplication = context.application as SchedulerApplication

        withContext(Dispatchers.IO) {
            calendarOperator.deleteCalendarTable(context, courseTable)

            val dao =
                timeManagerApplication.getCourseDatabase()
                    .courseTableDao()
            dao.deleteCourseTable(courseTable)
            context.deleteDatabase("table${courseTable.id}.db")
        }
    }
}