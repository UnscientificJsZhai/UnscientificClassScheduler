package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import kotlin.reflect.KProperty

/**
 * CourseDetailActivity的ViewModel。
 *
 * @param courseWithClassTimes 从RoomDatabase中获取的特定课程的LiveData对象。
 * @see CourseDetailActivity
 * @author UnscientificJsZhai
 */
class CourseDetailActivityViewModel(var courseWithClassTimes: LiveData<CourseWithClassTimes>) :
    ViewModel() {

    /**
     * 创建MainActivity的ViewModel的Factory。
     *
     * @param courseWithClassTimes 用于初始化ViewModel的构造函数的参数。
     */
    class Factory(private val courseWithClassTimes: LiveData<CourseWithClassTimes>) :
        ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseDetailActivityViewModel(courseWithClassTimes) as T
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<CourseWithClassTimes> {
        return this.courseWithClassTimes
    }
}