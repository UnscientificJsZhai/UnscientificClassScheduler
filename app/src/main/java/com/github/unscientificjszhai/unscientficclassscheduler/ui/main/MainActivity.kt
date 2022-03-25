package com.github.unscientificjszhai.unscientficclassscheduler.ui.main

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
import com.github.unscientificjszhai.unscientficclassscheduler.R
import com.github.unscientificjszhai.unscientficclassscheduler.TimeManagerApplication
import com.github.unscientificjszhai.unscientficclassscheduler.data.CurrentTimeMarker
import com.github.unscientificjszhai.unscientficclassscheduler.ui.WelcomeActivity
import com.github.unscientificjszhai.unscientficclassscheduler.ui.main.fragments.CourseListFragment
import com.github.unscientificjszhai.unscientficclassscheduler.util.setSystemUIAppearance
import com.github.unscientificjszhai.unscientficclassscheduler.util.startActivity
import java.util.*
import kotlin.reflect.KProperty

/**
 * 主页Activity。其中的RecyclerView的Adapter参见[CourseAdapter]。
 *
 * @see CourseAdapter
 * @see MainActivityViewModel
 * @author UnscientificJsZhai
 */
class MainActivity : AppCompatActivity(), CurrentTimeMarker.Getter {

    companion object {

        /**
         * 发送课程表变更的广播的Action。
         */
        const val COURSE_DATABASE_CHANGE_ACTION =
            "com.github.unscientificjszhai.unscientficclassscheduler.COURSE_DATABASE_CHANGE"

        /**
         * 确定是否显示帮助的Key，在SharedPreference：[TimeManagerApplication.INITIAL]中查找。
         */
        const val SHOW_GUIDE_KEY = "mainActivityGuideShowed"

        /**
         * 确定是否为仅显示今天的Key，在SharedPreference：[TimeManagerApplication.INITIAL]中查找。
         */
        const val SHOW_TODAY_ONLY_KEY = "showTodayOnly"
    }

    private lateinit var timeManagerApplication: TimeManagerApplication
    private val currentTimeMarker: CurrentTimeMarker by lazy {
        timeManagerApplication.courseTable?.let {
            CurrentTimeMarker(it)
        } ?: throw RuntimeException()
    }

    private lateinit var viewModel: MainActivityViewModel

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
                val courseTable by context.timeManagerApplication
                context.currentTimeMarker.setCourseTable(courseTable)
                if (fragment is CourseListFragment && fragment.lifecycle.currentState == Lifecycle.State.STARTED) {
                    fragment.viewModel.courseList.removeObservers(fragment.viewLifecycleOwner)
                    viewModel.courseList = context.timeManagerApplication
                        .getCourseDatabase().courseDao().getLiveCourses() // 更新ViewModel中的LiveData
                    fragment.viewModel.courseList.observe(this@MainActivity) { courseList ->
                        fragment.bindData(courseList)
                    }

                    fragment.updateActionBarLabel()
                } else {
                    viewModel.courseList = context.timeManagerApplication
                        .getCourseDatabase().courseDao().getLiveCourses() // 更新ViewModel中的LiveData
                }
            }
        }
    }

    /**
     * 处理系统日期变更的广播接收器。
     */
    inner class DateChangeReceiver : BroadcastReceiver() {

        /**
         * 上次收到时间变化广播的时间。
         */
        private var timeBeforeUpdate = Calendar.getInstance()

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                // 检测日期是否发生变化
                val timeRightNow = Calendar.getInstance()
                if (this.timeBeforeUpdate.get(Calendar.DAY_OF_YEAR) != timeRightNow.get(Calendar.DAY_OF_YEAR) ||
                    this.timeBeforeUpdate.get(Calendar.YEAR) != timeRightNow.get(Calendar.YEAR)
                ) {
                    if (context is MainActivity) {
                        // 如果为只显示今天则更新数据集
                        val fragment =
                            context.supportFragmentManager.findFragmentById(R.id.SingleFragmentActivity_RootView)
                        if (fragment is CourseListFragment && fragment.lifecycle.currentState == Lifecycle.State.STARTED) {
                            fragment.run {
                                bindData(viewModel.courseList.value ?: ArrayList())
                            }
                        }
                    }
                }

                // 最后更新新的当前时间
                this.timeBeforeUpdate = timeRightNow
            }
        }
    }

    private lateinit var databaseChangeReceiver: DatabaseChangeReceiver
    private lateinit var dateChangeReceiver: DateChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.timeManagerApplication = application as TimeManagerApplication
        // 与初次启动判定有关
        if (this.timeManagerApplication.nowTableID < 0) {
            Toast.makeText(this, R.string.activity_Main_NoTableFound, Toast.LENGTH_SHORT).show()
            startActivity<WelcomeActivity>(this)
            finish()
            return
        }

        setContentView(R.layout.activity_single_fragment)

        // 设置SystemUI颜色
        setSystemUIAppearance(this)

        val courseDatabase = timeManagerApplication.getCourseDatabase()
        this.viewModel =
            ViewModelProvider(
                this,
                MainActivityViewModel.Factory(courseDatabase.courseDao())
            )[MainActivityViewModel::class.java]

        this.rootView = findViewById(R.id.SingleFragmentActivity_RootView)

        // 加载首个Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.SingleFragmentActivity_RootView, CourseListFragment())
                .commit()
        }

        val sharedPreferences =
            getSharedPreferences(TimeManagerApplication.INITIAL, Context.MODE_PRIVATE)
        viewModel.showTodayOnly = sharedPreferences.getBoolean(SHOW_TODAY_ONLY_KEY, false)

        // 监听数据库变更
        this.databaseChangeReceiver = DatabaseChangeReceiver()
        registerReceiver(this.databaseChangeReceiver, IntentFilter().apply {
            addAction(COURSE_DATABASE_CHANGE_ACTION)
        })

        // 监听日期变更
        this.dateChangeReceiver = DateChangeReceiver()
        registerReceiver(this.dateChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
        })

        // 首次打开则显示帮助
        if (!sharedPreferences.getBoolean(SHOW_GUIDE_KEY, false)) {
            Toast.makeText(this, R.string.activity_Main_GuideToast, Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putBoolean(SHOW_GUIDE_KEY, true).apply()
        }
    }

    /**
     * 更新主界面的ActionBar的Label。
     *
     * @param label 新的label。
     */
    fun updateLabel(label: String) {
        supportActionBar?.title = label
    }

    override fun onStop() {
        super.onStop()
        // 保存是否只显示今天的情况
        val sharedPreferences =
            getSharedPreferences(TimeManagerApplication.INITIAL, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putBoolean(SHOW_TODAY_ONLY_KEY, viewModel.showTodayOnly)
            commit()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(this.databaseChangeReceiver)
        unregisterReceiver(this.dateChangeReceiver)
        super.onDestroy()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): CurrentTimeMarker {
        return this.currentTimeMarker
    }

    /**
     * 查询是否只显示今天，提供给Adapter使用。
     *
     * @return 是否只显示今天。
     */
    internal fun isShowTodayOnly(): Boolean = this.viewModel.showTodayOnly
}