package com.github.unscientificjszhai.unscientificclassscheduler.ui.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime

/**
 * 给周数选择器使用的Dialog中的RecyclerView的适配器。
 * 实现了单选多选功能。
 *
 * @param weekData 核心数据源。
 * @param items 每个选项的标题，RecyclerView的长度由它决定。
 * @see ClassTime
 * @author UnscientificJsZhai
 */
class WeekChooseDialogAdapter(
    private val weekData: ClassTime,
    private val items: Array<String>
) : RecyclerView.Adapter<WeekChooseDialogAdapter.ViewHolder>() {

    inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val checkBox: CheckBox = rootView.findViewById(R.id.WeekChooseRecycler_CheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_week_choose_dialog, parent, false)
        val holder = ViewHolder(view)

        // 短按实现单项选择
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            weekData.setWeekData(holder.bindingAdapterPosition + 1, isChecked)
        }

        // 长按实现多选
        holder.checkBox.setOnLongClickListener {
            var position = holder.bindingAdapterPosition

            do {
                if (!weekData.getWeekData(position + 1)) {
                    weekData.setWeekData(position + 1, true)
                    notifyItemChanged(position)
                    position -= 1
                } else {
                    break
                }
            } while (position >= 0)

            true
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.checkBox.isChecked = weekData.getWeekData(position + 1)
        holder.checkBox.text = items[position]
    }

    override fun getItemCount(): Int = items.size
}