package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientificclassscheduler.ui.editor.EditCourseActivity
import com.github.unscientificjszhai.unscientificclassscheduler.util.getWeekDescriptionString
import com.github.unscientificjszhai.unscientificclassscheduler.util.jumpToSystemPermissionSettings
import com.github.unscientificjszhai.unscientificclassscheduler.util.runIfPermissionGranted
import com.github.unscientificjszhai.unscientificclassscheduler.util.setSystemUIAppearance
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog形式的Activity。用于在[MainActivity]中点击一个项目的时候显示它的详情。
 * 传递进来的Intent中的可序列化Extra应该是[CourseWithClassTimes]类型的。
 *
 * @see MainActivity
 * @see Course
 * @author UnscientificJsZhai
 */
@AndroidEntryPoint
class CourseDetailActivity : AppCompatActivity() {

    companion object {

        /**
         * 启动此Activity的通用方法。如果context是[MainActivity]的话，则会启动下面的方法。
         *
         * @param context 上下文。
         * @param courseId 数据对象。
         * @param option 动画数据。
         */
        @JvmStatic
        @JvmOverloads
        fun startThisActivity(context: Context, courseId: Long, option: Bundle? = null) {
            val intent = Intent(context, CourseDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_COURSE, courseId)
            ActivityCompat.startActivity(context, intent, option)
        }

        /**
         * 启动此Activity时随Intent传入了一个整型Extra，其Key为此值。
         */
        const val INTENT_EXTRA_COURSE = "courseID"

        /**
         * 在[EditCourseActivity]中如果修改了Course对象，不仅会体现在数据库中，
         * 而且会通过Intent将修改后的对象传递回来。其Key为此值。
         */
        const val EDIT_INTENT_RESULT = "courseResult"

        /**
         * 此Activity在[onActivityResult]方法中接收[EditCourseActivity]返回的修改后对象时，requestCode为此值。
         */
        const val EDIT_REQUEST_CODE = 4
    }

    @Inject
    lateinit var courseDao: CourseDao

    @Inject
    lateinit var courseDeleter: CourseDeleter

    private val viewModel: CourseDetailActivityViewModel by viewModels()

    private lateinit var schedulerApplication: SchedulerApplication

    private lateinit var descriptionTextView: TextView
    private lateinit var timeDescriptionTextView: TextView
    private lateinit var remarkTextView: TextView

