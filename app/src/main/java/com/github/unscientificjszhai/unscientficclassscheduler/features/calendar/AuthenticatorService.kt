package com.github.unscientificjszhai.unscientficclassscheduler.features.calendar

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 空白验证器的绑定服务。当验证器启动时该服务启动。
 *
 * @see EmptyAuthenticator
 * @author UnscientificJsZhai
 */
class AuthenticatorService : Service() {

    private lateinit var authenticator: EmptyAuthenticator

    override fun onCreate() {
        authenticator = EmptyAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder = authenticator.iBinder
}
