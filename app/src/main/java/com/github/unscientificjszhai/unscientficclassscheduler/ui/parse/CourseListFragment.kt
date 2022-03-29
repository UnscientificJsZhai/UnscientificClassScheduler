package com.github.unscientificjszhai.unscientficclassscheduler.ui.parse

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.course.CourseWithClassTimes
import com.github.unscientificjszhai.unscientficclassscheduler.features.parse.ParserTypeConverter
import com.github.unscientificjszhai.unscientficclassscheduler.ui.others.ProgressDialog
import com.github.unscientificjszhai.unscientificcourseparser.core.export.CoursesJson.Companion.json
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.github.unscientificjszhai.unscientificcourseparser.core.data.Course as SourceCourse

/**
 * 展示解析结果的Fragment。
 *
 * @author UnscientificJsZhai
 */
@AndroidEntryPoint
class CourseListFragment : Fragment() {

    /**
     * 用于展示解析到的课程的RecyclerView的Adapter。
     *
     * @param courseList 要展示的数据列表。
     * @param classTimeString 格式化上课时间字符串的模板。
     */
    internal class CourseAdapter(
        private val courseList: List<CourseWithClassTimes>,
        private val classTimeString: String
    ) :
        RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val titleText: TextView =
                view.findViewById(R.id.CourseListFragmentRecycler_TitleText)
            val subTitleText: TextView =
                view.findViewById(R.id.CourseListFragmentRecycler_SubTitleText)
            val classTimeText: TextView =
                view.findViewById(R.id.CourseListFragmentRecycler_ClassTimeText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val rootView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_course_list_fragment, parent, false)
            return ViewHolder(rootView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val courseWithClassTimes = this.courseList[position]
            val course = courseWithClassTimes.course
            holder.titleText.text = course.title
            if (course.remarks.isEmpty()) {
                holder.subTitleText.visibility = View.GONE
            } else {
                holder.subTitleText.visibility = View.VISIBLE
                holder.subTitleText.text = course.remarks
            }
            holder.classTimeText.text =
                this.classTimeString.format(courseWithClassTimes.classTimes.size)
        }

        override fun getItemCount() = this.courseList.size
    }

    companion object {

        private const val RESULT_KEY = "result"

        /**
         * 启动这个Fragment的静态方法。
         *
         * @param result 解析结果。
         */
        @JvmStatic
        fun newInstance(result: List<SourceCourse>) = CourseListFragment().apply {
            arguments = Bundle().apply {
                putString(RESULT_KEY, result.json().toString())
            }
        }
    }

    private lateinit var viewModel: CourseListFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        this.viewModel = ViewModelProvider(this)[CourseListFragmentViewModel::class.java]
        if (savedInstanceState == null) {
            val jsonString = arguments?.getString(RESULT_KEY) ?: ""
            //Log.e("CourseListFragment", "\n$jsonString")
            this.viewModel.courseList =
                ParserTypeConverter.fromJson(jsonString).generateConvertedCourse()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_course_list, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.CourseListFragment_RecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CourseAdapter(
            this.viewModel.courseList,
            getString(R.string.fragment_CourseListFragment_ClassTimeDescription)
        )

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_course_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.ParseCourseActivity_Done) {
            val dialog = ProgressDialog(requireActivity())
            lifecycleScope.launch {
                dialog.show()
                val errorList =
                    viewModel.save(requireActivity().application as SchedulerApplication)
                dialog.postDismiss()
                // 显示确认按钮
                if (errorList?.isNotEmpty() == true || errorList == null) {
                    AlertDialog.Builder(requireContext()).apply {
                        if (errorList == null) {
                            setTitle(R.string.fragment_CourseListFragment_Dialog_FailTitle)
                            setMessage(R.string.fragment_CourseListFragment_Dialog_FailMessage)
                        } else {
                            setTitle(R.string.fragment_CourseListFragment_Dialog_CompleteTitle)
                            val message =
                                StringBuilder(getString(R.string.fragment_CourseListFragment_Dialog_CompleteMessage))
                            for (title in errorList) {
                                message.append("\n$title")
                            }
                            setMessage(message.toString())
                        }
                        setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            dialog.dismiss()
                        }
                        setOnDismissListener {
                            requireActivity().finish()
                        }
                    }.show()
                } else {
                    // 完全正常导入
                    requireActivity().finish()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}