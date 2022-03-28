package com.github.unscientificjszhai.unscientficclassscheduler.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes

/**
 * 查询[Course]的Dao接口。
 *
 * @author UnscientificJsZhai
 */
@Dao
interface CourseDao {

    /**
     * 获取所有的Course对象以及关联的ClassTime对象。
     *
     * @return 返回一个[CourseWithClassTimes]的列表。包含全部课程表的所有课程。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME}")
    @Transaction
    fun getCourses(): List<CourseWithClassTimes>

    /**
     * 获取所有的Course对象以及关联的ClassTime对象。
     *
     * @param tableId 要查询的课程表的ID。
     * @return 返回一个[CourseWithClassTimes]的列表。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE table_id = :tableId")
    @Transaction
    fun getCourses(tableId: Long): List<CourseWithClassTimes>

    /**
     * 获取特定的Course对象以及关联的ClassTime对象。
     *
     * @param id 想要查找的对象的id。
     * @return 查找到的目标对象。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE id = :id")
    @Transaction
    fun getCourse(id: Long): CourseWithClassTimes?

    /**
     * 获取特定的Course对象以及关联的ClassTime对象。
     * 以LiveData的形式返回查询结果。
     *
     * @param id 要查询的课程的ID。不限制它是否属于当前打开的课程表。
     * @return 查询到的课程项目。如果没有查询到给定ID对应的课程项目则返回null。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE id = :id")
    @Transaction
    fun getCourseLiveData(id: Long): LiveData<CourseWithClassTimes>?

    /**
     * 向数据库中插入Course对象，但是不包括关联的ClassTime对象。
     *
     * 当ID冲突时，覆盖旧的项目。
     *
     * @param course 需要插入的Course对象。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCourse(course: Course): Long

    /**
     * 删除一个Course对象。
     *
     * @param course 要删除的Course对象。
     */
    @Delete
    fun deleteCourse(vararg course: Course)

    /**
     * 更新一个Course对象。
     *
     * @param course 要更新的Course对象。
     */
    @Update
    fun updateCourse(vararg course: Course)

    /**
     * 获取全部异步查询LiveData对象。
     *
     * @return 包含全部查询结果的列表LiveData对象。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE table_id = :tableId")
    @Transaction
    fun getLiveCourses(tableId: Long): LiveData<List<CourseWithClassTimes>>

    /**
     * 获取一个异步查询LiveData对象。
     *
     * @param courseId 要查询的课程的ID。不限制它是否属于当前打开的课程表。
     * @return 查询到的课程项目的LiveData对象。如果没有查询到给定ID对应的课程项目则返回null。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE id = :courseId")
    @Transaction
    fun getLiveCourse(courseId: Long): LiveData<CourseWithClassTimes>?
}