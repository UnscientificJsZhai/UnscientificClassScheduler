package com.github.unscientificjszhai.unscientificclassscheduler.util

import android.app.Service
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

@Deprecated("Android 10以上使用自带振动效果")
private const val CLICK_VIBRATION_LENGTH: Long = 5//ms

@Deprecated("Android 10以上使用自带振动效果")
private const val CLICK_VIBRATION_STRENGTH = 5

/**
 * 创建轻触级别的振动。
 *
 * @param context 获取振动器的上下文。
 */
fun clickVibration(context: Context) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val manager =
                context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val effect =
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            manager.defaultVibrator.vibrate(effect)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
            @Suppress("DEPRECATION")
            (context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        }
        else -> {
            @Suppress("DEPRECATION")
            (context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(
                    CLICK_VIBRATION_LENGTH,
                    CLICK_VIBRATION_STRENGTH
                )
            )
        }
    }
}
