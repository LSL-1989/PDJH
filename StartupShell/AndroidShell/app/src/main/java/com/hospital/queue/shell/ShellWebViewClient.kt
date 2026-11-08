package com.hospital.queue.shell

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets

/**
 * 断网后页面加载失败时，由 MainActivity 定时重连。
 * 主文档失败时注入可读错误页，避免 WebView 纯空白。
 */
class ShellWebViewClient(
    private val onPageLoaded: () -> Unit,
    private val onPageError: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return false
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        // data:/about:blank 为错误页或销毁占位，不当作成功加载
        if (url.isNullOrBlank() || url.startsWith("data:") || url == "about:blank") {
            return
        }
        // AndroidNative 已由 addJavascriptInterface 注入，无需再包一层（包一层易自递归）
        onPageLoaded()
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request.isForMainFrame) {
            val detail = "HTTP ${errorResponse.statusCode} ${errorResponse.reasonPhrase}"
            showLoadErrorPage(view, request.url?.toString().orEmpty(), detail)
            onPageError()
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            showLoadErrorPage(view, request.url?.toString().orEmpty(), error.description?.toString().orEmpty())
            onPageError()
        }
    }

    /** 主框架加载失败时展示 URL / 错误说明，并提示长按改服务地址。 */
    private fun showLoadErrorPage(webView: WebView, failedUrl: String, detail: String) {
        val safeUrl = htmlEscape(failedUrl.ifBlank { "(未知地址)" })
        val safeDetail = htmlEscape(detail.ifBlank { "无法连接服务器" })
        val html = """
            <!DOCTYPE html>
            <html><head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <style>
              body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
                font-family:sans-serif;background:#0b1520;color:#e2e8f0;padding:24px;box-sizing:border-box}
              .card{max-width:420px;padding:28px 24px;border-radius:16px;background:#132033;border:1px solid #243447}
              h1{font-size:20px;margin:0 0 12px}
              p{font-size:14px;line-height:1.55;margin:0 0 10px;color:#94a3b8;word-break:break-all}
              .url{color:#2dd4bf}
              .hint{margin-top:16px;color:#cbd5e1}
            </style></head><body>
            <div class="card">
              <h1>页面加载失败</h1>
              <p class="url">$safeUrl</p>
              <p>$safeDetail</p>
              <p class="hint">请确认签到 H5 已启动，且服务根地址为 Vite/Nginx 根（勿带 /checkin）。长按屏幕可修改服务地址；开发联调一般为 http://&lt;本机IP&gt;:5174</p>
            </div></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html",
            StandardCharsets.UTF_8.name(),
            failedUrl.ifBlank { null }
        )
    }

    private fun htmlEscape(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
