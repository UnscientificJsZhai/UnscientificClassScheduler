package com.github.unscientificjszhai.unscientficclassscheduler.features.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.ui.settings.SettingsFragment
import java.util.*

/**
 * 日历操作工具对象。所有对日历的操作（表的层面上）都在这里完成。日历将被写入系统日历提供程序中，并和此应用的账户关联。
 * 这样可以使本应用被卸载时，系统自动删除本应用关联的所有日历。
 *
 * @see EmptyAuthenticator
 * @see EventsOperator
 * @author UnscientificJsZhai
 */
object CalendarOperator {

    /**
     * 为目标课程表创建一个日历表。同时会给CourseTable的成员变量赋值，但不会保存到数据库。
     * 应该异步调用此方法。
     * 调用时需要注意，此方法还会修改CourseTable中的数据且未保存。需要手动保存。
     *
     * @param context 插入操作的上下文。
     * @param courseTable 要创建日历表的CourseTable
     * @return 插入后的ID,插入失败则返回空。
     */
    @WorkerThread
    fun createCalendarTable(context: Context, courseTable: CourseTable): Long? {
        val timeZone = TimeZone.getDefault()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val color =
            sharedPreferences.getString(SettingsFragment.CALENDAR_COLOR_KEY, "-1409017")// 默认芒果色

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.NAME, courseTable.getCalendarTableName())
            put(CalendarContract.Calendars.ACCOUNT_NAME, EmptyAuthenticator.ACCOUNT_NAME)
            put(
                CalendarContract.Calendars.ACCOUNT_TYPE,
                EmptyAuthenticator.ACCOUNT_TYPE
            )
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, courseTable.name)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CALENDAR_COLOR, color)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.id)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, EmptyAuthenticator.ACCOUNT_NAME)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0)
        }

        val uri = context.contentResolver.insert(
            CalendarContract.Calendars.CONTENT_URI.asSyncAdapter(),
            values
        )

        val id = if (uri == null) {
            null
        } else {
            ContentUris.parseId(uri)
        }
        courseTable.calendarID = id// 更新CalendarID
        return id
    }

    /**
     * 更新日历表的名称。
     * 更改例如上课时间之类的直接影响事件的属性时，应该调用[EventsOperator.updateAllEvents]方法更新。
     *
     * @param context 插入操作的上下文。
     * @param courseTable 要创建日历表的CourseTable。
     * @param updateTimeZone 是否更新时区设置。
     * @return 更改是否成功。
     */
    @WorkerThread
    fun updateCalendarTable(
        context: Context,
        courseTable: CourseTable,
        updateTimeZone: Boolean = true
    ): Boolean {
        val calendarID = courseTable.calendarID
        return if (calendarID == null) {
            false
        } else {
            val values = ContentValues().apply {
                if (updateTimeZone) {
                    val timeZone = TimeZone.getDefault()
                    put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.id)
                }
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, courseTable.name)
            }
            val updateUri =
                ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID)
            context.contentResolver
                .update(updateUri.asSyncAdapter(), values, null, null) != -1
        }
    }

    /**
     * 删除一个日历表。
     *
     * @param context 操作的上下文。
     * @param courseTable 要删除的日历表。
     * @return 是否删除成功。
     */
    @WorkerThread
    fun deleteCalendarTable(context: Context, courseTable: CourseTable): Boolean {
        val calendarID = courseTable.calendarID
        return if (calendarID == null) {
            false
        } else {
            val deleteUri =
                ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID)
            context.contentResolver
                .delete(deleteUri.asSyncAdapter(), null, null) != -1
        }
    }

    /**
     * 删除全部日历表。
     * 一般用于清除全部数据后首次启动。
     *
     * @param context 操作的上下文。
     * @param exception 除外的日历表的日历ID。通过[CourseTable.calendarID]获得。不需要保留的情况下填入null。
     */
    @WorkerThread
    fun deleteAllTables(context: Context, exception: Long? = null) {
        val eventProjection = arrayOf(CalendarContract.Calendars._ID)

        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (" +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?) AND (" +
                "${CalendarContract.Calendars.OWNER_ACCOUNT} = ?))"
        val selectionArgs = arrayOf(
            EmptyAuthenticator.ACCOUNT_NAME,
            EmptyAuthenticator.ACCOUNT_TYPE,
            EmptyAuthenticator.ACCOUNT_NAME
        )
        val cursor =
            context.contentResolver.query(uri, eventProjection, selection, selectionArgs, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val calendarID = cursor.getLong(0)

                if (calendarID != exception) {
                    // 删除日历表
                    val deleteUri =
                        ContentUris.withAppendedId(
                            CalendarContract.Calendars.CONTENT_URI,
                            calendarID
                        )
                    context.contentResolver
                        .delete(deleteUri.asSyncAdapter(), null, null) != -1
                }
            }
        }
        cursor?.close()
    }

    /**
     * 用于更新日历表颜色的方法。
     *
     * @param context 更新操作的上下文。
     * @param color 要更新的颜色。
     */
    @WorkerThread
    fun updateCalendarColor(context: Context, @ColorInt color: Int) {
        val eventProjection = arrayOf(CalendarContract.Calendars._ID)

        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (" +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?) AND (" +
                "${CalendarContract.Calendars.OWNER_ACCOUNT} = ?))"
        val selectionArgs = arrayOf(
            EmptyAuthenticator.ACCOUNT_NAME,
            EmptyAuthenticator.ACCOUNT_TYPE,
            EmptyAuthenticator.ACCOUNT_NAME
        )
        val cursor =
            context.contentResolver.query(uri, eventProjection, selection, selectionArgs, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val calendarID = cursor.getLong(0)

                // 更改颜色
                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.CALENDAR_COLOR, color)
                }
                val updateUri =
                    ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID)
                context.contentResolver
                    .update(updateUri.asSyncAdapter(), values, null, null)
            }
        }
        cursor?.close()
    }

    /**
     * CourseTable的固定获取CalendarID的方法。
     *
     * @return 用于在创建日历表的过程的名称，对应字段为[CalendarContract.Calendars.NAME]。
     */
    private fun CourseTable.getCalendarTableName() = "TimeManager${this.id}"

    /**
     * 将Uri包转成以SyncAdapter的形式访问日历提供程序，以获取更多权限。
     *
     * @param account 访问时使用的账号。
     * @param accountType 访问时使用的账号类型。
     * @return 包装好的Uri对象。
     */
    private fun Uri.asSyncAdapter(account: String, accountType: String) = this.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
        .build()

    /**
     * 将Uri包转成以SyncAdapter的形式访问日历提供程序，以获取更多权限。使用应用唯一账户和账户类型包装。
     *
     * @return 包装好的Uri对象。
     */
    private fun Uri.asSyncAdapter() =
        this.asSyncAdapter(EmptyAuthenticator.ACCOUNT_NAME, EmptyAuthenticator.ACCOUNT_TYPE)
}