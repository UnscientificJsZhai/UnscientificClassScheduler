package com.github.unscientificjszhai.unscientificclassscheduler.di

import android.content.Context
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * 直接提供Application对象的依赖注入模块。
 *
 * @see SchedulerApplication
 * @author UnscientificJsZhai
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    /**
     * 提供Application。返回值直接是具体实现类。
     *
     * @param context Application级Context。
     * @return 转换为实现类的Application对象。
     */
    @Provides
    fun providesApplication(@ApplicationContext context: Context) = context as SchedulerApplication
}