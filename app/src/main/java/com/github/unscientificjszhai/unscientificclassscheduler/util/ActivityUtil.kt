@file:JvmName("ActivityUtil")

package com.github.unscientificjszhai.unscientificclassscheduler.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import com.github.unscientificjszhai.unscientificclassscheduler.R

/**
 * 为目标Activity设置浅色状态栏和导航栏。
 *
 * @param context Activity的上下文。
 * @author UnscientificJsZhai
 */
fun setSystemUIAppearance(context: Activity) {
    val window = context.window
    if (context.isDarkMode()) {
        // 深色模式不进行调整
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = window.insetsController
        val flag = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        controller?.setSystemBarsAppearance(flag, flag)
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}

/**
 * 判断当前是否处于深色模式中。
 *
 * @receiver 上下文。
 * @author UnscientificJsZhai
 */
private fun Context.isDarkMode(): Boolean =
    this.applicationContext.resources.configuration.uiMode == 0x21

/**
 * 当获得权限时运行。
 *
 * @receiver 要执行此操作的Activity。
 * @param permission 要检查的权限。
 * @param permissionDenied 当没有获得权限时运行的代码块。
 * @param block 当获得权限时运行的代码块。
 * @author UnscientificJsZhai
 */
inline fun <T> Activity.runIfPermissionGranted(
    permission: String,
    permissionDenied: Activity.() -> Unit,
    block: Activity.() -> T
): T? {
    if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
        return block()
    } else {
        permissionDenied()
    }
    return null
}

/**
 *  跳转到系统设置来请求权限。
 *
 *  @receiver 发起跳转的Activity。
 *  @author UnscientificJsZhai
 */
fun Activity.jumpToSystemPermissionSettings() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    } catch (e: Exception) {
        Log.e("ActivityUtil", "jumpToSystemPermissionSettings: \n$e")
        Toast.makeText(
            this,
            R.string.activity_WelcomeActivity_FailToJumpToSettings,
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * 启动指定的Activity。
 *
 * @param T 要启动的Activity
 * @param context 启动这个Activity的上下文。
 */
inline fun <reified T : Activity> startActivity(context: Context) {
    val intent = Intent(context, T::class.java)
    context.startActivity(intent)
}

/**
 * 启动指定的Activity。
 *
 * @param T 要启动的Activity
 * @param context 启动这个Activity的上下文。
 * @param doWithIntent 启动Activity前对Intent的操作，包括添加Flag等。
 */
inline fun <reified T : Activity> startActivity(
    context: Context,
    crossinline doWithIntent: Intent.() -> Unit
) {
    val intent = Intent(context, T::class.java)
    doWithIntent(intent)
    context.startActivity(intent)
}