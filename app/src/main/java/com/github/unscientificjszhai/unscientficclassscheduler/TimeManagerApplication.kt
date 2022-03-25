package com.github.unscientificjszhai.unscientficclassscheduler

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.database.CourseDatabase
import com.github.unscientificjszhai.unscientficclassscheduler.data.database.CourseTableDatabase
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.settings.SettingsActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.settings.SettingsFragment
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

/**
 * 全局单例Application对象。提供当前课程表的属性委托功能。
 *
 * @see CourseTable
 * @author UnscientificJsZhai
 */
class TimeManagerApplication : Application(), CourseTable.Getter {

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
         * 课程表数据库版本号。
         * 从1到2，添加了CourseTable的每周开始日列。
         */
        const val TABLE_DATABASE_VERSION: Int = 2

        /**
         * 课程数据库版本号。
         */
        const val COURSE_DATABASE_VERSION: Int = 1

        /**
         * 默认的[CourseTable]id，表示新创建的对象的id时使用（本来为null）。
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

    /**
     * CourseTable数据库对象。
     */
    private var courseTableDatabase: CourseTableDatabase? = null

    /**
     * Course数据库对象。
     */
    private var courseDatabase: CourseDatabase? = null

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = getSharedPreferences(INITIAL, Context.MODE_PRIVATE)
        this.nowTableID =
            sharedPreferences.getLong(NOW_TABLE_SP_KEY, DEFAULT_DATABASE_OBJECT_ID)
        // 开启子线程加载数据库对象
        thread(start = true) {
            getCourseTableDatabase()
            if (nowTableID != DEFAULT_DATABASE_OBJECT_ID) {
                this.courseTable =
                    getCourseTableDatabase().courseTableDao().getCourseTable(this.nowTableID)
            }
        }
        thread(start = true) {
            // 如果是第一次启动应用时，则不会在Application创建的过程中启动数据库对象
            if (nowTableID != DEFAULT_DATABASE_OBJECT_ID) {
                getCourseDatabase()
            }
        }
    }

    /**
     * 获取Course的RoomDatabase对象。全局单例。也可以调用此方法来使目标数据库初始化。
     *
     * @return Course的RoomDatabase对象，可以调用它的Dao方法进行数据操作。
     */
    fun getCourseDatabase(): CourseDatabase {
        if (this.courseDatabase == null) {
            // 初始化CourseDatabase
            this.courseDatabase = Room.databaseBuilder(
                this,
                CourseDatabase::class.java,
                "table$nowTableID.db"
            ).build()
        }
        return this.courseDatabase!!
    }

    /**
     * 获取CourseTable的RoomDatabase对象。全局单例。也可以调用此方法来使目标数据库初始化。
     *
     * @return CourseTable的RoomDatabase对象，可以调用它的Dao方法来进行数据操作。
     */
    fun getCourseTableDatabase(): CourseTableDatabase {
        // 初始化CourseTableDatabase
        if (this.courseTableDatabase == null) {
            this.courseTableDatabase =
                Room.databaseBuilder(
                    this,
                    CourseTableDatabase::class.java,
                    "database.db"
                ).addMigrations(CourseTableDatabase.MIGRATION_1_2).build()
        }
        return this.courseTableDatabase!!
    }

    /**
     * 更新当前课程表的ID数值。同时也会更新[courseTable]的值位对应的对象。
     * 因为创建新的CourseTable对象时id为null，所以需要将结果保存到数据库再从数据库中读取。
     *
     * @param newID 更改后的ID。可以从[CourseTable]对象中获取，也可以从[CourseTableDao.insertCourseTable]的返回值中获取。
     * @see MainActivity.DatabaseChangeReceiver
     * @see SettingsActivity.DatabaseChangeReceiver
     */
    fun updateTableID(newID: Long) {
        // 更新SharedPreference中的表数据
        val editor = getSharedPreferences(INITIAL, Context.MODE_PRIVATE).edit()
        editor.putLong(NOW_TABLE_SP_KEY, newID)
        editor.apply()

        val oldID = this.nowTableID
        this.nowTableID = newID
        thread(start = true) {
            this.courseTable = getCourseTableDatabase().courseTableDao().getCourseTable(newID)

            // 更新成员变量中的CourseDatabase对象的引用，实例化新的CourseDatabase对象
            if (oldID != this.nowTableID) {
                this.courseDatabase = null
                this.getCourseDatabase()
            }

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