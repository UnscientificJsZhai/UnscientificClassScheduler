package com.github.unscientificjszhai.unscientficclassscheduler.features.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.database.CourseDatabase
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.CalendarOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.ProgressDialog
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

/**
 * 导出和导入备份的操作类。
 * 使用序列化和反序列化实现功能。
 *
 * @author UnscientificJsZhai
 */
object BackupOperator {

    /**
     * 导出备份的具体实现。在处理过程中，会在窗口上显示一个Dialog。
     * 这个方法应该在[Activity.onActivityResult]中被调用，
     * 或者在[AppCompatActivity.registerForActivityResult]中注册。
     *
     * @param context 进行备份操作的上下文，因为要显示Dialog，仅接受Activity。
     * @param uri 备份文件的uri，需要可以被写入。
     */
    @UiThread
    fun exportBackup(context: Activity, uri: Uri) {
        val timeManagerApplication = (context.applicationContext) as TimeManagerApplication
        val courseTable by timeManagerApplication
        val contentResolver = context.contentResolver

        val progressDialog = ProgressDialog(context)
        progressDialog.show()
        thread(start = true) {
            val objectString: String
            val courseList =
                timeManagerApplication.getCourseDatabase().courseDao().getCourses()
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
        doOnImportThread: (tableID: Long, calendarID: Long?) -> Unit = { _, _ -> }
    ) {
        val timeManagerApplication = (context.applicationContext) as TimeManagerApplication
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
                tableWithCourses =
                    TableWithCourses.parseJson(jsonString)
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
                //数据判定合法，开始导入过程
                val newCourseTable = tableWithCourses.courseTable
                val courseTableDao =
                    timeManagerApplication.getCourseTableDatabase()
                        .courseTableDao()
                CalendarOperator.createCalendarTable(context, newCourseTable)
                val tableID = courseTableDao.insertCourseTable(newCourseTable)
                //创建Course数据库文件
                val courseDatabase =
                    Room.databaseBuilder(
                        context,
                        CourseDatabase::class.java,
                        "table$tableID.db"
                    ).build()
                //添加课程
                val courseDao = courseDatabase.courseDao()
                val classTimeDao = courseDatabase.classTimeDao()
                for (courseWithClassTimes in tableWithCourses.courses) {
                    EventsOperator.addEvent(context, newCourseTable, courseWithClassTimes)
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
            type = "application/octet-stream"
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
        type = "application/octet-stream"
    }
}