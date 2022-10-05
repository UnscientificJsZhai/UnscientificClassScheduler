package com.github.unscientificjszhai.unscientificclassscheduler.ui.others

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.SchedulerApplication
import com.github.unscientificjszhai.unscientificclassscheduler.util.jumpToSystemPermissionSettings

/**
 * 所有需要用到日历权限的Activity的基类。
 * 这些Activity都会在每次onStart调用时确认自己的权限。
 *
 * @author UnscientificJsZhai
 */
abstract class CalendarOperatorActivity : AppCompatActivity() {

    /**
     * 每次在onStart中检查权限。如果权限不足时用此启动器申请权限。
     */
    private lateinit var requestPermissionCallback: ActivityResultLauncher<Array<String>>

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestPermissionCallback = registerPermissionCallback()

    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        this.requestPermissionCallback = registerPermissionCallback()
    }

    @CallSuper
    override fun onStart() {
        if ((application as SchedulerApplication).useCalendar) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_DENIED
            ) {
                // 申请日历权限
                this.requestPermissionCallback.launch(
                    arrayOf(
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.READ_CALENDAR
                    )
                )
            }
        }
        super.onStart()
    }

    /**
     * 注册申请权限回调。只能使用在onCreate方法中。
     *
     * @return 申请权限启动器。已经写好回调。
     */
    private fun registerPermissionCallback() =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.contains(false)) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR)) {
                    // 系统仍然提示权限请求
                    AlertDialog.Builder(this)
                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                        .setMessage(R.string.activity_WelcomeActivity_AskPermissionText)
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            showDeniedToast()
                            dialog.dismiss()
                        }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.WRITE_CALENDAR
                                ) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.READ_CALENDAR
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                // 申请日历权限
                                this.requestPermissionCallback.launch(
                                    arrayOf(
                                        Manifest.permission.WRITE_CALENDAR,
                                        Manifest.permission.READ_CALENDAR
                                    )
                                )
                            }
                            dialog.dismiss()
                        }.show()
                } else {
                    // 系统不再提示权限请求
                    AlertDialog.Builder(this)
                        .setTitle(R.string.activity_WelcomeActivity_AskPermissionTitle)
                        .setMessage(
                            getString(R.string.activity_WelcomeActivity_AskPermissionText) + "\n"
                                    + getString(R.string.activity_WelcomeActivity_SettingsPermissionText)
                        )
                        .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                            showDeniedToast()
                            dialog.dismiss()
                        }.setPositiveButton(R.string.common_confirm) { dialog, _ ->
                            jumpToSystemPermissionSettings()
                            dialog.dismiss()
                        }.show()
                }
            }
        }

    /**
     * 权限申请不通过时弹出Toast提醒。
     */
    private fun showDeniedToast() {
        // 用户执意拒绝授权
        (application as SchedulerApplication).useCalendar = false
        Toast.makeText(
            this,
            R.string.preferences_CalendarOption_UseCalendar_TurnedOff,
            Toast.LENGTH_LONG
        ).show()
    }
}