    private var delete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)
        this.schedulerApplication = application as SchedulerApplication

        setSystemUIAppearance(this)

        this.descriptionTextView = findViewById(R.id.CourseDetailActivity_DescriptionText)
        this.timeDescriptionTextView = findViewById(R.id.CourseDetailActivity_TimeDescriptionText)
        this.remarkTextView = findViewById(R.id.CourseDetailActivity_RemarkText)

        val id = intent.getLongExtra(INTENT_EXTRA_COURSE, -1)
        try {
            this.viewModel.setCurrentCourseId(id)
        } catch (e: IllegalArgumentException) {
            // 数据异常退出Activity
            Toast.makeText(this, R.string.activity_CourseDetail_DataError, Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        // 监听数据变更
        val courseTable by schedulerApplication
        viewModel.courseDetail.observe(this) { courseWithClassTimes ->
            if (!Course.checkLegitimacy(courseWithClassTimes, courseTable)) {
                if (!delete) {
                    Toast.makeText(
                        this,
                        getText(R.string.activity_CourseDetail_LoadingErrorToast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish()
            } else {
                findViewById<TextView>(R.id.CourseDetailActivity_Title).text =
                    courseWithClassTimes.course.title
                descriptionTextView.text =
                    courseWithClassTimes.descriptionText()
                timeDescriptionTextView.text =
                    courseWithClassTimes.timeDescriptionText()
                remarkTextView.text =
                    courseWithClassTimes.remarkText()
            }
        }

        // 定义编辑按钮
        findViewById<Button>(R.id.CourseDetailActivity_EditButton).setOnClickListener {
            val courseWithClassTimes = viewModel.courseDetail.value
            if (courseWithClassTimes != null) {
                EditCourseActivity.startThisActivity(this, courseWithClassTimes)
            } else {
                Toast.makeText(this, R.string.activity_CourseDetail_DataError, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }

        // 定义删除按钮
        findViewById<Button>(R.id.CourseDetailActivity_DeleteButton).setOnClickListener {

            AlertDialog.Builder(this, R.style.Theme_TimeManager_AlertDialog)
                .setTitle(R.string.activity_CourseDetail_DeleteConfirm)
                .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                    dialog?.dismiss()
                }
                .setPositiveButton(R.string.common_confirm) { dialog, _ ->

                    runIfPermissionGranted(Manifest.permission.WRITE_CALENDAR, {
                        dialog.dismiss()
                        AlertDialog.Builder(this)
                            .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                            .setMessage(R.string.activity_CourseDetail_AskPermissionText)
                            .setNegativeButton(R.string.common_cancel) { permissionDialog, _ ->
                                // 拒绝授予日历权限。
                                permissionDialog.dismiss()
                            }.setPositiveButton(R.string.common_confirm) { permissionDialog, _ ->
                                // 同意授予日历权限，跳转到系统设置进行授权。
                                this@CourseDetailActivity.jumpToSystemPermissionSettings()
                                permissionDialog.dismiss()
                            }
                    }) {

                        val courseWithClassTimes = viewModel.courseDetail.value

                        if (courseWithClassTimes != null) {
                                  delete = true
                            viewModel.viewModelScope.launch {
                                viewModel.deleteCourse(
                                    this@CourseDetailActivity,
                                    courseWithClassTimes,
                                    schedulerApplication.useCalendar
                                )
                            }
                        } else {
                            Toast.makeText(
                                this,
                                R.string.activity_CourseDetail_DataError,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                        finish()
                    }
                }.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    // 以下为文字格式化方法。

    /**
     * 生成第一行描述性文字。
     *
     * @return 描述学分和上课周数的文字。
     */
    private fun CourseWithClassTimes.descriptionText(): String {
        val stringBuilder = StringBuilder()

        if (course.credit != 0.0) {
            if (course.credit % 1 == 0.0) {
                stringBuilder.append(course.credit.toInt())
            } else {
                stringBuilder.append(course.credit)
            }
            stringBuilder.append(getString(R.string.activity_EditCourse_Credit))
            stringBuilder.append(" ")
        }

        val courseTable by schedulerApplication
        var veryStart = courseTable.maxWeeks
        var veryEnd = 1
        for (classTime in classTimes) {
            for (index in 1 until veryStart) {
                if (classTime.getWeekData(index)) {
                    veryStart = index
                    break
                }
            }
            for (index in veryEnd..courseTable.maxWeeks) {
                if (classTime.getWeekData(index)) {
                    veryEnd = index
                }
            }
        }
        stringBuilder.append(
            getString(R.string.activity_CourseDetail_WeekFormatText)
                .format(veryStart, veryEnd)
        )

        return stringBuilder.toString()
    }

    /**
     * 生成第二行的描述性文字。
     *
     * @return 每个上课时间段的描述。每个[ClassTime]对象各占用一行。
     */
    private fun CourseWithClassTimes.timeDescriptionText(): String {
        val stringBuilder = StringBuilder()
        val courseTable by schedulerApplication

        this.classTimes.forEach { classTime: ClassTime ->
            stringBuilder.append(
                classTime.getWeekDescriptionString(
                    getString(R.string.view_ClassTimeEdit_WeekDescription),
                    getString(R.string.activity_CourseDetail_LoadingErrorToast),
                    courseTable.maxWeeks
                )
            )

            stringBuilder.append(" ")

            stringBuilder.append(
                getString(
                    when (classTime.whichDay) {
                        1 -> R.string.data_Week1
                        2 -> R.string.data_Week2
                        3 -> R.string.data_Week3
                        4 -> R.string.data_Week4
                        5 -> R.string.data_Week5
                        6 -> R.string.data_Week6
                        else -> R.string.data_Week0
                    }
                )
            )

            stringBuilder.append(" ")

            stringBuilder.append(
                getString(R.string.activity_CourseDetail_LessonNumberFormatText)
                    .format(
                        if (classTime.start == classTime.end) {
                            classTime.start.toString()
                        } else {
                            "${classTime.start}-${classTime.end}"
                        }
                    )
            )

            if (classTime.location.isNotBlank()) {
                stringBuilder.append(" ${classTime.location}")
            }

            if (classTimes.indexOf(classTime) != classTimes.size - 1) {
                stringBuilder.append("\n")
            }
        }

        return stringBuilder.toString()
    }

    /**
     * 生成第三行的描述性文字。
     *
     * @return 显示备注信息。
     */
    private fun CourseWithClassTimes.remarkText(): String {
        return course.remarks
    }
}