package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.content.Context
import androidx.preference.PreferenceDataStore
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

/**
 * 用于定义Preferences数据存储的自定义DataStore。
 *
 * @param nowCourseTable 当前正在修改的CourseTable对象，当当前的课程表更改的时候，[SettingsActivity]会更新这里的引用。
 * @param courseTableDao 操作数据库的Dao接口对象。
 * @param context 上下文，用于更新日历。
 * @author UnscientificJsZhai
 */
class SettingsDataStore(
    var nowCourseTable: CourseTable,
    private val courseTableDao: CourseTableDao,
    private val context: Context,
    private val eventsOperator: EventsOperator,
    private val notifyApplicationCourseTableChanged: ((Long) -> Unit)?
) : PreferenceDataStore() {

    /**
     * 为[SettingsDataStore]提供属性委托功能。
     */
    interface Getter {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): SettingsDataStore
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            //学期上课周数
            SettingsFragment.MAX_WEEK_KEY -> {
                if (value?.isEmpty() == false) {
                    val newValue = try {
                        value.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }
                    if (newValue in 1..ClassTime.MAX_STORAGE_SIZE) {
                        this.nowCourseTable.maxWeeks = newValue
                    }
                }
            }

            SettingsFragment.CLASSES_PER_DAY_KEY -> {
                if (value?.isEmpty() == false) {
                    val newValue: Int = try {
                        value.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }
                    if (newValue in 1..CourseTable.MAX_CLASS_PER_DAY) {
                        this.nowCourseTable.classesPerDay = newValue
                    }
                }
            }

            SettingsFragment.WEEK_START_KEY -> {
                when (value) {
                    "0" -> {
                        this.nowCourseTable.weekStart = false
                    }
                    "1" -> {
                        this.nowCourseTable.weekStart = true
                    }
                    else -> {
                        return super.putString(key, value)
                    }
                }
            }

            else -> {
                return super.putString(key, value)
            }
        }
        updateCourseTable()
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            SettingsFragment.MAX_WEEK_KEY -> {
                this.nowCourseTable.maxWeeks.toString()
            }

            SettingsFragment.CLASSES_PER_DAY_KEY -> {
                this.nowCourseTable.classesPerDay.toString()
            }

            SettingsFragment.WEEK_START_KEY -> {
                if (this.nowCourseTable.weekStart) {
                    "1"
                } else {
                    "0"
                }
            }

            else -> {
                null
            }
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == SettingsFragment.WEEK_START_KEY) {
            return if (this.nowCourseTable.weekStart) {
                1
            } else {
                0
            }
        }
        return super.getInt(key, defValue)
    }

    /**
     * 把暂存在DataStore中的CourseTable对象更新到数据库，也同时更新Application中的相应的对象。
     */
    internal fun updateCourseTable() {
        thread(start = true) {
            courseTableDao.updateCourseTable(this.nowCourseTable)
            notifyApplicationCourseTableChanged?.invoke(nowCourseTable.id!!)

            //向日历插入数据
            eventsOperator.updateAllEvents(context, this.nowCourseTable)
        }
    }
}