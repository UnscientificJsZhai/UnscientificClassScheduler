package com.github.unscientificjszhai.unscientificclassscheduler.ui.parse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificcourseparser.core.factory.ParserFactory

/**
 * 展示解析器列表的Fragment。
 *
 * @see ParseCourseActivity
 * @author UnscientificJsZhai
 */
class ParserListFragment : Fragment() {

    /**
     * 用于解析器列表的RecyclerView的适配器。
     */
    class ParserAdapter(
        factory: ParserFactory,
        private val setWebViewFragment: (String) -> Unit
    ) : RecyclerView.Adapter<ParserAdapter.ViewHolder>() {

        private val parserList = factory.parserList().keys.toList()
        private val parserMap = factory.parserList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val titleTextView: TextView = view.findViewById(R.id.ParserListRecycler_Title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_parser_list, parent, false)
            val viewHolder = ViewHolder(view)

            view.setOnClickListener {
                val beanName = parserMap[parserList[viewHolder.bindingAdapterPosition]]
                setWebViewFragment(beanName!!)
            }

            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.titleTextView.text = parserList[position]
        }

        override fun getItemCount() = this.parserList.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_parser_list, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.ParserListFragment_RecyclerView)
        val activity = requireActivity() as ParseCourseActivity
        val factory by activity
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = ParserAdapter(factory) { beanName ->
            //启动第二Fragment
            val webViewFragment = WebViewFragment.newInstance(beanName)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.SingleFragmentActivity_RootView, webViewFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_parser_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.ParseCourseActivity_Info) {
            //展示说明
            AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.outline_info_24)
                .setTitle(R.string.fragment_ParserListFragment_ParserListInfo)
                .setMessage(R.string.fragment_ParserListFragment_ParserListMessage)
                .setNegativeButton(R.string.fragment_ParserListFragment_DialogNegativeButton) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.fragment_ParserListFragment_DialogPositiveButton) { dialog, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data =
                            Uri.parse("https://github.com/UnscientificJsZhai/UnscientificCourseParser")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    dialog.dismiss()
                }.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}