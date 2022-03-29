package com.github.unscientificjszhai.unscientificclassscheduler.ui.editor

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.CourseDetailActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * EditCourseActivity的ViewModel。
 *
 * @see EditCourseActivity
 * @author UnscientificJsZhai
 */
@HiltViewModel
internal class EditCourseActivityViewModel @Inject constructor(
    private val eventsOperator: EventsOperator
) : ViewModel() {

    /**
     * 如果是修改已有Course的话，则不为空。
     */
    var course: Course? = null

    /**
     * 被删除的ClassTime对象。只有删除之前存在的对象（即数据库中存在）时，才将其引用送入此数组。
     */
    val removedClassTimes = ArrayList<ClassTime>()

    /**
     * 目前的ClassTime对象。
     */
    lateinit var classTimes: ArrayList<ClassTime>

    /**
     * 标志classTime列表已经初始化完成的布尔值。
     */
    var classTimesInitialized = false

    /**
     * 是否从上一个复制。
     */
    var copyFromPrevious = true

    /**
     * 保存数据的逻辑。调用时应该已经确定获得了日历写入权限。
     *
     * @param context 执行此次操作的Activity。
     * @param useCalendar 是否使用日历功能。
     */
    suspend fun saveData(context: EditCourseActivity, useCalendar: Boolean) {
        val application = context.application as SchedulerApplication
        withContext(Dispatchers.Default) {
            val courseTable = application.courseTable!!
            // 创建可读取数据库对象
            val courseDatabase = application.getCourseDatabase()
            val courseDao = courseDatabase.courseDao()
            val classTimeDao = courseDatabase.classTimeDao()

            val course = this@EditCourseActivityViewModel.course

            if (course?.title?.isBlank() == true) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.activity_EditCourse_DataError,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext
            }

            when {
                course == null -> {
                    Toast.makeText(
                        context,
                        R.string.activity_EditCourse_DataError,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                course.id == null -> {
                    //新建Course对象时
                    for (classTime in this@EditCourseActivityViewModel.classTimes) {
                        if (!classTime.isLegitimacy(courseTable)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    R.string.activity_EditCourse_DataError,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@withContext
                        }
                    }

                    if (useCalendar) {
                        // 插入日历表
                        val courseWithClassTimes =
                            CourseWithClassTimes(
                                course,
                                this@EditCourseActivityViewModel.classTimes
                            )
                        eventsOperator.addEvent(context, courseTable, courseWithClassTimes)

                        // 正式开始插入
                        try {
                            val courseId = courseDao.insertCourse(course)
                            for (classTime in this@EditCourseActivityViewModel.classTimes) {
                                classTime.courseId = courseId
                                classTimeDao.insertClassTime(classTime)
                            }
                        } catch (e: Exception) {
                            Log.e("EditCourseActivity", "saveData: Can not access Room database")
                            eventsOperator.deleteEvent(context, courseTable, courseWithClassTimes)
                        }
                    }

                    context.finish()
                }
                else -> {
                    // 修改现有Course对象时
                    for (classTime: ClassTime in this@EditCourseActivityViewModel.classTimes) {
                        if (!classTime.isLegitimacy(courseTable)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    R.string.activity_EditCourse_DataError,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@withContext
                        }
                    }

                    //从数据库中删除应该被删除的对象
                    for (removedClassTime: ClassTime in this@EditCourseActivityViewModel.removedClassTimes) {
                        this@EditCourseActivityViewModel.classTimes.removeIf { classTime ->
                            classTime.id == removedClassTime.id
                        }
                        classTimeDao.deleteClassTime(removedClassTime)
                    }

                    if (useCalendar) {
                        //修改日历表
                        val courseWithClassTimes =
                            CourseWithClassTimes(
                                course,
                                this@EditCourseActivityViewModel.classTimes
                            )
                        eventsOperator.updateEvent(context, courseTable, courseWithClassTimes)
                    }

                    //写入数据库的Course表
                    courseDao.updateCourse(course)
                    //写入数据库的ClassTime表
                    for (classTime: ClassTime in this@EditCourseActivityViewModel.classTimes) {
                        if (classTime.courseId == null) {
                            classTime.courseId = course.id
                        }
                        if (classTime.id == null) {
                            classTime.id = classTimeDao.insertClassTime(classTime)
                        } else {
                            classTimeDao.updateClassTime(classTime)
                        }
                    }

                    val intent = Intent()

                    intent.putExtra(
                        CourseDetailActivity.EDIT_INTENT_RESULT,
                        CourseWithClassTimes(course, this@EditCourseActivityViewModel.classTimes)
                    )
                    context.finish()
                }
            }
        }
    }
}