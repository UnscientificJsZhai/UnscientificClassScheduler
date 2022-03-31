package com.github.unscientificjszhai.unscientificclassscheduler.ui.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.data.course.Course
import com.google.android.material.textfield.TextInputEditText

/**
 * 编辑单个课程信息的Activity中的RecyclerView中，头部的卡片。
 *
 * @author UnscientificJsZhai
 */
class EditCourseHeaderAdapter(private val course: Course) :
    RecyclerView.Adapter<EditCourseHeaderAdapter.HeaderViewHolder>() {

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val constraintLayout =
            view.findViewById<ConstraintLayout>(R.id.widget_ClassTImeEditor_RootLayout)

        val titleEditText: TextInputEditText =
            constraintLayout.findViewById(R.id.EditCourseActivity_TitleEditText)
        val creditEditText: TextInputEditText =
            constraintLayout.findViewById(R.id.EditCourseActivity_CreditEditText)
        val descriptionEditText: EditText =
            constraintLayout.findViewById(R.id.EditCourseActivity_DescriptionEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.widget_course_editor, parent, false)
        val viewHolder = HeaderViewHolder(view)

        viewHolder.apply {
            titleEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    course.title = titleEditText.text.toString()
                }
            }

            creditEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    try {
                        course.credit = creditEditText.text.toString().toDouble()
                    } catch (e: NumberFormatException) {
                        course.credit = 0.0
                        creditEditText.setText(course.credit.toString())
                    }
                }
            }

            descriptionEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    course.remarks = descriptionEditText.text.toString()
                }
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.titleEditText.setText(this.course.title)
        holder.creditEditText.setText(this.course.credit.toString())
        holder.descriptionEditText.setText(this.course.remarks)
    }

    override fun getItemCount() = 1
}