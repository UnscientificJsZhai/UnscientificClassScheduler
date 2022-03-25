package com.github.unscientificjszhai.unscientficclassscheduler.features.calendar

import android.accounts.*
import android.content.Context
import android.os.Bundle

/**
 * 空白身份验证器，用于创建系统账号，使日历绑定到账号。
 *
 * @author UnscientificJsZhai
 */
class EmptyAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    companion object {

        const val ACCOUNT_TYPE = "com.github.unscientificjszhai.unscientficclassscheduler.calendar"

        const val ACCOUNT_NAME = "CalendarAccount"

        /**
         * 向系统内添加一个预设好的账号。
         *
         * @param context 任意Context对象，会调用它的[Context.getApplicationContext]方法。
         * @return 成功添加则为true，失败则为false。如果已经存在这个预设的账号的话则也为true。
         */
        @JvmStatic
        fun addAccountToSystem(context: Context): Boolean {
            val accountManager = AccountManager.get(context.applicationContext)
            return if (accountManager.getAccountsByType(ACCOUNT_TYPE).isEmpty()) {
                val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
                accountManager.addAccountExplicitly(account, "", Bundle())
            } else {
                true
            }
        }
    }

    // 不支持编辑账号。
    override fun editProperties(
        response: AccountAuthenticatorResponse,
        accountType: String
    ): Bundle {
        throw UnsupportedOperationException()
    }

    //不支持手动添加账号。
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle?
    ): Bundle? {
        addAccountToSystem(context)
        return null
    }

    // 无视此方法。
    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        options: Bundle
    ): Bundle? = null

    // 不支持获取AuthToken。
    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    // 不支持获取AuthTokenLabel。
    override fun getAuthTokenLabel(authTokenType: String): String {
        throw UnsupportedOperationException()
    }

    // 不支持更新用户资料。
    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    // 不支持查询账户功能。
    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        features: Array<String>
    ): Bundle {
        throw UnsupportedOperationException()
    }
}
