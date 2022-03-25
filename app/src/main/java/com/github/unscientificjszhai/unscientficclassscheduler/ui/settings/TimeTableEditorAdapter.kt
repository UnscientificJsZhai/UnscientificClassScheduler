package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.data.tables.FormattedTime

/**
 * 上课时间表中的RecyclerView的适配器。
 *
 * @see TimeTableEditorActivity
 * @author UnscientificJsZhai
 */
internal class TimeTableEditorAdapter(
    private val viewModel: TimeTableEditorActivityViewModel
) :
    RecyclerView.Adapter<TimeTableEditorAdapter.ViewHolder>() {

    inner class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        val titleTextView: TextView = rootView.findViewById(R.id.TimeTableEditorRecycler_TitleText)
        val startTextView: TextView = rootView.findViewById(R.id.TimeTableEditorRecycler_StartTime)
        val endTextView: TextView = rootView.findViewById(R.id.TimeTableEditorRecycler_EndTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_time_table_editor, parent, false)
        val holder = ViewHolder(view)

        val courseTable = viewModel.courseTable

        view.setOnClickListener {
            // 获取计时方法
            val is24HourView = Settings.System.getString(
                parent.context.contentResolver,
                Settings.System.TIME_12_24
            ) == "24"

            val formattedTime = FormattedTime(courseTable.timeTable[holder.bindingAdapterPosition])
            val timePickerDialog = TimePickerDialog(
                parent.context,
                { _, hourOfDay, minute ->
                    formattedTime.startH = hourOfDay
                    formattedTime.startM = minute

                    // 从ViewModel中读取间隔时间
                    formattedTime.autoSetEndTime(viewModel.duration)

                    courseTable.timeTable[holder.bindingAdapterPosition] = formattedTime.toString()
                    this.notifyItemChanged(holder.bindingAdapterPosition)
                },
                formattedTime.startH,
                formattedTime.startM,
                is24HourView
            )
            timePickerDialog.setTitle(R.string.activity_TimeTableEditor_StartTime)
            timePickerDialog.show()
        }
        view.setOnLongClickListener {
            // 获取计时方法
            val is24HourView = Settings.System.getString(
                parent.context.contentResolver,
                Settings.System.TIME_12_24
            ) == "24"

            val formattedTime = FormattedTime(courseTable.timeTable[holder.bindingAdapterPosition])
            val timePickerDialog = TimePickerDialog(
                parent.context,
                { _, hourOfDay, minute ->
                    formattedTime.endH = hourOfDay
                    formattedTime.endM = minute

                    courseTable.timeTable[holder.bindingAdapterPosition] = formattedTime.toString()
                    this.notifyItemChanged(holder.bindingAdapterPosition)
                },
                formattedTime.endH,
                formattedTime.endM,
                is24HourView
            )
            timePickerDialog.setTitle(R.string.activity_TimeTableEditor_EndTime)
            timePickerDialog.show()

            true
        }

        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val formattedTime = FormattedTime(viewModel.courseTable.timeTable[position])

        holder.titleTextView.setText(R.string.activity_TimeTableEditor_RecyclerItemTitle)
        holder.titleTextView.text = holder.titleTextView.text.toString().format(position + 1)
        holder.startTextView.text =
            "${timeNumberFormat(formattedTime.startH)}:${timeNumberFormat(formattedTime.startM)}"
        holder.endTextView.text =
            "${timeNumberFormat(formattedTime.endH)}:${timeNumberFormat(formattedTime.endM)}"
    }

    override fun getItemCount() = viewModel.courseTable.classesPerDay

    /**
     * 格式化整型数为2位字符串。
     *
     * @param number 要格式化的整型数。
     */
    private fun timeNumberFormat(number: Int) =
        if (number < 10) {
            "0$number"
        } else {
            number.toString()
        }
}