package com.github.unscientificjszhai.unscientficclassscheduler.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientficclassscheduler.features.backup.BackupOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.CalendarOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.calendar.EmptyAuthenticator
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.CalendarOperatorActivity
import com.github.unscientificjszhai.unscientficclassscheduler.util.jumpToSystemPermissionSettings
import com.github.unscientificjszhai.unscientficclassscheduler.util.runIfPermissionGranted
import com.github.unscientificjszhai.unscientficclassscheduler.util.setSystemUIAppearance
import com.github.unscientificjszhai.unscientficclassscheduler.util.startActivity
import kotlin.concurrent.thread

/**
 * 首页Activity，用于处理授权和创建第一个课程表。
 *
 * @author UnscientificJsZhai
 */
class WelcomeActivity : CalendarOperatorActivity(), View.OnClickListener {

    private lateinit var timeManagerApplication: TimeManagerApplication

    private lateinit var tableTitleEditText: EditText
    private lateinit var startButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView

    private lateinit var importBackupLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 添加帐号
        EmptyAuthenticator.addAccountToSystem(this)

        this.timeManagerApplication = application as TimeManagerApplication

        if (this.timeManagerApplication.nowTableID != TimeManagerApplication.DEFAULT_DATABASE_OBJECT_ID) {
            startActivity<MainActivity>(this)
            finish()
        } else {
            // 如果当前所在表的ID为-1，就是没有初始化
            setContentView(R.layout.activity_welcome)

            setSystemUIAppearance(this)

            this.tableTitleEditText = findViewById(R.id.WelcomeActivity_TableNameEditText)
            this.startButton = findViewById(R.id.WelcomeActivity_Button)
            this.progressBar = findViewById(R.id.WelcomeActivity_ProgressBar)
            this.textView = findViewById(R.id.WelcomeActivity_Text)

            // 回车确认功能
            this.tableTitleEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.onClick(this.startButton)
                }
                true
            }

            this.startButton.setOnClickListener(this)
        }

        // 注册导入功能
        this.importBackupLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val uri = it.data?.data
                if (uri != null) {
                    BackupOperator.importBackup(this, uri) { tableID, calendarID ->
                        timeManagerApplication.updateTableID(tableID)
                        CalendarOperator.deleteAllTables(this, calendarID)
                        runOnUiThread {
                            startActivity<MainActivity>(this)
                            finish()
                        }
                    }
                }
            }

        // 注册权限请求
        this.permissionRequestLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // 获得了写入日历的权限,继续写入日历。
                    createFirstTable(
                        if (tableTitleEditText.text.isBlank()) {
                            // 当输入为空时的默认课程表名
                            getString(R.string.activity_Welcome_EditTextHint)
                        } else {
                            tableTitleEditText.text.toString()
                        }
                    )
                }
            }
    }

    override fun onClick(v: View?) {
        startButton.isEnabled = false
        textView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        val tableName: String = if (tableTitleEditText.text.isEmpty()) {
            // 当输入为空时的默认课程表名
            getString(R.string.activity_Welcome_EditTextHint)
        } else {
            tableTitleEditText.text.toString()
        }

        if (timeManagerApplication.useCalendar) {
            runIfPermissionGranted(Manifest.permission.WRITE_CALENDAR, {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                    // 系统仍然提示权限请求
                    AlertDialog.Builder(this)
                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                        .setMessage(R.string.activity_WelcomeActivity_AskPermissionText)
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            dialog.dismiss()
                        }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                //申请日历权限
                                permissionRequestLauncher.launch(Manifest.permission.WRITE_CALENDAR)
                            }
                            dialog.dismiss()
                        }.show()
                } else {
                    // 系统不再提示权限请求
                    AlertDialog.Builder(this)
                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                        .setMessage(
                            getString(R.string.activity_WelcomeActivity_AskPermissionText) + "\n"
                                    + getString(R.string.activity_WelcomeActivity_SettingsPermissionText)
                        )
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            dialog.dismiss()
                        }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            jumpToSystemPermissionSettings()
                            dialog.dismiss()
                        }.show()
                }

            }) {
                createFirstTable(tableName)
            }
        } else {
            createFirstTable(tableName)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_welcome_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.WelcomeActivity_ImportBackup) {
            runIfPermissionGranted(Manifest.permission.WRITE_CALENDAR, {
                Toast.makeText(
                    this,
                    R.string.activity_WelcomeActivity_AskPermissionText,
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                importBackupLauncher.launch(BackupOperator.getImportBackupIntent())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 创建本应用的第一个课程表对象，同时也会将其写入日历。
     * 此方法不会检查日历权限，需要在调用钱检查。
     *
     * @param tableName 课程表的标题。
     */
    private fun createFirstTable(tableName: String) {
        thread(start = true) {
            val courseTable = CourseTable(tableName)

            if (timeManagerApplication.useCalendar) {
                CalendarOperator.deleteAllTables(this) // 检查并删除清除数据之前的遗留日历表。
                // 写入日历。
                CalendarOperator.createCalendarTable(this, courseTable)
            }
            // 写入数据库。
            val dao = timeManagerApplication.getCourseTableDatabase().courseTableDao()
            val id = dao.insertCourseTable(courseTable)
            timeManagerApplication.updateTableID(id)

            runOnUiThread {
                startActivity<MainActivity>(this)
                finish()
            }
        }
    }
}