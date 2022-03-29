package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
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
internal class CourseAdapter(private val activity: MainActivity) :
    ListAdapter<CourseWithClassTimes, CourseAdapter.ViewHolder>(CourseDiffCallback) {

    /**
     * 当前时间的标记对象，委托给宿主Activity。
     */
    private val timeTagger by activity

    private val courseTable by (activity.application as SchedulerApplication)

    /**
     * 获取宿主Activity的状态，是否只显示今天。
     *
     * @return 是否仅显示今天
     */
    private val showTodayOnly: () -> Boolean = activity::isShowTodayOnly

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
            holder.titleText.transitionName = "CourseTitle"
            val option = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                Pair(holder.titleText, "CourseTitle")
            )

            CourseDetailActivity.startThisActivity(
                view.context,
                course.course.id ?: -1,
                option.toBundle()
            )
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val course = this.getItem(position).course
            holder.titleText.text = course.title
            holder.informationText.visibility = View.VISIBLE
            holder.informationText.text = generateInformation(course) {
                holder.informationText.visibility = View.GONE
            }
        } catch (e: NullPointerException) {
            return
        }
    }

    private fun generateInformation(course: Course, emptyOption: () -> Unit): String {
        val stringBuilder = StringBuilder()

        if (showTodayOnly()) {
            if (course.specificClassTime == null) {
                // 异常情况，如果设置为只显示今天的话所有的course对象中的这个成员都不是null。
                stringBuilder.append(activity.getString(R.string.activity_Main_TodayOnlyMode_DataError))
            } else {
                val classTime = course.specificClassTime!!.get()
                if (classTime != null) {
                    // 上课时间
                    val nowLessonNumber = timeTagger.nowLessonNumber(courseTable)
                    when {
                        nowLessonNumber >= classTime.start && nowLessonNumber <= classTime.end -> {
                            stringBuilder.append(activity.getString(R.string.activity_Main_TodayOnlyMode_During))
                                .append(" ")
                        }
                        nowLessonNumber - classTime.start == -0.5 -> {
                            stringBuilder.append(activity.getString(R.string.activity_Main_TodayOnlyMode_Next))
                                .append(" ")
                        }
                        nowLessonNumber - classTime.end > 0 -> {
                            stringBuilder.append(activity.getString(R.string.activity_Main_TodayOnlyMode_Finished))
                                .append(" ")
                        }
                    }

                    val formattedTime = classTime.getFormattedTime(courseTable)
                    formattedTime.getTimeDescriptionText(stringBuilder)

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
                stringBuilder.append(activity.getString(R.string.activity_EditCourse_Credit))
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
     * @param stringBuilder 传入一个StringBuilder对象用于构建文本。
     */
    private fun FormattedTime.getTimeDescriptionText(stringBuilder: StringBuilder) {

        if (Settings.System.getString(
                activity.contentResolver,
                Settings.System.TIME_12_24
            ) != "24"
        ) {
            // 如果使用12小时的话
            val newStartH = if (this.startH > 12) {
                stringBuilder.append(activity.getString(R.string.common_time_afternoon))
                this.startH - 12
            } else {
                stringBuilder.append(activity.getString(R.string.common_time_noon))
                this.startH
            }
            stringBuilder.append("$newStartH:${this.startM.with0()}")
            val newEndH = if (this.endH > 12) {
                stringBuilder.append(activity.getString(R.string.common_time_afternoon))
                this.endH - 12
            } else {
                stringBuilder.append(activity.getString(R.string.common_time_noon))
                this.endH
            }
            stringBuilder.append("-$newEndH:${this.endM.with0()}")
        } else {
            stringBuilder.append("${this.startH}:${this.startM.with0()}")
            stringBuilder.append("-${this.endH}:${this.endM.with0()}")
        }
    }
}