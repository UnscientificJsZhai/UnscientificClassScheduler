package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.preference.*
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable
import com.github.unscientificjszhai.unscientificclassscheduler.ui.others.ProgressDialog
import kotlinx.coroutines.launch
import java.util.*

/**
 * 供设置Activity使用的Fragment。只能用于设置Activity。
 *
 * @see SettingsActivity
 * @author UnscientificJsZhai
 */
class SettingsFragment : PreferenceFragmentCompat(),
    Preference.SummaryProvider<Preference> {

    companion object {

        /**
         * Preference当前课程表的Key。
         */
        const val CURRENT_TABLE_KEY = "current"

        /**
         * Preference总周数的Key。
         */
        const val MAX_WEEK_KEY = "weeks"

        /**
         * Preference每日上课节数的Key。
         */
        const val CLASSES_PER_DAY_KEY = "classesPerDay"

        /**
         * Preference学期开始日的Key。
         */
        const val START_DATE_KEY = "startDate"

        /**
         * Preference周开始日的Key。
         */
        const val WEEK_START_KEY = "weekStart"

        /**
         * Preference是否使用日历的Key。
         */
        const val USE_CALENDAR_KEY = "useCalendar"

        /**
         * Preference更新日历的Key。
         */
        const val UPDATE_CALENDAR_KEY = "createCalendar"

        /**
         * Preference日历颜色的Key。
         */
        const val CALENDAR_COLOR_KEY = "calendarColor"

        /**
         * Preference提醒时间的Key
         */
        const val REMIND_TIME_KEY = "remindTime"
    }

    private val viewModel: SettingsActivityViewModel by activityViewModels()

    private var currentTablePreference: Preference? = null
    private var howManyWeeksPreference: EditTextPreference? = null
    private var classesPerDayPreference: EditTextPreference? = null
    private var startDatePreference: Preference? = null
    private var weekStartPreference: ListPreference? = null

    private var useCalendarPreference: SwitchPreferenceCompat? = null
    private var updateCalendarPreference: Preference? = null
    private var calendarColorPreference: ListPreference? = null
    private var remindTimePreference: ListPreference? = null

    private var saveBackupPreference: Preference? = null
    private var importBackupPreference: Preference? = null
    private var exportIcsPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // 当前课程表的设置项
        this.currentTablePreference = findPreference(CURRENT_TABLE_KEY)
        currentTablePreference?.summaryProvider = this

        val dataStore by viewModel

        // 学期周数的设置项
        this.howManyWeeksPreference = findPreference(MAX_WEEK_KEY)
        howManyWeeksPreference?.preferenceDataStore = dataStore
        howManyWeeksPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            val content = dataStore.nowCourseTable.maxWeeks.toString()
            editText.setText(content)
            editText.setSelection(content.length)
        }
        howManyWeeksPreference?.summaryProvider = this

        // 每日上课节数的设置项
        this.classesPerDayPreference = findPreference(CLASSES_PER_DAY_KEY)
        classesPerDayPreference?.preferenceDataStore = dataStore
        classesPerDayPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            val content = dataStore.nowCourseTable.classesPerDay.toString()
            editText.setText(content)
            editText.setSelection(content.length)
        }
        classesPerDayPreference?.summaryProvider = this

        // 学期开始日的设置项
        this.startDatePreference = findPreference(START_DATE_KEY)
        startDatePreference?.setOnPreferenceClickListener {
            val parentActivity = activity
            if (parentActivity is SettingsActivity) {
                DatePickerDialog(
                    requireActivity(),
                    { _, year, month, dayOfMonth ->
                        dataStore.nowCourseTable.startDate.set(year, month, dayOfMonth)
                        dataStore.updateCourseTable()
                    },
                    dataStore.nowCourseTable.startDate.get(Calendar.YEAR),
                    dataStore.nowCourseTable.startDate.get(Calendar.MONTH),
                    dataStore.nowCourseTable.startDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            true
        }
        startDatePreference?.summaryProvider = this

        // 周开始日的设置项
        this.weekStartPreference = findPreference(WEEK_START_KEY)
        weekStartPreference?.preferenceDataStore = dataStore

        // 读取日历开关设置
        val useCalendar = (requireActivity().application as SchedulerApplication).useCalendar

        // 更新日历的设置项
        this.updateCalendarPreference = findPreference(UPDATE_CALENDAR_KEY)
        this.updateCalendarPreference?.isEnabled = useCalendar
        updateCalendarPreference?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.preferences_UpdateCalendar_DialogTitle)
                .setMessage(R.string.preferences_UpdateCalendar_DialogMessage)
                .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                    dialog.dismiss()
                }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                    val progressDialog = ProgressDialog(requireActivity())
                    progressDialog.show()
                    viewModel.viewModelScope.launch {
                        viewModel.updateCalendar(requireActivity())
                        //完成后关闭ProgressDialog。
                        progressDialog.postDismiss()
                        Toast.makeText(
                            requireContext(),
                            R.string.preferences_UpdateCalendar_Complete,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog.dismiss()
                }.show()
            true
        }

        // 日历颜色设置
        this.calendarColorPreference = findPreference(CALENDAR_COLOR_KEY)
        this.calendarColorPreference?.isEnabled = useCalendar
        calendarColorPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                viewModel.viewModelScope.launch {
                    viewModel.setCalendarColor(requireActivity(), newValue)
                }
            }
            true
        }


        this.remindTimePreference = findPreference(REMIND_TIME_KEY)
        this.remindTimePreference?.isEnabled = useCalendar
        // 提醒时间更新后立刻通知
        remindTimePreference?.setOnPreferenceChangeListener { preference, newValue ->
            if ((preference as ListPreference).value != (newValue as String)) {
                // 立刻更新日历
                val progressDialog = ProgressDialog(requireActivity())
                progressDialog.show()
                viewModel.viewModelScope.launch {
                    viewModel.updateCalendar(requireActivity())
                    progressDialog.postDismiss()
                    Toast.makeText(
                        requireContext(),
                        R.string.preferences_UpdateCalendar_Complete,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            true
        }

        // 设置使用日历功能的设置项
        this.useCalendarPreference = findPreference(USE_CALENDAR_KEY)
        useCalendarPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (newValue) {
                    // 检查权限
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_CALENDAR
                        ) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.READ_CALENDAR
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        Toast.makeText(
                            requireContext(),
                            R.string.preferences_CalendarOption_UseCalendar_NotGranted,
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnPreferenceChangeListener false
                    } else {
                        // 重新创建日历表
                        val progressDialog = ProgressDialog(requireActivity())
                        progressDialog.show()
                        viewModel.viewModelScope.launch {
                            viewModel.updateCalendar(requireActivity())
                            //完成后关闭ProgressDialog。
                            progressDialog.postDismiss()
                            Toast.makeText(
                                requireContext(),
                                R.string.preferences_UpdateCalendar_Complete,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // 删除所有日历表
                    val progressDialog = ProgressDialog(requireActivity())
                    progressDialog.show()
                    viewModel.viewModelScope.launch {
                        viewModel.clearCalendar(requireActivity())
                        //完成后关闭ProgressDialog。
                        progressDialog.postDismiss()
                        Toast.makeText(
                            requireContext(),
                            R.string.preferences_UpdateCalendar_Complete,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                this.updateCalendarPreference?.isEnabled = newValue
                this.calendarColorPreference?.isEnabled = newValue
                this.remindTimePreference?.isEnabled = newValue
            }
            true
        }

        this.saveBackupPreference = findPreference("backup")
        saveBackupPreference?.setOnPreferenceClickListener {
            (requireActivity() as SettingsActivity).saveBackup()
        }

        this.importBackupPreference = findPreference("import")
        importBackupPreference?.setOnPreferenceClickListener {
            (requireActivity() as SettingsActivity).importBackup()
        }

        this.exportIcsPreference = findPreference("ics")
        exportIcsPreference?.setOnPreferenceClickListener {
            (requireActivity() as SettingsActivity).exportIcs()
        }
    }

    override fun onResume() {
        super.onResume()
        this.weekStartPreference?.apply {
            value = preferenceDataStore?.getString(key, "0")
        }
    }

    /**
     * 宿主Activity用于通知CourseTable更新的方法。
     *
     * @param courseTable 更新后的CourseTable。
     */
    fun updateCourseTable(courseTable: CourseTable) {
        val dataStore by viewModel
        dataStore.nowCourseTable = courseTable
        // 通过重设summaryProvider的方法更新Summary
        this.currentTablePreference?.summaryProvider = this
        this.howManyWeeksPreference?.summaryProvider = this
        this.classesPerDayPreference?.summaryProvider = this
        this.startDatePreference?.summaryProvider = this
    }

    override fun provideSummary(preference: Preference): String {
        val dataStore by viewModel
        return when (preference) {
            this.currentTablePreference -> dataStore.nowCourseTable.name
            this.howManyWeeksPreference -> dataStore.nowCourseTable.maxWeeks.toString() +
                    getString(R.string.preferences_CurrentTable_MaxWeeksSummary)
            this.classesPerDayPreference -> dataStore.nowCourseTable.classesPerDay.toString() +
                    getString(R.string.preferences_CurrentTable_ClassesPerDaySummary)
            this.startDatePreference -> getString(R.string.preferences_CurrentTable_StartDateSummary).format(
                dataStore.nowCourseTable.startDate.get(Calendar.YEAR),
                dataStore.nowCourseTable.startDate.get(Calendar.MONTH) + 1,
                dataStore.nowCourseTable.startDate.get(Calendar.DATE)
            )
            else -> ""
        }
    }
}