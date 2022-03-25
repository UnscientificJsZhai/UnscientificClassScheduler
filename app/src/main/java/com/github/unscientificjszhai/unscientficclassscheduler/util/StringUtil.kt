@file:JvmName("StringUtil")

package com.github.unscientificjszhai.unscientficclassscheduler.util

import com.github.unscientificjszhai.unscientficclassscheduler.data.course.ClassTime

/**
 * 获取周数选择的描述字符串。
 *
 * @receiver 上课时间数据对象。
 * @param template 模板，包含"%s"用于格式化。
 * @param returnWhenEmpty 当没有选中任何一周时，返回的文字。
 * @param rangeTo 最大范围，等于当前课程表定义的最大周数。
 * @return 格式化后的周数描述字符串。
 * @author UnscientificJsZhai
 */
internal fun ClassTime.getWeekDescriptionString(
    template: String,
    returnWhenEmpty: String,
    rangeTo: Int
): String {
    if (this.week == 0) {
        return returnWhenEmpty
    } else {
        val stringBuilder = StringBuilder()

        var index = 1
        do {
            if (getWeekData(index)) {
                //如果非空则添加逗号分割
                if (stringBuilder.isNotBlank()) {
                    stringBuilder.append(",")
                }

                //首先添加当前指向的周数
                stringBuilder.append(index.toString())
                //二级循环查找连续的值
                if (index < rangeTo) {
                    for (subIndex in index + 1..rangeTo) {
                        if (!getWeekData(subIndex)) {
                            if (subIndex > index + 1) {
                                stringBuilder.append("-${subIndex - 1}")
                            }
                            index = subIndex + 1
                            break
                        } else if (subIndex >= rangeTo) {
                            stringBuilder.append("-${subIndex}")
                            index = subIndex + 1
                        }
                    }
                } else if (index == rangeTo) {
                    break
                }
            } else {
                index += 1
            }
        } while (index <= rangeTo)

        return template.format(stringBuilder.toString())
    }
}

/**
 * 给一位或两位整数统一成两位数的字符形式。
 *
 * @receiver 一个整型数，这个数应该大于0小于100。
 * @return 如果输入值为一位数，则为0x，如果是两位数，就是xx。如果是负数或者超过99的数，则为空字符串。
 * @author UnscientificJsZhai
 */
internal fun Int.with0() = when {
    this < 0 -> ""
    this <= 9 -> "0$this"
    this > 99 -> ""
    else -> this.toString()
}