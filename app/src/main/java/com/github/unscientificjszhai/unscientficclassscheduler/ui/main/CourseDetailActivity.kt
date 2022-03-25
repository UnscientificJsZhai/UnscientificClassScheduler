package com.github.unscientificjszhai.unscientficclassscheduler.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientficclassscheduler.ui.editor.EditCourseActivity
import com.github.unscientificjszhai.unscientficclassscheduler.util.getWeekDescriptionString
import com.github.unscientificjszhai.unscientficclassscheduler.util.jumpToSystemPermissionSettings
import com.github.unscientificjszhai.unscientficclassscheduler.util.runIfPermissionGranted
import com.github.unscientificjszhai.unscientficclassscheduler.util.setSystemUIAppearance
import kotlinx.coroutines.launch

/**
 * Dialog形式的Activity。用于在[MainActivity]中点击一个项目的时候显示它的详情。
 * 传递进来的Intent中的可序列化Extra应该是[CourseWithClassTimes]类型的。
 *
 * @see MainActivity
 * @see Course
 * @author UnscientificJsZhai
 */
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
        fun startThisActivity(context: Context, courseId: Long, option: Bundle?) {
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

    private lateinit var timeManagerApplication: TimeManagerApplication

    private lateinit var courseDao: CourseDao

    private lateinit var viewModel: CourseDetailActivityViewModel


    private lateinit var descriptionTextView: TextView
    private lateinit var timeDescriptionTextView: TextView
    private lateinit var remarkTextView: TextView

    private var delete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        setSystemUIAppearance(this)

        this.timeManagerApplication = application as TimeManagerApplication
        this.courseDao = this.timeManagerApplication.getCourseDatabase().courseDao()

        this.descriptionTextView = findViewById(R.id.CourseDetailActivity_DescriptionText)
        this.timeDescriptionTextView = findViewById(R.id.CourseDetailActivity_TimeDescriptionText)
        this.remarkTextView = findViewById(R.id.CourseDetailActivity_RemarkText)

        val id = intent.getLongExtra(INTENT_EXTRA_COURSE, -1)
        val courseWithClassTimesLiveData = courseDao.getLiveCourse(id)
        if (courseWithClassTimesLiveData == null) {
            // 数据异常退出Activity
            Toast.makeText(this, R.string.activity_CourseDetail_DataError, Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }


        this.viewModel = ViewModelProvider(
            this,
            CourseDetailActivityViewModel.Factory(courseWithClassTimesLiveData)
        )[CourseDetailActivityViewModel::class.java]

        // 监听数据变更
        val courseTable by timeManagerApplication
        viewModel.courseWithClassTimes.observe(this) { courseWithClassTimes ->
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
            val courseWithClassTimes = viewModel.courseWithClassTimes.value
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

                        val courseWithClassTimes = viewModel.courseWithClassTimes.value

                        if (courseWithClassTimes != null) {
                            delete = true
                            viewModel.viewModelScope.launch {
                                MainActivityViewModel.deleteCourse(
                                    this@CourseDetailActivity,
                                    courseWithClassTimes,
                                    timeManagerApplication.useCalendar
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
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish()
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

        val courseTable by timeManagerApplication
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
        val courseTable by timeManagerApplication

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