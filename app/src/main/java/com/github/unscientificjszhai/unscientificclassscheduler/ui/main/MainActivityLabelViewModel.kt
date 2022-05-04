package com.github.unscientificjszhai.unscientificclassscheduler.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 用于存储[MainActivity]的ActionBar标签字符串的ViewModel。
 *
 * @author UnscientificJsZhai
 */
class MainActivityLabelViewModel : ViewModel() {

    private val labelLiveData = MutableLiveData<String>()

    /**
     * 更新标签。
     *
     * @param label 要更改的标签。
     */
    fun postLabel(label: String) {
        this.labelLiveData.postValue(label)
    }

    /**
     * 获取LiveData对象用于观察。
     *
     * @return 存储标签字符串信息的LiveData对象。
     */
    fun getLiveData(): LiveData<String> = this.labelLiveData
}