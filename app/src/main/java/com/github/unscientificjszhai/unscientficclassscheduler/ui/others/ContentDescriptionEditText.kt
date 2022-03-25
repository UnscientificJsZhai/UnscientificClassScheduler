package com.github.unscientificjszhai.unscientficclassscheduler.ui.others

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.widget.AppCompatEditText

/**
 * 无障碍使用时，会朗读ContentDescription而不是Hint的EditText。
 *
 * 通过调用[setContentDescription]方法传入ContentDescription，可以使其在无障碍使用时被朗读出指定的内容。
 * 默认的EditText会朗读Hint的内容。
 *
 * @author UnscientificJsZhai
 */
class ContentDescriptionEditText : AppCompatEditText {

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.text = contentDescription
    }
}