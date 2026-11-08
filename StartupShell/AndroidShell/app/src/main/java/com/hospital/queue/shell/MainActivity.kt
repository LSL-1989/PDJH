package com.hospital.queue.shell

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var pageLoadFailed = false
    private var lastBackPressTime = 0L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var serverConfigDialog: AlertDialog? = null

    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (pageLoadFailed && isNetworkAvailable()) {
                loadHome()
            }
            if (pageLoadFailed) {
                handler.postDelayed(this, RECONNECT_INTERVAL_MS)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableFullscreen()

        webView = WebView(this)
        setContentView(webView)

        configureWebView()
        registerNetworkCallback()
        loadHome()
    }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            // 关闭 overview 缩放，避免竖屏 WebView 将页面按内容高度缩小后下半屏留白
            loadWithOverviewMode = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                Log.d(
                    TAG,
                    "JS ${message.messageLevel()}: ${message.message()} @${message.sourceId()}:${message.lineNumber()}"
                )
                return true
            }
        }

        // 长按打开服务地址维护（Kiosk 现场改 baseUrl）
        webView.setOnLongClickListener {
            showServerConfigDialog()
            true
        }
        webView.isLongClickable = true

        webView.addJavascriptInterface(AndroidNativeBridge(this), "AndroidNative")
        webView.webViewClient = ShellWebViewClient(
            onPageLoaded = {
                pageLoadFailed = false
                handler.removeCallbacks(reconnectRunnable)
            },
            onPageError = {
                pageLoadFailed = true
                handler.removeCallbacks(reconnectRunnable)
                handler.postDelayed(reconnectRunnable, RECONNECT_INTERVAL_MS)
            }
        )
    }

    /** 维护模式：配置内网服务根 URL，保存后重载首页。 */
    private fun showServerConfigDialog() {
        if (serverConfigDialog?.isShowing == true) return

        val padding = (24 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            setText(ServerConfig.getServerBase(this@MainActivity))
            hint = getString(R.string.server_base_hint)
            setSingleLine(true)
            setPadding(padding, padding / 2, padding, padding / 2)
        }
        val container = FrameLayout(this).apply {
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.leftMargin = padding
            lp.rightMargin = padding
            addView(input, lp)
        }

        val deviceId = DeviceIdManager.getOrCreateDeviceId(this)
        val type = BuildConfig.TERMINAL_TYPE
        serverConfigDialog = AlertDialog.Builder(this)
            .setTitle(R.string.server_config_title)
            .setMessage(getString(R.string.server_config_message, type, deviceId))
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val base = input.text?.toString()?.trim().orEmpty()
                ServerConfig.setServerBase(this, base)
                Toast.makeText(this, R.string.server_saved, Toast.LENGTH_SHORT).show()
                pageLoadFailed = false
                handler.removeCallbacks(reconnectRunnable)
                loadHome()
            }
            .setNeutralButton(R.string.reset_default) { _, _ ->
                ServerConfig.clearServerBase(this)
                Toast.makeText(this, R.string.server_reset, Toast.LENGTH_SHORT).show()
                pageLoadFailed = false
                handler.removeCallbacks(reconnectRunnable)
                loadHome()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { serverConfigDialog = null }
            .show()
    }

    private fun loadHome() {
        webView.loadUrl(ServerConfig.getHomeUrl(this))
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (pageLoadFailed) {
                    handler.post { loadHome() }
                }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < DOUBLE_BACK_INTERVAL_MS) {
                finish()
                return true
            }
            lastBackPressTime = now
            Toast.makeText(this, R.string.exit_hint, Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        serverConfigDialog?.dismiss()
        handler.removeCallbacks(reconnectRunnable)
        networkCallback?.let {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        webView.apply {
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "QueueShell"
        private const val RECONNECT_INTERVAL_MS = 5000L
        private const val DOUBLE_BACK_INTERVAL_MS = 2000L
    }
}
