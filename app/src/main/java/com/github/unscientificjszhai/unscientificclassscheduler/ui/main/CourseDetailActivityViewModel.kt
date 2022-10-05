package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * CourseDetailActivity的ViewModel。
 *
 * @param courseDao 访问课程表数据库的Dao对象。
 * @param courseDeleter 删除课程表的操作器。
 * @see CourseDetailActivity
 * @since 1.3.2
 * @author UnscientificJsZhai
 */
@HiltViewModel
class CourseDetailActivityViewModel @Inject constructor(
    private val courseDao: CourseDao,
    private val courseDeleter: CourseDeleter
) : ViewModel() {

    private var currentCourse: LiveData<CourseWithClassTimes>? = null

    private val _courseDetail = MediatorLiveData<CourseWithClassTimes>()
    val courseDetail: LiveData<CourseWithClassTimes> get() = this._courseDetail

    /**
     * 设置当前查看的课程的ID。设置后LiveData才会开始更新。
     *
     * @param currentCourseId 课程的ID。
     * @throws IllegalArgumentException 如果课程的ID找不到则会抛出此异常。
     */
    @Throws(IllegalArgumentException::class)
    fun setCurrentCourseId(currentCourseId: Long) {
        val newCourse = courseDao.getLiveCourse(currentCourseId)
        if (newCourse != null) {
            currentCourse?.let {
                _courseDetail.removeSource(it)
            }
            _courseDetail.addSource(newCourse) {
                _courseDetail.value = it
            }
            this.currentCourse = newCourse
        } else {
            throw IllegalArgumentException("Course ID: $currentCourseId not found!")
        }
    }

    /**
     * MainActivity和CourseDetailActivity操作删除课程。
     *
     * @param context 删除操作的上下文。
     * @param courseWithClassTimes 要删除的课程对象。
     * @param useCalendar 是否使用日历功能。
     */
    suspend fun deleteCourse(
        context: Context,
        courseWithClassTimes: CourseWithClassTimes,
        useCalendar: Boolean
    ) {
        courseDeleter.deleteCourse(context, courseWithClassTimes, useCalendar)
    }
}