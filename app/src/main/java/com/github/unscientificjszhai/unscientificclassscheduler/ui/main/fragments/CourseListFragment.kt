package com.github.unscientificjszhai.unscientificclassscheduler.ui.main.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.CurrentTimeMarker
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.ui.editor.EditCourseActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.CourseAdapter
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.MainActivityLabelViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.MainActivityViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.ui.others.RecyclerViewWithContextMenu
import com.github.unscientificjszhai.unscientificclassscheduler.ui.parse.ParseCourseActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.SettingsActivity
import com.github.unscientificjszhai.unscientificclassscheduler.util.jumpToSystemPermissionSettings
import com.github.unscientificjszhai.unscientificclassscheduler.util.runIfPermissionGranted
import com.github.unscientificjszhai.unscientificclassscheduler.util.startActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

/**
 * 首页显示课程列表的Fragment。
 *
 * @author UnscientificJsZhai
 */
class CourseListFragment : Fragment() {

    private val viewModel: MainActivityViewModel by activityViewModels()
    private val labelViewModel: MainActivityLabelViewModel by activityViewModels()

    private lateinit var rootView: CoordinatorLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: CourseAdapter

    private lateinit var progressBar: ProgressBar
    private lateinit var schedulerApplication: SchedulerApplication

    private lateinit var emptyTextView: TextView

    private val currentTimeMarker: CurrentTimeMarker by lazy {
        schedulerApplication.courseTable?.let {
            CurrentTimeMarker(it)
        } ?: throw RuntimeException()
    }

