@file:JvmName("RecyclerScrollHelper")

package com.github.unscientificjszhai.unscientificclassscheduler.util

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/**
 * 使参数中的RecyclerView滚动到指定位置。RecyclerView必须使用LinearLayoutManager作为LayoutManager。
 *
 * @receiver 要滚动的RecyclerView。
 * @param position 目标位置。
 * @author UnscientificJsZhai
 */
private fun RecyclerView.scrollToGivenPosition(position: Int) {
    val manager = this.layoutManager
    if (manager is LinearLayoutManager) {
        val scroller = LinearSmoothScroller(this.context)

        scroller.targetPosition = position
        manager.startSmoothScroll(scroller)
    }
}

/**
 * 使参数中的RecyclerView滚动到底部。
 *
 * @receiver 要滚动的RecyclerView。
 * @author UnscientificJsZhai
 */
fun RecyclerView.scrollToBottom() {
    this.adapter?.let {
        val position = it.itemCount
        scrollToGivenPosition(position)
    }
}