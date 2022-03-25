package com.github.unscientificjszhai.unscientficclassscheduler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.ClassTimeDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable

/**
 * Course对象的数据库。文件名为table1.db，其中1的位置应该为这个数据库文件对应的[CourseTable]对象的id。
 *
 * @author UnscientificJsZhai
 */
@Database(
    entities = [Course::class, ClassTime::class],
    version = TimeManagerApplication.COURSE_DATABASE_VERSION
)
abstract class CourseDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun classTimeDao(): ClassTimeDao
}