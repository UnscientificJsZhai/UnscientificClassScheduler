package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.selector

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.CalendarOperator
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
class CurrentTableSelectorActivityViewModel @Inject constructor(
    private val dao: CourseTableDao,
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

        withContext(Dispatchers.IO) {
            calendarOperator.updateCalendarTable(context, courseTable)

            dao.updateCourseTable(courseTable)
        }
    }

    suspend fun deleteCourseTable(context: Activity, courseTable: CourseTable) {

        withContext(Dispatchers.IO) {
            calendarOperator.deleteCalendarTable(context, courseTable)

            dao.deleteCourseTable(courseTable)
            context.deleteDatabase("table${courseTable.id}.db")
        }
    }
}