package com.github.unscientificjszhai.unscientficclassscheduler.ui.main.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.ui.editor.EditCourseActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.CourseAdapter
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.MainActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.MainActivityViewModel
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.RecyclerViewWithContextMenu
import com.github.unscientificjszhai.unscientficclassscheduler.ui.parse.ParseCourseActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.settings.SettingsActivity
import com.github.unscientificjszhai.unscientficclassscheduler.util.jumpToSystemPermissionSettings
import com.github.unscientificjszhai.unscientficclassscheduler.util.runIfPermissionGranted
import com.github.unscientificjszhai.unscientficclassscheduler.util.startActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

class CourseListFragment : Fragment() {

    internal val viewModel: MainActivityViewModel by activityViewModels()

    private lateinit var rootView: CoordinatorLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: CourseAdapter

    private lateinit var progressBar: ProgressBar
    private lateinit var schedulerApplication: SchedulerApplication

    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        this.schedulerApplication = requireActivity().application as SchedulerApplication
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
        this.recyclerViewAdapter = CourseAdapter(requireActivity() as MainActivity)
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
        val currentTimeMarker by requireActivity() as MainActivity

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
                                        MainActivityViewModel.deleteCourse(
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
                                                MainActivityViewModel.undoDeleteCourse(
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
        val currentTimeMarker by requireActivity() as MainActivity
        progressBar.visibility = View.GONE
        val listToSubmit = if (viewModel.showTodayOnly) {
            currentTimeMarker.getTodayCourseList(courseList)
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
        val currentTimeMarker by requireActivity() as MainActivity
        updateActionBarLabel()
        if (currentTimeMarker.getWeekNumber() == 0) {
            viewModel.showTodayOnly = false
        }

        //监听LiveData变更
        this.viewModel.courseList.observe(viewLifecycleOwner) {
            bindData(it)
        }

        changeText()
    }

    override fun onPause() {
        super.onPause()
        viewModel.courseList.removeObservers(viewLifecycleOwner) //每次暂停都移除监听
    }

    /**
     * 更新ActionBar的标题。
     */
    fun updateActionBarLabel() {
        val currentTimeMarker by requireActivity() as MainActivity
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
                    )
                        .append(" ")
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
        (requireActivity() as MainActivity).updateLabel(stringBuilder.toString())
    }
}