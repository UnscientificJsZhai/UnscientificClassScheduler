package com.github.unscientificjszhai.unscientificclassscheduler.ui.parse

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.ui.others.CalendarOperatorActivity
import com.github.unscientificjszhai.unscientificclassscheduler.util.setSystemUIAppearance
import com.github.unscientificjszhai.unscientificcourseparser.core.factory.ParserFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.KProperty

/**
 * 从教务系统导入。
 *
 * @see ParserListFragment
 * @see WebViewFragment
 * @see CourseListFragment
 * @author UnscientificJsZhai
 */
@AndroidEntryPoint
class ParseCourseActivity : CalendarOperatorActivity() {

    private lateinit var viewModel: ParseCourseActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)

        setSystemUIAppearance(this)

        this.viewModel = ViewModelProvider(this)[ParseCourseActivityViewModel::class.java]

        if (savedInstanceState == null) {
            // 旋转屏幕时不重新进入ParserListFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.SingleFragmentActivity_RootView, ParserListFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.SingleFragmentActivity_RootView)
        if (fragment is WebViewFragment && fragment.webPageBack()) {
            return
        }
        super.onBackPressed()
    }
}