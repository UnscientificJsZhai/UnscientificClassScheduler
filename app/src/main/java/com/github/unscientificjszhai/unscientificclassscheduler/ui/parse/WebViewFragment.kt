package com.github.unscientificjszhai.unscientificclassscheduler.ui.parse

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.unscientificjszhai.unscientificclassscheduler.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 显示网页的Fragment。
 *
 * @see ParseCourseActivity
 * @author UnscientificJsZhai
 */
class WebViewFragment : Fragment() {

    /**
     * JavaScript对象，用于获取HTML源。
     */
    inner class InJavaScriptLocalObject {

        @JavascriptInterface
        fun getSource(html: String) {
            parse(html)
        }
    }

    /**
     * 调用注入的JavaScript函数。
     *
     * @see InJavaScriptLocalObject.getSource
     */
    private fun callInjectedJavaScriptMethod() {
        this.webView.loadUrl(
            "javascript:window.$JAVASCRIPT_INTERFACE_NAME.getSource('<html>'+document." +
                    "getElementsByTagName('html')[0].innerHTML+'</html>');"
        )
    }

    companion object {

        private const val BEAN_NAME_KEY = "beanName"

        private const val JAVASCRIPT_INTERFACE_NAME = "java_interface"

        /**
         * 启动这个Fragment的静态方法。
         *
         * @param beanName 要加载的解析器的beanName。
         */
        @JvmStatic
        fun newInstance(beanName: String) = WebViewFragment().apply {
            arguments = Bundle().apply {
                putString(BEAN_NAME_KEY, beanName)
            }
        }
    }

    private lateinit var webView: WebView

    private lateinit var viewModel: WebViewFragmentViewModel
    private val activityViewModel: ParseCourseActivityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        this.viewModel = ViewModelProvider(this)[WebViewFragmentViewModel::class.java]
        if (savedInstanceState == null) {
            arguments?.let {
                val beanName = it.getString(BEAN_NAME_KEY)
                viewModel.parser = activityViewModel.parserFactory[beanName!!]
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_web_view, container, false)

        this.webView = view.findViewById(R.id.WebViewFragment_WebView)
        webView.apply {
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    // Do nothing
                }

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    if (message != null && result != null) {
                        result.confirm()
                        return true
                    }
                    return super.onJsAlert(view, url, message, result)
                }

                override fun onJsConfirm(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    if (message != null && result != null) {
                        return true
                    }
                    return super.onJsConfirm(view, url, message, result)
                }
            }
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }
            settings.apply {
                userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36 Edg/92.0.902.73"
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                blockNetworkLoads = false
                databaseEnabled = true
            }
            addJavascriptInterface(InJavaScriptLocalObject(), JAVASCRIPT_INTERFACE_NAME)
            loadUrl(viewModel.parser.url)
        }

        //显示提示气泡
        Toast.makeText(
            container?.context,
            viewModel.parser.message.ifBlank {
                getString(R.string.activity_ParseCourseActivity_HelpToast)
            },
            Toast.LENGTH_LONG
        ).show()

        return view
    }

    /**
     * 解析方法，经过一系列调用后最后会把html返回到这里。
     *
     * @param html 当前页面的HTML代码。
     */
    private fun parse(html: String) {
        val parser = viewModel.parser
        viewModel.viewModelScope.launch {
            val list = withContext(Dispatchers.Default) {
                try {
                    parser.parse(html)
                } catch (e: Exception) {
                    ArrayList()
                }
            }

            //启动第三Fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.SingleFragmentActivity_RootView, CourseListFragment.newInstance(list))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_web_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.ParseCourseActivity_Done) {
            this.callInjectedJavaScriptMethod()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 在WebView上返回上一个页面。
     *
     * @return WebView已经返回，返回true。如果WebView不能返回，返回false。
     */
    @Deprecated("")
    fun webPageBack(): Boolean = if (webView.canGoBack()) {
        webView.goBack()
        true
    } else {
        false
    }
}