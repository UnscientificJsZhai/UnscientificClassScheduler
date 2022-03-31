package com.github.unscientificjszhai.unscientificclassscheduler.ui.parse

import androidx.lifecycle.ViewModel
import com.github.unscientificjszhai.unscientificcourseparser.core.factory.ParserFactory

/**
 * ParseCourseActivity的ViewModel。
 *
 * @see ParseCourseActivity
 * @author UnscientificJsZhai
 */
class ParseCourseActivityViewModel : ViewModel() {

    val parserFactory by lazy {
        ParserFactory()
    }
}