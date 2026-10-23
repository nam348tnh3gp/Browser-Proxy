package TGFN.BrowserMini

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.net.URI
import java.util.concurrent.Executor

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var proxySummary: TextView
    private val prefs by lazy { getSharedPreferences("browser_settings", Context.MODE_PRIVATE) }
    private val mainExecutor = Executor { command -> runOnUiThread(command) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        configureWebView()
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(prefs.getString("home_url", "https://www.google.com") ?: "https://www.google.com")
        }
        updateProxySummary()
    }

    private fun configureWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                status.text = "Đang tải…"
                if (!url.isNullOrBlank()) address.setText(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = "Sẵn sàng"
                if (!url.isNullOrBlank()) address.setText(url)
            }
            override fun onReceivedError(
                view: WebView?, request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (request?.isForMainFrame == true) status.text = "Lỗi tải trang: ${error?.description ?: "không rõ"}"
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(6))
        }
        val addressRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        address = EditText(this).apply {
            hint = "Tìm kiếm hoặc nhập URL"
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setText("https://www.google.com")
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { navigate(); true } else false
            }
        }
        addressRow.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addressRow.addView(Button(this).apply { text = "Đi"; setOnClickListener { navigate() } })
        root.addView(addressRow)

        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolbar.addView(Button(this).apply { text = "←"; contentDescription = "Quay lại"; setOnClickListener { if (webView.canGoBack()) webView.goBack() } })
        toolbar.addView(Button(this).apply { text = "→"; contentDescription = "Tiến"; setOnClickListener { if (webView.canGoForward()) webView.goForward() } })
        toolbar.addView(Button(this).apply { text = "↻"; contentDescription = "Tải lại"; setOnClickListener { webView.reload() } })
        toolbar.addView(Button(this).apply { text = "⚙ Settings"; setOnClickListener { showSettings() } })
        root.addView(toolbar)

        proxySummary = TextView(this).apply { textSize = 12f; setPadding(dp(4), dp(2), dp(4), dp(4)) }
        root.addView(proxySummary)
        status = TextView(this).apply { text = "Sẵn sàng"; textSize = 12f }
        root.addView(status)
        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun showSettings() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val title = TextView(this).apply { text = "TGFN.BrowserMini Settings"; textSize = 20f }
        content.addView(title)

        val enabled = android.widget.CheckBox(this).apply {
            text = "Bật HTTP proxy"
            isChecked = prefs.getBoolean("proxy_enabled", false)
        }
        content.addView(enabled)

        val proxyUrl = EditText(this).apply {
            hint = "Proxy URL: http://host:port"
            singleLine = true
            setText(prefs.getString("proxy_url", "") ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        content.addView(proxyUrl)

        val home = EditText(this).apply {
            hint = "Trang chủ URL"
            singleLine = true
            setText(prefs.getString("home_url", "https://www.google.com") ?: "")
        }
        content.addView(home)

        val help = TextView(this).apply {
            text = "Nhập URL proxy HTTP, ví dụ http://192.168.1.10:8080. Proxy phải hỗ trợ HTTP CONNECT để truy cập HTTPS. Không nhập URL của một trang web proxy (web proxy) trừ khi dịch vụ cung cấp endpoint tương thích HTTP proxy."
            textSize = 13f
            setPadding(0, dp(8), 0, dp(8))
        }
        content.addView(help)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("Hủy", null)
            .setNeutralButton("Tắt proxy", null)
            .setPositiveButton("Lưu", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val normalizedHome = normalizeHttpUrl(home.text.toString().trim())
                if (normalizedHome == null) {
                    home.error = "Nhập URL HTTP/HTTPS hợp lệ"
                    return@setOnClickListener
                }
                val rawProxy = proxyUrl.text.toString().trim()
                if (enabled.isChecked && parseProxyUrl(rawProxy) == null) {
                    proxyUrl.error = "Dùng dạng http://host:port"
                    return@setOnClickListener
                }
                prefs.edit()
                    .putBoolean("proxy_enabled", enabled.isChecked)
                    .putString("proxy_url", rawProxy)
                    .putString("home_url", normalizedHome)
                    .apply()
                dialog.dismiss()
                applySavedProxy { success ->
                    updateProxySummary()
                    status.text = if (success) "Đã lưu cài đặt" else "WebView không hỗ trợ cấu hình proxy"
                    Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    webView.reload()
                }
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                enabled.isChecked = false
                prefs.edit().putBoolean("proxy_enabled", false).apply()
                dialog.dismiss()
                applySavedProxy { success ->
                    updateProxySummary()
                    status.text = if (success) "Đã tắt proxy" else "Không thể đổi proxy trên WebView này"
                }
            }
        }
        dialog.show()
    }

    private fun parseProxyUrl(raw: String): Pair<String, Int>? {
        return try {
            val uri = URI(raw)
            if (!uri.scheme.equals("http", ignoreCase = true) || uri.host.isNullOrBlank()) return null
            if (uri.rawUserInfo != null || (uri.rawPath != null && uri.rawPath.isNotEmpty() && uri.rawPath != "/") || uri.rawQuery != null || uri.rawFragment != null) return null
            val port = if (uri.port == -1) 8080 else uri.port
            if (port !in 1..65535) null else uri.host to port
        } catch (_: Exception) {
            null
        }
    }

    private fun applySavedProxy(callback: (Boolean) -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            callback(false)
            return
        }
        val enabled = prefs.getBoolean("proxy_enabled", false)
        val raw = prefs.getString("proxy_url", "") ?: ""
        val executor = mainExecutor
        if (!enabled) {
            ProxyController.getInstance().clearProxyOverride(Runnable { callback(true) }, executor)
            return
        }
        val endpoint = parseProxyUrl(raw)
        if (endpoint == null) {
            callback(false)
            return
        }
        val config = ProxyConfig.Builder().addProxyRule("${endpoint.first}:${endpoint.second}").build()
        ProxyController.getInstance().setProxyOverride(config, Runnable { callback(true) }, executor)
    }

    private fun updateProxySummary() {
        proxySummary.text = if (prefs.getBoolean("proxy_enabled", false)) {
            "Proxy: ${prefs.getString("proxy_url", "") ?: ""}"
        } else {
            "Proxy: Tắt"
        }
    }

    private fun normalizeHttpUrl(raw: String): String? {
        val candidate = if (raw.contains("://")) raw else "https://$raw"
        return try {
            val uri = URI(candidate)
            if ((uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null) candidate else null
        } catch (_: Exception) { null }
    }

    private fun navigate() {
        val input = address.text.toString().trim()
        if (input.isBlank()) return
        val target = if (input.contains("://")) input else if (input.contains(" ") || !input.contains(".")) {
            "https://www.google.com/search?q=" + java.net.URLEncoder.encode(input, "UTF-8")
        } else "https://$input"
        val normalized = normalizeHttpUrl(target)
        if (normalized == null) {
            Toast.makeText(this, "Chỉ hỗ trợ URL HTTP/HTTPS hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }
        webView.loadUrl(normalized)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
