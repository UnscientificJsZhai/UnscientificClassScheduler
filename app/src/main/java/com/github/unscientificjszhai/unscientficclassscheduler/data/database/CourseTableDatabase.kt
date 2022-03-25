package com.github.unscientificjszhai.unscientficclassscheduler.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.dao.CourseTableDao
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.CourseTable

/**
 * CourseTable的数据库对象，文件名为database.db。
 *
 * @author UnscientificJsZhai
 */
@Database(
    entities = [CourseTable::class],
    version = TimeManagerApplication.TABLE_DATABASE_VERSION
)
abstract class CourseTableDatabase : RoomDatabase() {

    abstract fun courseTableDao(): CourseTableDao

    companion object {

        /**
         * 添加了每周开始日的列。boolean类型。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ${CourseTable.TABLE_NAME} ADD COLUMN week_start INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}