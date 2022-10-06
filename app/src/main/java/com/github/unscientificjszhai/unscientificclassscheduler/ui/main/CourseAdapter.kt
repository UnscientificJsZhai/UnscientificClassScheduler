package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.content.Context
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.data.CurrentTimeMarker
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientificclassscheduler.data.tables.FormattedTime
import com.github.unscientificjszhai.unscientificclassscheduler.util.with0

/**
 * 供主界面的RecyclerView使用的适配器。
 *
 * @see MainActivity
 * @author UnscientificJsZhai
 */
class CourseAdapter(
    private val timeTagger: CurrentTimeMarker,
    private var showTodayOnly: Boolean
) : ListAdapter<CourseWithClassTimes, CourseAdapter.ViewHolder>(CourseDiffCallback) {

    fun setShowTodayOnly(showTodayOnly: Boolean) {
        this.showTodayOnly = showTodayOnly
    }

    /**
     * ListAdapter用于对比数据变化的方法集合，用于CourseAdapter类。
     */
    private object CourseDiffCallback : DiffUtil.ItemCallback<CourseWithClassTimes>() {

        override fun areItemsTheSame(
            oldItem: CourseWithClassTimes,
            newItem: CourseWithClassTimes
        ) = oldItem.course.id == newItem.course.id

        override fun areContentsTheSame(
            oldItem: CourseWithClassTimes,
            newItem: CourseWithClassTimes
        ) = false
    }

    class ViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val titleText: TextView = rootView.findViewById(R.id.CourseWidget_TitleText)
        val informationText: TextView = rootView.findViewById(R.id.CourseWidget_Information)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_course, parent, false)
        val holder = ViewHolder(view)

        view.setOnClickListener {
            val course = getItem(holder.bindingAdapterPosition)

            CourseDetailActivity.startThisActivity(
                view.context,
                course.course.id ?: -1
            )
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val course = this.getItem(position).course
            holder.titleText.text = course.title
            holder.informationText.visibility = View.VISIBLE
            holder.informationText.text =
                generateInformation(holder.informationText.context, course) {
                    holder.informationText.visibility = View.GONE
                }
        } catch (e: NullPointerException) {
            return
        }
    }

    /**
     * 显示当前课程的状态。例如正在上课、即将上课等。
     *
     * @param context 获取字符串内容的上下文。
     * @param course 课程。
     * @param emptyOption 当无内容显示时进行的操作。例如可以将这一栏的TextView可见性设为[View.GONE]。
     * @return 状态文字。
     */
    private fun generateInformation(
        context: Context,
        course: Course,
        emptyOption: () -> Unit
    ): String {
        val stringBuilder = StringBuilder()

        if (showTodayOnly) {
            if (course.specificClassTime == null) {
                // 异常情况，如果设置为只显示今天的话所有的course对象中的这个成员都不是null。
                stringBuilder.append(context.getString(R.string.activity_Main_TodayOnlyMode_DataError))
            } else {
                val classTime = course.specificClassTime!!.get()
                if (classTime != null) {
                    // 上课时间
                    val nowLessonNumber = timeTagger.nowLessonNumber()
                    when {
                        nowLessonNumber >= classTime.start && nowLessonNumber <= classTime.end -> {
                            stringBuilder.append(context.getString(R.string.activity_Main_TodayOnlyMode_During))
                                .append(" ")
                        }
                        nowLessonNumber - classTime.start == -0.5 -> {
                            stringBuilder.append(context.getString(R.string.activity_Main_TodayOnlyMode_Next))
                                .append(" ")
                        }
                        nowLessonNumber - classTime.end > 0 -> {
                            stringBuilder.append(context.getString(R.string.activity_Main_TodayOnlyMode_Finished))
                                .append(" ")
                        }
                    }

                    val formattedTime = classTime.getFormattedTime(timeTagger.courseTable)
                    formattedTime.getTimeDescriptionText(context, stringBuilder)

                    // 上课地点
                    if (classTime.location.isNotBlank()) {
                        stringBuilder.append(" @")
                        stringBuilder.append(classTime.location)
                    }

                    // 老师姓名
                    if (classTime.teacherName.isNotBlank()) {
                        stringBuilder.append(" ")
                        stringBuilder.append(classTime.teacherName)
                    }
                }
            }
        } else {
            var showEmptyMessage = true
            if (course.credit != 0.0) {
                stringBuilder.append(course.credit)
                stringBuilder.append(context.getString(R.string.activity_EditCourse_Credit))
                showEmptyMessage = false
            }
            if (course.remarks.isNotBlank()) {
                stringBuilder.append(" ")
                stringBuilder.append(course.remarks)
                showEmptyMessage = false
            }
            if (showEmptyMessage) {
                emptyOption()
            }
        }

        return stringBuilder.toString()
    }

    /**
     * 把FormattedTime对象格式化成显示在界面上的文本。
     * 不会返回值，需要调用入参的[toString]方法。
     *
     * @param context 获取字符串资源的上下文。
     * @param stringBuilder 传入一个StringBuilder对象用于构建文本。
     */
    private fun FormattedTime.getTimeDescriptionText(
        context: Context,
        stringBuilder: StringBuilder
    ) {

        if (Settings.System.getString(
                context.contentResolver,
                Settings.System.TIME_12_24
            ) != "24"
        ) {
            // 如果使用12小时的话
            val newStartH = if (this.startH > 12) {
                stringBuilder.append(context.getString(R.string.common_time_afternoon))
                this.startH - 12
            } else {
                stringBuilder.append(context.getString(R.string.common_time_noon))
                this.startH
            }
            stringBuilder.append("$newStartH:${this.startM.with0()}")
            val newEndH = if (this.endH > 12) {
                stringBuilder.append(context.getString(R.string.common_time_afternoon))
                this.endH - 12
            } else {
                stringBuilder.append(context.getString(R.string.common_time_noon))
                this.endH
            }
            stringBuilder.append("-$newEndH:${this.endM.with0()}")
        } else {
            stringBuilder.append("${this.startH}:${this.startM.with0()}")
            stringBuilder.append("-${this.endH}:${this.endM.with0()}")
        }
    }
}