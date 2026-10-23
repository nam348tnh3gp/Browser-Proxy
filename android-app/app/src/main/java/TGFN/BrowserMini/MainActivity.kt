package TGFN.BrowserMini

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private lateinit var pageTitle: TextView
    private lateinit var proxySummary: TextView
    private lateinit var progress: ProgressBar

    private val prefs by lazy { getSharedPreferences("browser_settings", Context.MODE_PRIVATE) }
    private val mainExecutor = Executor { command -> runOnUiThread(command) }

    private val ink = Color.rgb(25, 36, 61)
    private val muted = Color.rgb(103, 116, 139)
    private val accent = Color.rgb(48, 91, 230)
    private val canvas = Color.rgb(243, 246, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(243, 246, 252)
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
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
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                status.text = "Đang tải trang…"
                if (!url.isNullOrBlank()) address.setText(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = "Sẵn sàng"
                if (!url.isNullOrBlank()) address.setText(url)
                pageTitle.text = view?.title?.takeIf { it.isNotBlank() } ?: hostLabel(url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    status.text = "Không tải được trang: ${error?.description ?: "lỗi không xác định"}"
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                if (newProgress == 100) {
                    pageTitle.text = view?.title?.takeIf { it.isNotBlank() } ?: hostLabel(view?.url)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) pageTitle.text = title
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(6))
            setBackgroundColor(canvas)
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(8))
        }
        val brandMark = TextView(this).apply {
            text = "◈"
            textSize = 27f
            setTextColor(accent)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            contentDescription = "TGFN Browser logo"
        }
        brandRow.addView(brandMark, LinearLayout.LayoutParams(dp(38), dp(40)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandText.addView(TextView(this).apply {
            text = "TGFN Browser"
            textSize = 18f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        brandText.addView(TextView(this).apply {
            text = "NHẸ · NHANH · RIÊNG TƯ"
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(muted)
        })
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        brandRow.addView(iconButton("⚙", "Cài đặt", accent) { showSettings() })
        root.addView(brandRow)

        val addressCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(5), dp(4))
            background = rounded(Color.WHITE, dp(18), Color.rgb(225, 231, 243), dp(1))
            elevation = dp(2).toFloat()
        }
        val secureMark = TextView(this).apply {
            text = "⌕"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(accent)
            contentDescription = "Địa chỉ hoặc tìm kiếm"
        }
        addressCard.addView(secureMark, LinearLayout.LayoutParams(dp(30), dp(42)))
        address = EditText(this).apply {
            hint = "Tìm kiếm hoặc nhập địa chỉ"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            textSize = 14f
            setTextColor(ink)
            setHintTextColor(Color.rgb(145, 155, 174))
            setPadding(dp(4), 0, dp(4), 0)
            background = null
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSelectAllOnFocus(false)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate()
                    true
                } else false
            }
        }
        addressCard.addView(address, LinearLayout.LayoutParams(0, dp(42), 1f))
        addressCard.addView(textButton("Đi  ↗", Color.WHITE, accent, dp(15)) { navigate() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)))
        root.addView(addressCard)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(222, 229, 244))
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))

        val toolbarScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(iconButton("‹", "Quay lại") { if (webView.canGoBack()) webView.goBack() })
        toolbar.addView(iconButton("›", "Tiến") { if (webView.canGoForward()) webView.goForward() })
        toolbar.addView(iconButton("⌂", "Trang chủ") {
            webView.loadUrl(prefs.getString("home_url", "https://www.google.com") ?: "https://www.google.com")
        })
        toolbar.addView(iconButton("↻", "Tải lại") { webView.reload() })
        toolbar.addView(iconButton("☆", "Lưu trang") { toggleBookmark() })
        toolbar.addView(iconButton("▤", "Dấu trang") { showBookmarks() })
        toolbar.addView(iconButton("↗", "Chia sẻ") { shareCurrentPage() })
        toolbarScroll.addView(toolbar)
        root.addView(toolbarScroll)

        val titleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(0), dp(4), dp(7))
        }
        pageTitle = TextView(this).apply {
            text = "Đang mở trình duyệt…"
            textSize = 13f
            setTextColor(ink)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = Typeface.DEFAULT_BOLD
        }
        titleCard.addView(pageTitle)
        root.addView(titleCard)

        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(8), dp(5), dp(2))
        }
        status = TextView(this).apply {
            text = "Sẵn sàng"
            textSize = 11f
            setTextColor(muted)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        footer.addView(status, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        proxySummary = TextView(this).apply {
            textSize = 10f
            setTextColor(accent)
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = rounded(Color.rgb(230, 237, 255), dp(20))
        }
        footer.addView(proxySummary)
        root.addView(footer)
        setContentView(root)
    }

    private fun iconButton(symbol: String, label: String, color: Int = accent, onClick: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = rounded(Color.WHITE, dp(15), Color.rgb(231, 236, 246), dp(1))
            isClickable = true
            isFocusable = true
            contentDescription = label
            setOnClickListener { onClick() }
        }
        item.addView(TextView(this).apply {
            text = symbol
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(color)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(dp(34), dp(31)))
        item.addView(TextView(this).apply {
            text = label
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(muted)
            maxLines = 1
        })
        val params = LinearLayout.LayoutParams(dp(64), dp(62))
        params.marginEnd = dp(6)
        item.layoutParams = params
        return item
    }

    private fun textButton(label: String, textColor: Int, fillColor: Int, radiusDp: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setPadding(dp(13), 0, dp(13), 0)
            background = rounded(fillColor, dp(radiusDp))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (stroke != null) setStroke(strokeWidth, stroke)
        }
    }

    private fun showSettings() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        content.addView(TextView(this).apply {
            text = "Cài đặt trình duyệt"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, 0, 0, dp(12))
        })

        val enabled = android.widget.CheckBox(this).apply {
            text = "Bật HTTP proxy"
            isChecked = prefs.getBoolean("proxy_enabled", false)
        }
        content.addView(enabled)

        val proxyUrl = EditText(this).apply {
            hint = "Proxy URL: http://host:port"
            setSingleLine(true)
            setText(prefs.getString("proxy_url", "") ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        content.addView(proxyUrl)

        val home = EditText(this).apply {
            hint = "Trang chủ URL"
            setSingleLine(true)
            setText(prefs.getString("home_url", "https://www.google.com") ?: "")
        }
        content.addView(home)

        content.addView(TextView(this).apply {
            text = "Proxy chỉ được sử dụng khi bật. Khi tắt, trình duyệt kết nối Internet trực tiếp. Endpoint cần hỗ trợ HTTP CONNECT để tải HTTPS."
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(8))
        })

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

    private fun toggleBookmark() {
        val url = webView.url ?: return
        val bookmarks = (prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()).toMutableSet()
        if (bookmarks.remove(url)) {
            prefs.edit().putStringSet("bookmarks", bookmarks).apply()
            Toast.makeText(this, "Đã xóa dấu trang", Toast.LENGTH_SHORT).show()
        } else {
            bookmarks.add(url)
            prefs.edit().putStringSet("bookmarks", bookmarks).apply()
            Toast.makeText(this, "Đã lưu dấu trang", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        val bookmarks = (prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()).toList().sorted()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "Chưa có dấu trang. Nhấn ☆ Lưu trang để thêm.", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Dấu trang đã lưu")
            .setItems(bookmarks.toTypedArray()) { _, which ->
                webView.loadUrl(bookmarks[which])
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun shareCurrentPage() {
        val url = webView.url ?: return
        val title = webView.title ?: url
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        startActivity(Intent.createChooser(send, "Chia sẻ trang bằng"))
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
        if (!enabled) {
            ProxyController.getInstance().clearProxyOverride(mainExecutor, Runnable { callback(true) })
            return
        }
        val endpoint = parseProxyUrl(raw)
        if (endpoint == null) {
            callback(false)
            return
        }
        val config = ProxyConfig.Builder().addProxyRule("${endpoint.first}:${endpoint.second}").build()
        ProxyController.getInstance().setProxyOverride(config, mainExecutor, Runnable { callback(true) })
    }

    private fun updateProxySummary() {
        val enabled = prefs.getBoolean("proxy_enabled", false)
        proxySummary.text = if (enabled) "● PROXY BẬT" else "● TRỰC TIẾP"
        proxySummary.setTextColor(if (enabled) accent else Color.rgb(44, 132, 91))
        proxySummary.background = rounded(
            if (enabled) Color.rgb(230, 237, 255) else Color.rgb(225, 246, 235),
            dp(20)
        )
    }

    private fun normalizeHttpUrl(raw: String): String? {
        val candidate = if (raw.contains("://")) raw else "https://$raw"
        return try {
            val uri = URI(candidate)
            if ((uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null) candidate else null
        } catch (_: Exception) {
            null
        }
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

    private fun hostLabel(url: String?): String {
        return try {
            URI(url ?: "").host ?: "TGFN Browser"
        } catch (_: Exception) {
            "TGFN Browser"
        }
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
