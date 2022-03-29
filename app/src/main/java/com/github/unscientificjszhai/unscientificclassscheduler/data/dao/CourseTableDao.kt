package com.github.unscientificjszhai.unscientificclassscheduler.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable

/**
 * 查询[CourseTable]的Dao接口。
 *
 * @author UnscientificJsZhai
 */
@Dao
interface CourseTableDao {

    /**
     * 查询所有的CourseTable对象。
     *
     * @return 一个列表对象
     */
    @Query("SELECT * FROM ${CourseTable.TABLE_NAME}")
    fun getCourseTables(): List<CourseTable>

    /**
     * 查询特定id的CourseTable对象。
     *
     * @param id 目标CourseTable对象的ID。
     * @return 具有给定id的CourseTable对象。
     */
    @Query("SELECT * FROM ${CourseTable.TABLE_NAME} WHERE id = :id")
    fun getCourseTable(id: Long): CourseTable?

    /**
     * 向数据库中插入CourseTable对象。
     *
     * @param courseTable 需要插入的Course对象。
     */
    @Insert
    fun insertCourseTable(courseTable: CourseTable): Long

    /**
     * 更新一个CourseTable对象。
     *
     * @param courseTable 要更新的CourseTable对象。
     */
    @Update
    fun updateCourseTable(courseTable: CourseTable)

    /**
     * 删除一个CourseTable对象。但是没有删除对应的数据库文件，还需要手动删除。
     *
     * @param courseTable 要删除的CourseTable对象。
     */
    @Delete
    fun deleteCourseTable(courseTable: CourseTable)

    /**
     * 获取一个异步查询LiveData对象。
     *
     * @return 获取所有CourseTable对象列表的LiveData对象。
     */
    @Query("SELECT * FROM ${CourseTable.TABLE_NAME}")
    @Transaction
    fun getLiveCourseTables(): LiveData<List<CourseTable>>
}