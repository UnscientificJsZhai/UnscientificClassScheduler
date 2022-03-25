package com.github.unscientificjszhai.unscientficclassscheduler.ui.others

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 支持ContextMenu的RecyclerView。
 *
 * @author UnscientificJsZhai
 */
class RecyclerViewWithContextMenu : RecyclerView {

    /**
     * 用于传递位置数据的MenuInfo。
     */
    class PositionMenuInfo(var position: Int) : ContextMenu.ContextMenuInfo

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    private val contextInfo = PositionMenuInfo(-1)

    override fun showContextMenuForChild(originalView: View, x: Float, y: Float): Boolean {
        if (layoutManager != null) {
            val position = layoutManager!!.getPosition(originalView)
            this.contextInfo.position = position
        }
        return super.showContextMenuForChild(originalView, x, y)
    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return this.contextInfo
    }
}