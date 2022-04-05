package com.github.unscientificjszhai.unscientificclassscheduler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.ClassTimeDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable

/**
 * Course对象的数据库。文件名为table.db。
 *
 * @author UnscientificJsZhai
 */
@Database(
    entities = [Course::class, ClassTime::class, CourseTable::class],
    version = SchedulerApplication.COURSE_DATABASE_VERSION
)
abstract class CourseDatabase : RoomDatabase() {

    /**
     * 获取操作课程表数据库的Dao对象。
     *
     * @return 由Room提供的Dao接口实现对象。
     */
    abstract fun courseTableDao(): CourseTableDao

    /**
     * 获取操作课程数据库的Dao对象。
     *
     * @return 由Room提供的Dao接口实现对象。
     */
    abstract fun courseDao(): CourseDao

    /**
     * 获取操作上课时间数据库的Dao对象。
     *
     * @return 由Room提供的Dao接口实现对象。
     */
    abstract fun classTimeDao(): ClassTimeDao
}