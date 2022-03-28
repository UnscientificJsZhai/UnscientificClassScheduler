package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.features.backup.BackupOperator
import com.github.unscientificjszhai.unscientficclassscheduler.features.backup.CourseICS
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.CalendarOperatorActivity
import com.github.unscientificjszhai.unscientficclassscheduler.util.setSystemUIAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置Activity，设置项的初始化在SettingsFragment中。使用了JetPack库的Preference库。
 *
 * @see SettingsFragment
 * @author UnscientificJsZhai
 */
class SettingsActivity : CalendarOperatorActivity() {

    private lateinit var schedulerApplication: SchedulerApplication

    internal lateinit var viewModel: SettingsActivityViewModel

    private lateinit var backupLauncher: ActivityResultLauncher<Intent>
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportIcsLauncher: ActivityResultLauncher<Intent>

    inner class DatabaseChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context is SettingsActivity) {
                // 当在Selector中选择新的课程表时调用此方法更新DataStore中的courseTable对象
                // 并且会同时更新子Fragment中的数据简介
                settingsFragment?.updateCourseTable(schedulerApplication.courseTable!!)
            }
        }
    }

    private lateinit var databaseChangeReceiver: DatabaseChangeReceiver

    private var settingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.activity_settings)

        // 设置SystemUI颜色
        setSystemUIAppearance(this)

        this.schedulerApplication = application as SchedulerApplication
        val courseTable by schedulerApplication

        val dataStore = SettingsDataStore(
            courseTable,
            schedulerApplication.getCourseDatabase().courseTableDao(),
            this,
            schedulerApplication::updateTableID
        )
        this.viewModel = ViewModelProvider(
            this,
            SettingsActivityViewModel.Factory(dataStore)
        )[SettingsActivityViewModel::class.java]

        // 替换Fragment

        this.settingsFragment = SettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, this.settingsFragment!!)
            .commit()

        // 监听数据库变更
        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.COURSE_DATABASE_CHANGE_ACTION)
        this.databaseChangeReceiver = DatabaseChangeReceiver()
        registerReceiver(this.databaseChangeReceiver, intentFilter)

        // 定义保存备份的逻辑
        this.backupLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val uri = it.data?.data
                    if (uri != null) {
                        BackupOperator.exportBackup(this, uri)
                    }
                }
            }

        // 定义导入备份的逻辑
        this.importLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val uri = it.data?.data
                    if (uri != null) {
                        BackupOperator.importBackup(this, uri)
                    }
                }
            }

        // 定义导出ICS的逻辑
        this.exportIcsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val uri = it.data?.data
                    if (uri != null) {
                        viewModel.viewModelScope.launch {
                            val courseICS = withContext(Dispatchers.Default) {
                                // 获取当前选中的学期的课程表
                                val courseList = schedulerApplication.getCourseDatabase()
                                    .courseDao().getCourses(schedulerApplication.nowTableID)
                                CourseICS(courseList, courseTable)
                            }
                            courseICS.writeToFile(this@SettingsActivity, uri)
                        }
                    }
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.SettingsActivity_Info -> {
                val intent = Intent(this, InfoActivity::class.java)
                startActivity(intent)
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    override fun onDestroy() {
        unregisterReceiver(this.databaseChangeReceiver)
        super.onDestroy()
    }

    /**
     * Fragment调用的，保存备份时调用的方法。
     *
     * @return 给onPreferenceClick做返回值的true。
     */
    internal fun saveBackup(): Boolean {
        val courseTable by schedulerApplication
        backupLauncher.launch(BackupOperator.getExportBackupIntent(courseTable))
        return true
    }

    /**
     * Fragment调用的，导入备份时调用的方法。
     *
     * @return 给onPreferenceClick做返回值的true。
     */
    internal fun importBackup(): Boolean {
        importLauncher.launch(BackupOperator.getImportBackupIntent())
        return true
    }

    /**
     * Fragment调用的，导出ics时调用的方法。
     *
     * @return 给onPreferenceClick做返回值的true。
     */
    internal fun exportIcs(): Boolean {
        val courseTable by schedulerApplication
        exportIcsLauncher.launch(CourseICS.getExportIcsIntent(courseTable))
        return true
    }
}