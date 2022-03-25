package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.CalendarOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KProperty

/**
 * [SettingsActivity]的ViewModel。
 *
 * @see SettingsActivity
 * @author UnscientificJsZhai
 */
internal class SettingsActivityViewModel(private val dataStore: SettingsDataStore) : ViewModel(),
    SettingsDataStore.Getter {

    class Factory(private val dataStore: SettingsDataStore) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsActivityViewModel(dataStore) as T
        }
    }

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
            CalendarOperator.deleteCalendarTable(context, courseTable)
            CalendarOperator.createCalendarTable(context, courseTable)
            val application =
                context.applicationContext as TimeManagerApplication
            val tableDao =
                application.getCourseTableDatabase().courseTableDao()
            tableDao.updateCourseTable(courseTable)
            val courseDao = application.getCourseDatabase().courseDao()
            courseDao.getCourses().run {
                for (courseWithClassTimes in this) {
                    EventsOperator.addEvent(
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
            CalendarOperator.updateCalendarColor(context, color)
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
            CalendarOperator.deleteAllTables(context)
        }
    }
}