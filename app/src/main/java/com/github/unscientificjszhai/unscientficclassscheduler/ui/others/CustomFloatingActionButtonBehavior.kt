package com.github.unscientificjszhai.unscientficclassscheduler.ui.others

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 使悬浮按钮在上下滑动界面时显示和隐藏。需要悬浮按钮直接作为[CoordinatorLayout]的子控件。
 * 参数用于在xml中静态声明behavior使用。
 *
 * @author UnscientificJsZhai
 */
class CustomFloatingActionButtonBehavior(context: Context, attributes: AttributeSet) :
    FloatingActionButton.Behavior(context, attributes) {

    /**
     * 动画实现对象。在这里实现动画效果。
     */
    internal object Animate {

        private val linearInterpolator = AccelerateDecelerateInterpolator()

        private const val DURATION: Long = 200

        /**
         * 显示悬浮按钮。让悬浮按钮从不可见转为可见。
         *
         * @param view 要显示的View。
         */
        fun showButton(view: View) {
            view.animate().cancel()
            view.alpha = 0f
            view.scaleY = 0f
            view.scaleX = 0f

            view.animate().scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DURATION)
                .setInterpolator(linearInterpolator)
                .setListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationStart(animation: Animator?) {
                        view.visibility = View.VISIBLE
                    }
                }).start()
        }

        /**
         * 隐藏悬浮按钮，让悬浮按钮从可见转为不可见。
         *
         * @param view 要隐藏的View。
         */
        fun hideButton(view: View) {
            view.animate().cancel()
            view.animate().scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(DURATION)
                .setInterpolator(linearInterpolator)
                .setListener(object : AnimatorListenerAdapter() {

                    private var mCanceled = false

                    override fun onAnimationStart(animation: Animator?) {
                        view.visibility = View.VISIBLE
                        mCanceled = false
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        mCanceled = true
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        if (!mCanceled) {
                            view.visibility = View.INVISIBLE
                        }
                    }
                }).start()
        }
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return super.onStartNestedScroll(
            coordinatorLayout,
            child,
            directTargetChild,
            target,
            axes,
            type
        ) || axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        // 隐藏
        if ((dyConsumed > 0 || dyUnconsumed > 0) && child.visibility == View.VISIBLE) {
            Animate.hideButton(child)
        }
        // 显示
        else if ((dyConsumed < 0 || dyUnconsumed < 0) && child.visibility == View.INVISIBLE) {
            Animate.showButton(child)
        }
    }
}