    /**
     * 处理系统日期变更的广播接收器。
     */
    inner class DateChangeReceiver : BroadcastReceiver() {

        /**
         * 上次收到时间变化广播的时间。
         */
        private var timeBeforeUpdate = Calendar.getInstance()

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                // 检测日期是否发生变化
                val timeRightNow = Calendar.getInstance()
                if (this.timeBeforeUpdate.get(Calendar.DAY_OF_YEAR) != timeRightNow.get(Calendar.DAY_OF_YEAR) ||
                    this.timeBeforeUpdate.get(Calendar.YEAR) != timeRightNow.get(Calendar.YEAR)
                ) {
                    // 如果为只显示今天则更新数据集
                    if (lifecycle.currentState == Lifecycle.State.STARTED) {
                        bindData(viewModel.courseList.value ?: ArrayList())
                    }
                }

                // 最后更新新的当前时间
                this.timeBeforeUpdate = timeRightNow
            }
        }
    }

    /**
     * 用于处理当前课程表更改事件的广播接收器。
     */
    inner class DatabaseChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            // 更新NowTimeTagger
            val schedulerApplication = context.applicationContext as SchedulerApplication
            val courseTable by schedulerApplication
            currentTimeMarker.courseTable = courseTable

            viewModel.courseList.removeObservers(this@CourseListFragment)
            viewModel.courseList = schedulerApplication
                .getCourseDatabase().courseDao()
                .getLiveCourses(schedulerApplication.nowTableID) // 更新ViewModel中的LiveData
            viewModel.courseList.observe(this@CourseListFragment) { courseList ->
                bindData(courseList)
                updateActionBarLabel()
            }
        }
    }

    private lateinit var databaseChangeReceiver: DatabaseChangeReceiver

    private lateinit var dateChangeReceiver: DateChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        this.schedulerApplication = requireActivity().application as SchedulerApplication

        // 监听日期变更
        this.dateChangeReceiver = DateChangeReceiver()
        registerReceiver(requireContext(), this.dateChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
        }, ContextCompat.RECEIVER_EXPORTED)

        // 监听数据库变更
        this.databaseChangeReceiver = DatabaseChangeReceiver()
        registerReceiver(requireContext(), this.databaseChangeReceiver, IntentFilter().apply {
            addAction(MainActivity.COURSE_DATABASE_CHANGE_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        this.progressBar = view.findViewById(R.id.MainActivity_ProgressBar)
        this.rootView = view.findViewById(R.id.MainActivity_RootView)

        this.recyclerView = view.findViewById(R.id.MainActivity_RootRecyclerView)
        this.recyclerViewAdapter = CourseAdapter(
            currentTimeMarker,
            isShowTodayOnly()
        )
        this.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        this.recyclerView.adapter = this.recyclerViewAdapter
        registerForContextMenu(recyclerView)

        this.emptyTextView = view.findViewById(R.id.MainActivity_EmptyScreenTextView)

        view.findViewById<FloatingActionButton>(R.id.MainActivity_FloatingActionButton)
            .setOnClickListener {
                EditCourseActivity.startThisActivity(requireContext())
            }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.MainActivity_ShowTodayOnly).apply {
            isEnabled = currentTimeMarker.getWeekNumber() != 0
            isChecked = viewModel.showTodayOnly
        }
        menu.findItem(R.id.MainActivity_Parse).isVisible = viewModel.isListEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.MainActivity_Settings -> startActivity<SettingsActivity>(requireActivity())
            R.id.MainActivity_ShowTodayOnly -> {
                viewModel.showTodayOnly = !item.isChecked
                recyclerViewAdapter.setShowTodayOnly(viewModel.showTodayOnly)
                bindData(viewModel.courseList.value ?: ArrayList())
                updateActionBarLabel()
            }
            R.id.MainActivity_JumpToCalendar -> {
                Calendar.getInstance().timeInMillis
                val startCalendarIntent = Intent(Intent.ACTION_VIEW).apply {
                    data =
                        Uri.parse("content://com.android.calendar/time/${Calendar.getInstance().timeInMillis}")
                }
                startActivity(startCalendarIntent)
            }
            R.id.MainActivity_Parse -> startActivity<ParseCourseActivity>(requireActivity())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.context_main, menu)
        if (v is RecyclerView && menuInfo is RecyclerViewWithContextMenu.PositionMenuInfo) {
            try {
                val courseWithClassTimes =
                    (recyclerView.adapter as CourseAdapter).currentList[menuInfo.position]
                menu.setHeaderTitle(courseWithClassTimes.course.title)
            } catch (e: NullPointerException) {
                menu.close()
            }
        } else {
            menu.close()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo
        if (info is RecyclerViewWithContextMenu.PositionMenuInfo) {
            val courseWithClassTimes =
                (recyclerView.adapter as CourseAdapter).currentList[info.position]

            when (item.itemId) {
                R.id.MainActivity_Edit -> {
                    if (courseWithClassTimes != null) {
                        EditCourseActivity.startThisActivity(requireContext(), courseWithClassTimes)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.activity_CourseDetail_DataError,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                R.id.MainActivity_Delete -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.activity_CourseDetail_DeleteConfirm)
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            dialog?.dismiss()
                        }
                        .setPositiveButton(R.string.common_confirm) { dialog, _ ->

                            requireActivity().runIfPermissionGranted(
                                Manifest.permission.WRITE_CALENDAR,
                                {
                                    dialog.dismiss()
                                    AlertDialog.Builder(this)
                                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                                        .setMessage(R.string.activity_CourseDetail_AskPermissionText)
                                        .setNegativeButton(R.string.common_cancel) { permissionDialog, _ ->
                                            //拒绝授予日历权限。
                                            permissionDialog.dismiss()
                                        }
                                        .setPositiveButton(R.string.common_confirm) { permissionDialog, _ ->
                                            //同意授予日历权限，跳转到系统设置进行授权。
                                            requireActivity().jumpToSystemPermissionSettings()
                                            permissionDialog.dismiss()
                                        }
                                }) {
                                if (courseWithClassTimes != null) {
                                    viewModel.viewModelScope.launch {
                                        viewModel.deleteCourse(
                                            requireActivity(),
                                            courseWithClassTimes,
                                            schedulerApplication.useCalendar
                                        )
                                        val snackBar = Snackbar.make(
                                            rootView,
                                            R.string.activity_Main_DeletedMessage,
                                            Snackbar.LENGTH_LONG
                                        )
                                        snackBar.setAction(R.string.common_undo) {
                                            viewModel.viewModelScope.launch {
                                                viewModel.undoDeleteCourse(
                                                    requireActivity(),
                                                    courseWithClassTimes,
                                                    schedulerApplication.useCalendar
                                                )
                                            }
                                        }
                                        snackBar.show()
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        R.string.activity_CourseDetail_DataError,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dialog.dismiss()
                            }
                        }.create().show()
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    /**
     * 更新列表数据的方法。会判断是否仅显示今天的课程，然后传入正确的列表给[CourseAdapter]。
     *
     * @param courseList 完整的Course列表。
     */
    fun bindData(courseList: List<CourseWithClassTimes>) {
        progressBar.visibility = View.GONE
        val listToSubmit = if (viewModel.showTodayOnly) {
            currentTimeMarker.getTodayCourseList(originalList = courseList)
        } else {
            courseList
        }
        recyclerViewAdapter.submitList(listToSubmit)

        if (listToSubmit.isEmpty()) {
            this.emptyTextView.visibility = View.VISIBLE
            changeText()
        } else {
            this.emptyTextView.visibility = View.GONE
        }
    }

    /**
     * 更改中心TextView的文字。
     */
    private fun changeText() {
        val messagesList = context?.resources?.getStringArray(R.array.fragment_CourseList_Empty)
        this.emptyTextView.text =
            messagesList?.randomOrNull() ?: getString(R.string.fragment_CourseList_EmptyList1)
    }

    override fun onStart() {
        super.onStart()
        updateActionBarLabel()
        if (currentTimeMarker.getWeekNumber() == 0) {
            viewModel.showTodayOnly = false
        }

        // 监听LiveData变更
        this.viewModel.courseList.observe(viewLifecycleOwner) {
            bindData(it)
        }

        changeText()
    }

    override fun onPause() {
        super.onPause()
        viewModel.courseList.removeObservers(viewLifecycleOwner) //每次暂停都移除监听
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(this.databaseChangeReceiver)
        requireActivity().unregisterReceiver(this.dateChangeReceiver)
    }

    /**
     * 更新ActionBar的标题。
     */
    fun updateActionBarLabel() {
        val application = requireActivity().application as SchedulerApplication
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val option = sharedPreferences.getString("showOnMainActivity", "table")
        val stringBuilder = StringBuilder()
        val courseTable by application
        when (option) {
            "table" -> stringBuilder.append(courseTable.name)
            "today" -> {

                /**
                 * 用来获取当前是星期几的局部函数。
                 *
                 * @return 表示星期几的字符串，可以直接用于显示。
                 */
                fun dayOfWeek(): String = getString(
                    when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> R.string.data_Week1
                        Calendar.TUESDAY -> R.string.data_Week2
                        Calendar.WEDNESDAY -> R.string.data_Week3
                        Calendar.THURSDAY -> R.string.data_Week4
                        Calendar.FRIDAY -> R.string.data_Week5
                        Calendar.SATURDAY -> R.string.data_Week6
                        else -> R.string.data_Week0
                    }
                )

                val weekNumber = currentTimeMarker.getWeekNumber()
                if (weekNumber == 0) {
                    stringBuilder.append(getString(R.string.activity_Main_NotStartYet))
                } else {
                    stringBuilder.append(
                        getString(R.string.view_ClassTimeEdit_WeekItem_ForKotlin)
                            .format(currentTimeMarker.getWeekNumber())
                    ).append(" ")
                        .append(dayOfWeek())
                        .append(" ")
                        .append(
                            getString(
                                if (viewModel.showTodayOnly) {
                                    R.string.activity_Main_ActionBarLabel_TodayOnly
                                } else {
                                    R.string.activity_Main_ActionBarLabel_All
                                }
                            )
                        )
                }
            }
        }
        labelViewModel.postLabel(stringBuilder.toString())
    }

    /**
     * 查询是否只显示今天，提供给Adapter使用。
     *
     * @return 是否只显示今天。
     */
    private fun isShowTodayOnly(): Boolean = this.viewModel.showTodayOnly
}