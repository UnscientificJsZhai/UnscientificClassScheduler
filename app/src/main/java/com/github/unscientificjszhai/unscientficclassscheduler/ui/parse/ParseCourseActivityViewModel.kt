package com.github.unscientificjszhai.unscientficclassscheduler.ui.parse

import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificcourseparser.core.factory.ParserFactory

/**
 * ParseCourseActivity的ViewModel。
 *
 * @see ParseCourseActivity
 * @author UnscientificJsZhai
 */
internal class ParseCourseActivityViewModel : ViewModel() {

    val parserFactory by lazy {
        ParserFactory()
    }
}