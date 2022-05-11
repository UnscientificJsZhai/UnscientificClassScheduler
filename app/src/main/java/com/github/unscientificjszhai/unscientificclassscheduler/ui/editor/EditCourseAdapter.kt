package com.github.unscientificjszhai.unscientificclassscheduler.ui.editor

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.ClassTime
import com.github.unscientificjszhai.unscientificclassscheduler.ui.others.StaticViewHeaderAdapter
import com.github.unscientificjszhai.unscientificclassscheduler.util.clickVibration
import com.github.unscientificjszhai.unscientificclassscheduler.util.getWeekDescriptionString

/**
 * ClassTime编辑器的适配器。
 *
 * @param classTimes 被编辑的所有ClassTime的ArrayList。
 * @param maxWeeks 最大周数。
 * @see EditCourseActivity
 * @author UnscientificJsZhai
 */
class EditCourseAdapter(
    private val classTimes: ArrayList<ClassTime>,
    private val maxWeeks: Int
) : RecyclerView.Adapter<EditCourseAdapter.ViewHolder>() {

    inner class ViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView) {

        lateinit var classTime: ClassTime

        val weekChooseText: TextView = rootView.findViewById(R.id.ClassTimeEditView_WeekText)
        val dayText: TextView = rootView.findViewById(R.id.ClassTimeEditView_DayText)
        val daySeekBar: SeekBar = rootView.findViewById(R.id.ClassTimeEditView_DaySeekBar)
        val startEditText: EditText = rootView.findViewById(R.id.ClassTimeEditView_FromEditText)
        val endEditText: EditText = rootView.findViewById(R.id.ClassTimeEditView_ToEditText)
        val teacherEditText: EditText =
            rootView.findViewById(R.id.ClassTimeEditView_TeacherNameEditText)
        val locationEditText: EditText =
            rootView.findViewById(R.id.ClassTimeEditView_LocationEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.recycler_class_time_editor, parent, false)
        val holder = ViewHolder(view)

        // 初始化横向选择条
        holder.apply {
            daySeekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        dayText.text = when (progress) {
                            1 -> context.getString(R.string.data_Week1)
                            2 -> context.getString(R.string.data_Week2)
                            3 -> context.getString(R.string.data_Week3)
                            4 -> context.getString(R.string.data_Week4)
                            5 -> context.getString(R.string.data_Week5)
                            6 -> context.getString(R.string.data_Week6)
                            else -> context.getString(R.string.data_Week0)
                        }

                        // 更新控件描述
                        seekBar?.run {
                            contentDescription =
                                this.context.getString(R.string.view_ClassTimeEdit_SeekBarDescription)
                                    .format(dayText.text)
                        }

                        //创建振动效果
                        if (fromUser) {
                            clickVibration(context)
                        }

                        holder.classTime.whichDay = progress
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }

        // 初始化删除按钮
        view.findViewById<ImageButton>(R.id.ClassTimeEditView_Cancel).setOnClickListener {
            if (context is EditCourseActivity) {
                context.removeClassTime(holder.classTime)
            }
        }

        // 设置启动周数选择器的方法。
        holder.weekChooseText.setOnClickListener {
            val length = this.maxWeeks
            Log.d("WeekText", "tapped")

            val items: Array<String> =
                Array(length) { item ->
                    context.getString(R.string.view_ClassTimeEdit_WeekItem_ForKotlin)
                        .format(item + 1)//索引对齐
                }
            val weekData = holder.classTime.copy()

            val builder =
                AlertDialog.Builder(context).setIcon(R.drawable.baseline_calendar_view_month_20)
                    .setTitle(R.string.view_ClassTimeEdit_WeekChooseDialogTitle)
            // 初始化Dialog中的RecyclerView
            val recyclerView = RecyclerView(builder.context)
            recyclerView.layoutManager = LinearLayoutManager(builder.context)
            recyclerView.adapter = ConcatAdapter(
                StaticViewHeaderAdapter(R.layout.widget_week_choose_dialig_header),
                WeekChooseDialogAdapter(weekData, items)
            )

            builder.setView(recyclerView)

            // 定义Dialog的2个按键
            builder.setPositiveButton(
                R.string.common_confirm
            ) { dialog, _ ->
                holder.classTime.week = weekData.week
                holder.weekChooseText.text = holder.classTime.getWeekDescriptionString(
                    context.getString(R.string.view_ClassTimeEdit_WeekDescription),
                    holder.weekChooseText.context.getString(R.string.view_ClassTimeEdit_WeekDescriptionWhenEmpty),
                    this.maxWeeks
                )
                dialog?.dismiss()
            }.setNegativeButton(
                R.string.common_cancel
            ) { dialog, _ -> dialog?.dismiss() }

            builder.create().show()
        }

        // 初始化所有EditText
        holder.apply {
            startEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    try {
                        classTime.start = startEditText.text.toString().toInt()
                    } catch (e: NumberFormatException) {
                        classTime.start = -1
                        startEditText.setText("")
                    }
                }
            }

            endEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    try {
                        classTime.end = endEditText.text.toString().toInt()
                    } catch (e: NumberFormatException) {
                        classTime.end = -1
                        endEditText.setText("")
                    }
                }
            }

            teacherEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    classTime.teacherName = teacherEditText.text.toString()
                }
            }

            locationEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    classTime.location = locationEditText.text.toString()
                }
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val classTime = classTimes[position]
        holder.classTime = classTime

        holder.weekChooseText.text = classTime.getWeekDescriptionString(
            holder.weekChooseText.context.getString(R.string.view_ClassTimeEdit_WeekDescription),
            holder.weekChooseText.context.getString(R.string.view_ClassTimeEdit_WeekDescriptionWhenEmpty),
            this.maxWeeks
        )

        holder.startEditText.setText(
            if (classTime.start > 0) {
                classTime.start.toString()
            } else {
                ""
            }
        )
        holder.endEditText.setText(
            if (classTime.end > 0) {
                classTime.end.toString()
            } else {
                ""
            }
        )

        holder.daySeekBar.progress = classTime.whichDay

        holder.teacherEditText.setText(classTime.teacherName)
        holder.locationEditText.setText(classTime.location)

        holder.rootView.run {
            contentDescription = context.getString(R.string.view_ClassTimeEdit_ContentDescription)
                .format(position + 1, itemCount)
        }

        holder.startEditText.run {
            contentDescription =
                context.getString(R.string.view_ClassTimeEdit_FromEditTextDescription)
                    .format(holder.classTime.start)
        }

        holder.endEditText.run {
            contentDescription =
                context.getString(R.string.view_ClassTimeEdit_ToEditTextDescription)
                    .format(holder.classTime.end)
        }
    }

    override fun getItemCount() = classTimes.size
}