package com.github.unscientificjszhai.unscientficclassscheduler.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.google.android.material.textfield.TextInputEditText

/**
 * 上课时间编辑Activity的头部。
 *
 * @author UnscientificJsZhai
 */
internal class TimeTableHeaderAdapter(private val viewModel: TimeTableEditorActivityViewModel) :
    RecyclerView.Adapter<TimeTableHeaderAdapter.HeaderViewHolder>() {

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val titleText: TextView = view.findViewById(R.id.TimeTableEditorActivity_HeaderText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.widget_time_table_editor_header, parent, false)
        val viewHolder = HeaderViewHolder(view)

        view.setOnClickListener {
            val root: FrameLayout =
                View.inflate(parent.context, R.layout.dialog_input, null) as FrameLayout
            val editText = root.findViewById<TextInputEditText>(R.id.InputDialog_EditText)
            editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setHint(R.string.activity_TimeTableEditor_HeaderEditDialogHint)
            editText.setText(this.viewModel.duration.toString())

            AlertDialog.Builder(parent.context)
                .setTitle(R.string.activity_TimeTableEditor_HeaderEditDialogTitle)
                .setView(root)
                .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                    dialog?.dismiss()
                }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                    viewModel.duration = editText.text.toString().toInt()
                    viewHolder.titleText.apply {
                        setText(R.string.activity_TimeTableEditor_HeaderItemTitle)
                        text = text.toString().format(viewModel.duration)
                    }
                    dialog.dismiss()
                }.show()
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.titleText.apply {
            setText(R.string.activity_TimeTableEditor_HeaderItemTitle)
            text = text.toString().format(viewModel.duration)
        }
    }

    override fun getItemCount() = 1
}