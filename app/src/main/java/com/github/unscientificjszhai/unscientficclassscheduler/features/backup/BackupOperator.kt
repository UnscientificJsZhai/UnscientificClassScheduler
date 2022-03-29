package com.github.unscientificjszhai.unscientficclassscheduler.features.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.ClassTimeDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.CalendarOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.ProgressDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * 导出和导入备份的操作类。
 * 使用序列化和反序列化实现功能。
 *
 * @author UnscientificJsZhai
 */
@Singleton
class BackupOperator @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val courseTableDao: CourseTableDao,
    private val courseDao: CourseDao,
    private val classTimeDao: ClassTimeDao,
    private val calendarOperator: CalendarOperator,
    private val eventsOperator: EventsOperator
) {

    companion object {

        /**
         * 日历Ics文件的MIME Type。
         */
        private const val MIME_TYPE = "application/octet-stream"
    }

    private val application: SchedulerApplication get() = this.applicationContext as SchedulerApplication

    /**
     * 导出备份的具体实现。在处理过程中，会在窗口上显示一个Dialog。
     * 这个方法应该在[Activity.onActivityResult]中被调用，
     * 或者在[AppCompatActivity.registerForActivityResult]中注册。
     *
     * 只会导出当前打开的课程表的备份，会从[context]参数中获取当前的课程表。
     *
     * @param context 进行备份操作的上下文，因为要显示Dialog，仅接受Activity。
     * @param uri 备份文件的uri，需要可以被写入。
     */
    @UiThread
    fun exportBackup(context: Activity, uri: Uri) {
        val courseTable by application
        val contentResolver = context.contentResolver

        val progressDialog = ProgressDialog(context)
        progressDialog.show()
        thread(start = true) {
            val objectString: String
            val courseList = courseDao.getCourses(application.nowTableID)
            val tableWithCourses = TableWithCourses(courseTable, courseList)
            try {
                objectString = tableWithCourses.toJson().toString()
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream!!.write(objectString.toByteArray(StandardCharsets.UTF_8))
                outputStream.close()
            } catch (e: IOException) {
                context.runOnUiThread {
                    Toast.makeText(
                        context,
                        R.string.activity_Settings_FailToBackup,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            progressDialog.postDismiss()
        }
    }

    /**
     * 导入备份的具体实现。在处理过程中，会在窗口上显示一个Dialog。
     * 这个方法应该在[Activity.onActivityResult]中被调用，
     * 或者在[AppCompatActivity.registerForActivityResult]中注册。
     * 调用前需确保已经获得日历授权。
     *
     * @param context 进行备份操作的上下文，因为要显示Dialog，仅接受Activity。
     * @param uri 备份文件的uri，需要可以被读取。
     * @param doOnImportThread 导入完成后在导入线程（工作线程）上继续的操作。
     */
    @UiThread
    fun importBackup(
        context: Activity,
        uri: Uri,
        @WorkerThread doOnImportThread: (tableID: Long, calendarID: Long?) -> Unit = { _, _ -> }
    ) {
        val contentResolver = context.contentResolver

        val progressDialog = ProgressDialog(context)
        progressDialog.show()
        thread(start = true) {
            val inputStream = contentResolver.openInputStream(uri)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            val jsonBuilder = StringBuilder()
            while (true) {
                val line = bufferedReader.readLine() ?: break
                jsonBuilder.append(line)
            }

            val tableWithCourses: TableWithCourses?
            try {
                val jsonString = jsonBuilder.toString()
                tableWithCourses = TableWithCourses.parseJson(jsonString)
            } catch (e: Exception) {
                progressDialog.postDismiss()
                context.runOnUiThread {
                    Toast.makeText(
                        context,
                        R.string.activity_Settings_ImportError,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@thread
            } finally {
                bufferedReader.close()
                inputStream?.close()
            }

            if (tableWithCourses == null) {
                progressDialog.postDismiss()
                context.runOnUiThread {
                    Toast.makeText(
                        context,
                        R.string.activity_Settings_ImportError,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@thread
            } else {
                // 数据判定合法，开始导入过程
                val newCourseTable = tableWithCourses.courseTable
                calendarOperator.createCalendarTable(context, newCourseTable)
                val tableID = courseTableDao.insertCourseTable(newCourseTable)
                // 添加课程
                for (courseWithClassTimes in tableWithCourses.courses) {
                    courseWithClassTimes.course.tableId = tableID
                    eventsOperator.addEvent(context, newCourseTable, courseWithClassTimes)
                    val courseId = courseDao.insertCourse(courseWithClassTimes.course)
                    for (classTime in courseWithClassTimes.classTimes) {
                        classTimeDao.insertClassTime(classTime.apply {
                            this.courseId = courseId
                        })
                    }
                }
                progressDialog.postDismiss()
                doOnImportThread(tableID, newCourseTable.calendarID)
            }
            progressDialog.postDismiss()
        }
    }

    /**
     * 生成用于启动导出过程的Intent。
     *
     * @param courseTable 将要备份的课程表对象，用于获取标题以决定文件名。
     * @return 生成的Intent。以这个Intent调用[Activity.startActivityForResult]或者[ActivityResultLauncher.launch]方法，
     * 启动系统文件管理器选择文件存储。
     */
    fun getExportBackupIntent(courseTable: CourseTable) =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, "${courseTable.name}.tmb")
        }

    /**
     * 生成用于启动导入过程的Intent。
     *
     * @return 生成的Intent。以这个Intent调用[Activity.startActivityForResult]或者[ActivityResultLauncher.launch]方法，
     * 启动系统文件管理器选择文件读取备份。
     */
    fun getImportBackupIntent() = Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = MIME_TYPE
    }
}