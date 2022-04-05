package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.table

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.TimetableTypeConverter
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.reflect.KProperty

/**
 * 用于给[TimeTableEditorActivity]保存每节课间隔的ViewModel。
 *
 * @see TimeTableEditorActivity
 * @author UnscientificJsZhai
 */
@HiltViewModel
class TimeTableEditorActivityViewModel @Inject constructor(
    private val eventsOperator: EventsOperator
) : ViewModel() {

    var duration = 0

    lateinit var courseTable: CourseTable

    /**
     * 用于校验前后是否修改过表。
     */
    var originTimeTable = ""

    private val typeConverter = TimetableTypeConverter()

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this.typeConverter

    /**
     * 保存更改后的上课时间数据。
     *
     * @param context 执行此操作的Activity上下文。
     */
    suspend fun save(context: Activity, useCalendar: Boolean) {
        val timeManagerApplication = context.application as SchedulerApplication

        withContext(Dispatchers.Default) {
            timeManagerApplication.getCourseDatabase().courseTableDao()
                .updateCourseTable(this@TimeTableEditorActivityViewModel.courseTable)
            timeManagerApplication.updateTableID(this@TimeTableEditorActivityViewModel.courseTable.id!!)

            if (useCalendar) {
                eventsOperator.updateAllEvents(
                    context,
                    this@TimeTableEditorActivityViewModel.courseTable
                )
            }
        }
    }
}