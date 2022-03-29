package com.github.unscientificjszhai.unscientficclassscheduler.di

import android.content.Context
import androidx.room.Room
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.ClassTimeDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.database.CourseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 用于数据库依赖注入的模块。
 *
 * @author UnscientificJsZhai
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供数据库对象。
     *
     * @param context 应用级context对象。
     * @return 数据库对象。
     */
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): CourseDatabase =
        Room.databaseBuilder(
            context,
            CourseDatabase::class.java,
            "table.db"
        ).build()

    /**
     * 提供访问课程表信息的Dao对象。
     *
     * @param database 数据库对象。
     * @return Dao接口实现对象。
     */
    @Provides
    @Singleton
    fun providesCourseTableDao(database: CourseDatabase): CourseTableDao = database.courseTableDao()

    /**
     * 提供访问课程信息的Dao对象。
     *
     * @param database 数据库对象。
     * @return Dao接口实现对象。
     */
    @Provides
    @Singleton
    fun providesCourseDao(database: CourseDatabase): CourseDao = database.courseDao()

    /**
     * 提供访问课程信息的Dao对象。
     *
     * @param database 数据库对象。
     * @return Dao接口实现对象。
     */
    @Provides
    @Singleton
    fun providesClassTimeDao(database: CourseDatabase): ClassTimeDao = database.classTimeDao()
}