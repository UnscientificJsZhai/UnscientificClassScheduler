package com.github.unscientificjszhai.unscientficclassscheduler.ui.parse

import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificcourseparser.core.parser.Parser

/**
 * 用于存放当前的解析器的ViewModel。
 *
 * @see WebViewFragment
 * @author UnscientificJsZhai
 */
internal class WebViewFragmentViewModel : ViewModel() {

    lateinit var parser: Parser
}