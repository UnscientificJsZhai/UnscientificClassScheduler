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
     * @return 返回一个[CourseWithClassTimes]的列表。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME}")
    @Transaction
    fun getCourses(): List<CourseWithClassTimes>

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
     * @param id 想要查找的对象的id。
     * @return 查找到的目标对象。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE id = :id")
    @Transaction
    fun getCourseLiveData(id: Long): LiveData<CourseWithClassTimes>?

    /**
     * 向数据库中插入Course对象，但是不包括关联的ClassTime对象。
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
     * @return 包含全部查询结果的LiveData对象。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME}")
    @Transaction
    fun getLiveCourses(): LiveData<List<CourseWithClassTimes>>

    /**
     * 获取一个异步查询LiveData对象。
     *
     * @return 一个包含了表中所有Course和ClassTime对象的列表的LiveData对象。
     */
    @Query("SELECT * FROM ${Course.TABLE_NAME} WHERE id = :courseId")
    @Transaction
    fun getLiveCourse(courseId: Long): LiveData<CourseWithClassTimes>?
}