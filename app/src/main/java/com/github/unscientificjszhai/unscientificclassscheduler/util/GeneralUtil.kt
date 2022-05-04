@file:JvmName("Util")

package com.github.unscientificjszhai.unscientificclassscheduler.util

/**
 * 对若干对象依次执行操作。
 *
 * @param elements 要执行操作的若干个对象。
 * @param action 要执行的操作。
 */
inline fun <T> forEachIn(vararg elements: T, action: (T) -> Unit) = elements.forEach(action)