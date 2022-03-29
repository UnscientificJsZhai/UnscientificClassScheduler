package com.github.unscientificjszhai.unscientificclassscheduler.ui.others

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 用于[ConcatAdapter]的头部对象的适配器。为ConcatAdapter提供一个简单的头对象。
 *
 * @param layoutRes 头部对象的布局文件。
 * @param listener 点击监听器。会添加给布局文件的根元素。默认为null。传入null时也不会添加监听器。
 * @author UnscientificJsZhai
 */
class StaticViewHeaderAdapter(
    @LayoutRes private val layoutRes: Int,
    private val listener: View.OnClickListener? = null
) : RecyclerView.Adapter<StaticViewHeaderAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)

        if (this.listener != null) {
            view.setOnClickListener(listener)
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun getItemCount() = 1
}