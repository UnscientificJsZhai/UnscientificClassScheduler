package com.github.unscientificjszhai.unscientificclassscheduler

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.fragments.CourseListFragment
import com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.SettingsActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.SettingsFragment
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

/**
 * 全局单例Application对象。提供当前课程表的属性委托功能。
 *
 * @see CourseTable
 * @author UnscientificJsZhai
 */
@HiltAndroidApp
class SchedulerApplication : Application(), CourseTable.Provider {

    companion object {

        /**
         * 用在SharedPreferences中的Name。
         */
        const val INITIAL = "init"

        /**
         * 在[INITIAL]作为Name的SharedPreferences中查找目前表的编号使用的Key。
         */
        const val NOW_TABLE_SP_KEY = "now"

        /**
         * 课程数据库版本号。
         */
        const val COURSE_DATABASE_VERSION: Int = 1

        /**
         * 默认的[CourseTable]的id，表示新创建的对象的id时使用（本来为null）。
         */
        const val DEFAULT_DATABASE_OBJECT_ID: Long = -1
    }

    /**
     * 记录当前所在课程表位置的数值。在Application初始化的过程中就从SharedPreference中读取了它的数值。
     */
    var nowTableID: Long = DEFAULT_DATABASE_OBJECT_ID
        private set

    /**
     * 当前所在课程表的对象。除非是第一次打开应用时，否则不会为null。
     */
    var courseTable: CourseTable? = null
        private set

    private lateinit var tableDao: CourseTableDao
    private lateinit var courseDao: CourseDao

    @Inject
    fun injectDao(
        tableDao: CourseTableDao,
        courseDao: CourseDao
    ) {
        this.tableDao = tableDao
        this.courseDao = courseDao
    }

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences(INITIAL, Context.MODE_PRIVATE)
        this.nowTableID =
            sharedPreferences.getLong(NOW_TABLE_SP_KEY, DEFAULT_DATABASE_OBJECT_ID)
        thread {
            this.courseTable = this.tableDao.getCourseTable(this.nowTableID)
        }
    }

    /**
     * 更新当前课程表的ID数值。同时也会更新[courseTable]的值位对应的对象。
     * 因为创建新的CourseTable对象时id为null，所以需要将结果保存到数据库再从数据库中读取。
     *
     * @param newID 更改后的ID。可以从[CourseTable]对象中获取，也可以从[CourseTableDao.insertCourseTable]的返回值中获取。
     * @see CourseListFragment.DatabaseChangeReceiver
     * @see SettingsActivity.DatabaseChangeReceiver
     */
    fun updateTableID(newID: Long) {
        // 更新SharedPreference中的表数据
        val editor = getSharedPreferences(INITIAL, Context.MODE_PRIVATE).edit()
        editor.putLong(NOW_TABLE_SP_KEY, newID)
        editor.apply()

        this.nowTableID = newID
        thread(start = true) {
            this.courseTable = this.tableDao.getCourseTable(newID)

            // 发送广播
            val broadcastIntent = Intent(MainActivity.COURSE_DATABASE_CHANGE_ACTION)
            broadcastIntent.setPackage(packageName)
            sendBroadcast(broadcastIntent)
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): CourseTable {
        return this.courseTable!!
    }

    /**
     * 检查设置，返回当前应用是否在使用日历功能。参见[SettingsFragment.USE_CALENDAR_KEY]这一SwitchPreference。
     */
    var useCalendar: Boolean
        get() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            return sharedPreferences.getBoolean(SettingsFragment.USE_CALENDAR_KEY, true)
        }
        set(value) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            sharedPreferences.edit().putBoolean(SettingsFragment.USE_CALENDAR_KEY, value).apply()
        }
}