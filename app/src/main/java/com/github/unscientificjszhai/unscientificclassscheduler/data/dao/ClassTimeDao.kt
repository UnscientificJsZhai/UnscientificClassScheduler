package com.github.unscientificjszhai.unscientificclassscheduler.data.dao

import androidx.room.*
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime

/**
 * 查询[ClassTime]的Dao接口。
 *
 * @author UnscientificJsZhai
 */
@Dao
interface ClassTimeDao {

    /**
     * 查询所有的ClassTime对象。
     *
     * @return 一个列表对象。
     */
    @Query("SELECT * FROM ${ClassTime.TABLE_NAME}")
    fun getClassTimes(): List<ClassTime>

    /**
     * 查询属于给定Course的ClassTIme对象。
     *
     * @param courseId 目标Course对象的ID。
     * @return 一个列表对象。
     */
    @Query("SELECT * FROM ${ClassTime.TABLE_NAME} WHERE course_id = :courseId")
    fun getClassTimes(courseId: Int): List<ClassTime>

    /**
     * 插入一个ClassTime对象到数据库中。
     *
     * @param classTime 要插入的ClassTime对象。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClassTime(classTime: ClassTime): Long

    /**
     * 更新一个ClassTime对象。
     *
     * @param classTime 要更新的ClassTime对象。
     */
    @Update
    fun updateClassTime(vararg classTime: ClassTime)

    /**
     * 删除一个ClassTime对象。
     *
     * @param classTime 要删除的ClassTime对象。
     */
    @Delete
    fun deleteClassTime(vararg classTime: ClassTime)

    /**
     * 删除一组ClassTime对象。
     *
     * @param classTimes 要删除的ClassTime对象的列表。
     */
    @Delete
    fun deleteClassTimes(classTimes: List<ClassTime>)
}