package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.CourseTable

/**
 * [CurrentTableSelectorActivity]中的RecyclerView的Adapter。
 *
 * @param nowTableId 当前的Table的id。
 * @param setTable 一个选中、删除当前表的方法。此方法会在UI线程中执行。
 * @see CurrentTableSelectorActivity
 * @author UnscientificJsZhai
 */
class CurrentTableSelectorAdapter(
    private var nowTableId: Long,
    val setTable: (CourseTable, Boolean) -> Unit
) :
    ListAdapter<CourseTable, CurrentTableSelectorAdapter.ViewHolder>(CourseTableDiffCallback) {

    private object CourseTableDiffCallback : DiffUtil.ItemCallback<CourseTable>() {

        override fun areItemsTheSame(oldItem: CourseTable, newItem: CourseTable) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CourseTable, newItem: CourseTable) =
            false
    }

    inner class ViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView) {
        val textView: TextView = rootView.findViewById(R.id.CurrentTableSelector_TitleText)
        val mark: ImageView = rootView.findViewById(R.id.CurrentTableSelector_SelectedMark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_current_table_selector, parent, false)

        val viewHolder = ViewHolder(view)

        view.setOnClickListener {
            val courseTable = getItem(viewHolder.bindingAdapterPosition)
            setTable(courseTable, false)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val courseTable = getItem(position)
        holder.textView.text = courseTable.name
        holder.mark.visibility = if (courseTable.id == nowTableId) {
            holder.rootView.run {
                contentDescription =
                    courseTable.name + context.getString(R.string.activity_CurrentTableSelector_SelectedDescription)
            }
            View.VISIBLE
        } else {
            holder.rootView.contentDescription = courseTable.name
            View.GONE
        }
    }
}