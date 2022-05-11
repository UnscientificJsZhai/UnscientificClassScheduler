package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.data.CurrentTimeMarker
import com.github.unscientificjszhai.unscientificclassscheduler.ui.WelcomeActivity
import com.github.unscientificjszhai.unscientificclassscheduler.ui.main.fragments.CourseListFragment
import com.github.unscientificjszhai.unscientificclassscheduler.util.setSystemUIAppearance
import com.github.unscientificjszhai.unscientificclassscheduler.util.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.KProperty

/**
 * 主页Activity。其中的RecyclerView的Adapter参见[CourseAdapter]。
 *
 * @see CourseAdapter
 * @see MainActivityViewModel
 * @author UnscientificJsZhai
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CurrentTimeMarker.Provider {

    companion object {

        /**
         * 发送课程表变更的广播的Action。
         */
        const val COURSE_DATABASE_CHANGE_ACTION =
            "com.github.unscientificjszhai.unscientficclassscheduler.COURSE_DATABASE_CHANGE"

        /**
         * 确定是否显示帮助的Key，在SharedPreference：[SchedulerApplication.INITIAL]中查找。
         */
        const val SHOW_GUIDE_KEY = "mainActivityGuideShowed"

        /**
         * 确定是否为仅显示今天的Key，在SharedPreference：[SchedulerApplication.INITIAL]中查找。
         */
        const val SHOW_TODAY_ONLY_KEY = "showTodayOnly"
    }

    private lateinit var schedulerApplication: SchedulerApplication
    private val currentTimeMarker: CurrentTimeMarker by lazy {
        schedulerApplication.courseTable?.let {
            CurrentTimeMarker(it)
        } ?: throw RuntimeException()
    }

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var labelViewModel: MainActivityLabelViewModel

    private lateinit var rootView: FrameLayout

    /**
     * 用于处理当前课程表更改事件的广播接收器。
     */
    inner class DatabaseChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context is MainActivity) {
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.SingleFragmentActivity_RootView)

                // 更新NowTimeTagger
                val courseTable by context.schedulerApplication
                context.currentTimeMarker.setCourseTable(courseTable)
                if (fragment is CourseListFragment && fragment.lifecycle.currentState == Lifecycle.State.STARTED) {
                    viewModel.courseList.removeObservers(fragment.viewLifecycleOwner)
                    viewModel.courseList = context.schedulerApplication
                        .getCourseDatabase().courseDao()
                        .getLiveCourses(schedulerApplication.nowTableID) // 更新ViewModel中的LiveData
                    viewModel.courseList.observe(this@MainActivity) { courseList ->
                        fragment.bindData(courseList)
                    }

                    fragment.updateActionBarLabel()
                } else {
                    viewModel.courseList = context.schedulerApplication
                        .getCourseDatabase().courseDao()
                        .getLiveCourses(schedulerApplication.nowTableID) // 更新ViewModel中的LiveData
                }
            }
        }
    }

    private lateinit var databaseChangeReceiver: DatabaseChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.schedulerApplication = application as SchedulerApplication
        // 与初次启动判定有关
        if (this.schedulerApplication.nowTableID < 0) {
            Toast.makeText(this, R.string.activity_Main_NoTableFound, Toast.LENGTH_SHORT).show()
            startActivity<WelcomeActivity>(this)
            finish()
            return
        }

        setContentView(R.layout.activity_single_fragment)

        // 设置SystemUI颜色
        setSystemUIAppearance(this)

        this.viewModel =
            ViewModelProvider(this)[MainActivityViewModel::class.java]
        this.labelViewModel =
            ViewModelProvider(this)[MainActivityLabelViewModel::class.java]

        this.labelViewModel.getLiveData().observe(this) {
            supportActionBar?.title = it
        }

        this.rootView = findViewById(R.id.SingleFragmentActivity_RootView)

        // 加载首个Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.SingleFragmentActivity_RootView, CourseListFragment())
                .commit()
        }

        val sharedPreferences =
            getSharedPreferences(SchedulerApplication.INITIAL, Context.MODE_PRIVATE)
        viewModel.showTodayOnly = sharedPreferences.getBoolean(SHOW_TODAY_ONLY_KEY, false)

        // 监听数据库变更
        this.databaseChangeReceiver = DatabaseChangeReceiver()
        registerReceiver(this.databaseChangeReceiver, IntentFilter().apply {
            addAction(COURSE_DATABASE_CHANGE_ACTION)
        })

        // 首次打开则显示帮助
        if (!sharedPreferences.getBoolean(SHOW_GUIDE_KEY, false)) {
            Toast.makeText(this, R.string.activity_Main_GuideToast, Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putBoolean(SHOW_GUIDE_KEY, true).apply()
        }
    }

    override fun onStop() {
        super.onStop()
        // 保存是否只显示今天的情况
        val sharedPreferences =
            getSharedPreferences(SchedulerApplication.INITIAL, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putBoolean(SHOW_TODAY_ONLY_KEY, viewModel.showTodayOnly)
            commit()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(this.databaseChangeReceiver)
        super.onDestroy()
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): CurrentTimeMarker {
        return this.currentTimeMarker
    }

    /**
     * 查询是否只显示今天，提供给Adapter使用。
     *
     * @return 是否只显示今天。
     */
    internal fun isShowTodayOnly(): Boolean = this.viewModel.showTodayOnly
}