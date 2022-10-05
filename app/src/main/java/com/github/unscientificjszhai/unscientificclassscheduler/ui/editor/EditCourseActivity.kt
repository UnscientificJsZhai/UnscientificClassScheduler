package com.github.unscientificjszhai.unscientificclassscheduler.ui.editor

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.data.database.CourseDatabase
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.CourseDetailActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.others.CalendarOperatorActivity
import com.github.unscientificjszhai.unscientificclassscheduler.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 编辑单个课程信息的Activity。
 *
 * @author UnscientificJsZhai
 */
@AndroidEntryPoint
class EditCourseActivity : CalendarOperatorActivity(), View.OnClickListener,
    View.OnLongClickListener {

    companion object {

        /**
         * 启动此Activity的通用方法。
         *
         * @param context 上下文。
         */
        @JvmStatic
        fun startThisActivity(context: Context) {
            startActivity<EditCourseActivity>(context)
        }

        /**
         * 启动此Activity的专用方法，使用[startActivityForResult]方法启动，
         * 请求码为[CourseDetailActivity.EDIT_REQUEST_CODE]。
         *
         * @param context 上下文。
         * @param courseWithClassTimes 数据对象。
         * @see CourseDetailActivity
         */
        @JvmStatic
        fun startThisActivity(
            context: Context,
            courseWithClassTimes: CourseWithClassTimes
        ) {
            startActivity<EditCourseActivity>(context) {
                putExtra(CourseDetailActivity.INTENT_EXTRA_COURSE, courseWithClassTimes)
            }
        }
    }

    private lateinit var schedulerApplication: SchedulerApplication

    private lateinit var viewModel: EditCourseActivityViewModel

    private lateinit var courseDatabase: CourseDatabase

    private lateinit var rootRecyclerView: RecyclerView
    private lateinit var adapter: EditCourseAdapter
    private lateinit var headerAdapter: EditCourseHeaderAdapter

    private lateinit var floatingActionButton: FloatingActionButton

    /**
     * 保存数据时申请权限的回调。
     */
    private lateinit var requestWriteCalendarPermissionCallback: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_course)

        // 初始化ViewModel
        this.viewModel = ViewModelProvider(this)[EditCourseActivityViewModel::class.java]

        setSystemUIAppearance(this)

        this.schedulerApplication = application as SchedulerApplication

        this.rootRecyclerView = findViewById(R.id.EditCourseActivity_RecyclerView)

        @Suppress("DEPRECATION")
        val courseWithClassTimes =
            intent.getSerializableExtra(CourseDetailActivity.INTENT_EXTRA_COURSE)
        if (courseWithClassTimes is CourseWithClassTimes) {
            // 给ViewModel设定值
            this.viewModel.course = courseWithClassTimes.course

            if (!viewModel.classTimesInitialized) {
                viewModel.classTimes = ArrayList(courseWithClassTimes.classTimes)
            }
        } else {
            this.viewModel.course = Course(schedulerApplication.nowTableID)

            if (!viewModel.classTimesInitialized) {
                viewModel.classTimes = ArrayList()
                viewModel.classTimes.add(ClassTime())
            }
        }
        // 保证ViewModel中的classTimes数组已经初始化
        viewModel.classTimesInitialized = true

        this.headerAdapter =
            EditCourseHeaderAdapter(viewModel.course ?: Course(schedulerApplication.nowTableID))
        this.adapter = EditCourseAdapter(
            viewModel.classTimes,
            schedulerApplication.courseTable?.maxWeeks ?: ClassTime.MAX_STORAGE_SIZE
        )

        val linearLayoutManager = LinearLayoutManager(this)
        this.rootRecyclerView.layoutManager = linearLayoutManager
        this.rootRecyclerView.adapter = ConcatAdapter(headerAdapter, adapter)

        // 从Application获取Database的引用
        this.courseDatabase = (application as SchedulerApplication).getCourseDatabase()

        // 浮动按钮的监听器
        this.floatingActionButton = findViewById(R.id.EditCourseActivity_PlusButton)
        floatingActionButton.setOnClickListener(this)
        floatingActionButton.setOnLongClickListener(this)

        // 注册申请权限回调
        this.requestWriteCalendarPermissionCallback = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                viewModel.viewModelScope.launch {
                    viewModel.saveData(this@EditCourseActivity, schedulerApplication.useCalendar)
                    finish()
                }
            }
        }

        // 注册返回监听
        onBackPressedDispatcher.addCallback {
            showUnsavedAlertDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_course_edit_activity, menu)
        return true
    }

    /**
     * 菜单栏项目点击监听。实现保存功能。
     *
     * @param item 菜单项目
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.EditCourseActivity_Done -> {
                runIfPermissionGranted(Manifest.permission.WRITE_CALENDAR, {
                    // 没有获得权限时。
                    AlertDialog.Builder(this)
                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                        .setMessage(R.string.activity_EditCourse_AskPermissionText)
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            dialog.dismiss()
                        }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                                requestWriteCalendarPermissionCallback.launch(Manifest.permission.WRITE_CALENDAR)
                            } else {
                                jumpToSystemPermissionSettings()
                            }
                            dialog.dismiss()
                        }
                }) {
                    rootRecyclerView.clearFocus()
                    viewModel.viewModelScope.launch {
                        viewModel.saveData(
                            this@EditCourseActivity,
                            schedulerApplication.useCalendar
                        )
                        finish()
                    }
                }
            }

            // 按下左上角返回箭头的逻辑。
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }
        return true
    }

    override fun onClick(v: View?) {
        when (v) {
            // 长按添加按钮的回调。
            this.floatingActionButton -> {
                val lastClassTime = viewModel.classTimes.lastOrNull()
                if (lastClassTime == null || !viewModel.copyFromPrevious) {
                    viewModel.classTimes.add(ClassTime())
                } else {
                    rootRecyclerView.clearFocus()
                    viewModel.classTimes.add(ClassTime(lastClassTime))
                }
                this.adapter.notifyItemInserted(adapter.itemCount - 1)

                //滚动到底部
                this.rootRecyclerView.scrollToBottom()
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        when (v) {
            this.floatingActionButton -> {
                viewModel.copyFromPrevious = !viewModel.copyFromPrevious

                Toast.makeText(
                    this,
                    if (viewModel.copyFromPrevious) {
                        R.string.activity_EditCourse_CopyFromPrevious_True
                    } else {
                        R.string.activity_EditCourse_CopyFromPrevious_False
                    },
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }
        return false
    }

    /**
     * 显示未保存内容的警告弹窗。
     */
    private fun showUnsavedAlertDialog() {
        AlertDialog.Builder(this).setTitle(R.string.activity_EditCourse_UnsavedAlertTitle)
            // 确定按键
            .setPositiveButton(R.string.common_confirm) { dialog, _ ->
                dialog?.dismiss()
                this.finish()
            }
            // 取消按键
            .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                dialog?.dismiss()
            }.create().show()
    }

    /**
     * 移除一个[ClassTime]。
     *
     * @param classTime 要移除的对象。
     * @return 如果成功移除则返回true，否则false。
     */
    internal fun removeClassTime(classTime: ClassTime): Boolean =
        if (viewModel.classTimes.size < 2) {
            Toast.makeText(
                this,
                getString(R.string.activity_EditCourse_NoMoreClassTimeObjectToast),
                Toast.LENGTH_SHORT
            ).show()
            false
        } else if (!viewModel.classTimes.contains(classTime)) {
            false
        } else {
            // 滚动到被删除项的前一个
            val index = viewModel.classTimes.indexOf(classTime)
            if (index > -1) {
                viewModel.classTimes.remove(classTime)
                this.adapter.notifyItemRemoved(index)
                if (classTime.id != null) {
                    // 当id为空时则说明该对象还未插入数据库
                    viewModel.removedClassTimes.add(classTime)
                }
            }

            true
        }
}