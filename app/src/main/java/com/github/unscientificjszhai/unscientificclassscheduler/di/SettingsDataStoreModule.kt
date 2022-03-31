package com.github.unscientificjszhai.unscientificclassscheduler.di

import android.content.Context
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientificclassscheduler.features.calendar.EventsOperator
import com.github.unscientificjszhai.unscientificclassscheduler.ui.settings.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * 为设置Activity提供自定义DataStore的Module。
 *
 * @see SettingsDataStore
 * @author UnscientificJsZhai
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
object SettingsDataStoreModule {

    /**
     * 提供设置DataStore对象。
     *
     * @param context 应用级Context。
     * @param courseTableDao 课程表数据库操作Dao。
     * @param eventsOperator 日历活动操作器。
     * @return 自定义DataStore对象。
     */
    @Provides
    fun providesDataStore(
        @ApplicationContext context: Context,
        courseTableDao: CourseTableDao,
        eventsOperator: EventsOperator
    ) = (context as SchedulerApplication).run {
        val courseTable by this
        SettingsDataStore(
            courseTableDao = courseTableDao,
            context = context,
            nowCourseTable = courseTable,
            eventsOperator = eventsOperator,
            notifyApplicationCourseTableChanged = context::updateTableID
        )
    }
}