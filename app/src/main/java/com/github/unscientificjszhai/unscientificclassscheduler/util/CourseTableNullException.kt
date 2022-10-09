package com.github.unscientificjszhai.unscientificclassscheduler.util

/**
 * 当需要查询当前课程表且Application中保存的当前课程表为空时抛出此异常。
 *
 * @author UnscientificJsZhai
 */
class CourseTableNullException :
    IllegalStateException("Current CourseTable is null in Application")