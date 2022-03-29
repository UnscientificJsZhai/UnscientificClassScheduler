package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.features.backup.BackupOperator
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.CalendarOperator
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.reflect.KProperty

/**
 * [SettingsActivity]的ViewModel。
 *
 * @see SettingsActivity
 * @author UnscientificJsZhai
 */
@HiltViewModel
internal class SettingsActivityViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    val backupOperator: BackupOperator,
    private val calendarOperator: CalendarOperator,
    private val eventsOperator: EventsOperator
) : ViewModel(), SettingsDataStore.Getter {

    override fun getValue(thisRef: Any?, property: KProperty<*>) = this.dataStore

    /**
     * 删除所有日历项目并重新创建。
     *
     * @param context 执行本操作的Activity上下文。
     */
    suspend fun updateCalendar(context: Activity) {
        // 删除全部日历并重新创建。
        withContext(Dispatchers.Default) {
            val courseTable = dataStore.nowCourseTable
            calendarOperator.deleteCalendarTable(context, courseTable)
            calendarOperator.createCalendarTable(context, courseTable)
            val application =
                context.applicationContext as SchedulerApplication

            val tableDao =
                application.getCourseDatabase().courseTableDao()
            tableDao.updateCourseTable(courseTable)
            val courseDao = application.getCourseDatabase().courseDao()
            courseDao.getCourses(application.nowTableID).run {
                for (courseWithClassTimes in this) {
                    eventsOperator.addEvent(
                        context,
                        courseTable,
                        courseWithClassTimes
                    )
                    courseDao.updateCourse(courseWithClassTimes.course)
                }
            }
        }
    }

    suspend fun setCalendarColor(context: Activity, value: String) {
        val color = value.toInt()
        withContext(Dispatchers.Default) {
            calendarOperator.updateCalendarColor(context, color)
        }
    }

    /**
     * 清空日历项表。
     *
     * 会删除所有本应用创建的日历表。
     *
     * @param context 执行本操作的Activity上下文。
     */
    suspend fun clearCalendar(context: Activity) {
        withContext(Dispatchers.Default) {
            calendarOperator.deleteAllTables(context)
        }
    }
}