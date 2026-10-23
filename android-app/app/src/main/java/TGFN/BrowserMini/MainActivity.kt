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
import android.widget.EditText
import android.widget.FrameLayout
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
    private data class BrowserTab(val webView: WebView, var title: String = "Tab mới", var url: String = "")

    private val tabs = mutableListOf<BrowserTab>()
    private var selectedTab = 0
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var tabButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var webContainer: FrameLayout
    private lateinit var proxySummary: TextView

    private val prefs by lazy { getSharedPreferences("browser_settings", Context.MODE_PRIVATE) }
    private val mainExecutor = Executor { command -> runOnUiThread(command) }
    private val ink = Color.rgb(32, 33, 36)
    private val muted = Color.rgb(95, 99, 104)
    private val accent = Color.rgb(26, 115, 232)
    private val canvas = Color.rgb(255, 255, 255)
    private val pale = Color.rgb(241, 243, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()

        val savedUrls = savedInstanceState?.getStringArrayList("tab_urls")
        val restoreIndex = savedInstanceState?.getInt("selected_tab", 0) ?: 0
        applySavedProxy { success ->
            updateProxySummary()
            if (!success && prefs.getBoolean("proxy_enabled", false)) {
                status.text = "WebView không hỗ trợ cấu hình proxy"
            }
            if (!savedUrls.isNullOrEmpty()) {
                savedUrls.forEachIndexed { index, url -> createTab(url, index == restoreIndex) }
                selectedTab = restoreIndex.coerceIn(0, tabs.lastIndex)
                selectTab(selectedTab)
            } else {
                createTab(null, true)
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(canvas)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.WHITE)
        }

        val logo = TextView(this).apply {
            text = "◉"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            gravity = Gravity.CENTER
            contentDescription = "TGFN Browser"
        }
        topBar.addView(logo, LinearLayout.LayoutParams(dp(36), dp(42)))

        val addressCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(5), 0)
            background = rounded(pale, dp(28))
        }
        val security = TextView(this).apply {
            text = "⌕"
            textSize = 22f
            setTextColor(muted)
            gravity = Gravity.CENTER
            contentDescription = "Địa chỉ trang"
        }
        addressCard.addView(security, LinearLayout.LayoutParams(dp(25), dp(42)))

        address = EditText(this).apply {
            hint = "Tìm kiếm hoặc nhập URL"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            textSize = 14f
            setTextColor(ink)
            setHintTextColor(Color.rgb(95, 99, 104))
            setPadding(dp(3), 0, dp(3), 0)
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
        addressCard.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        val refresh = smallAction("↻", "Tải lại") { currentWebView()?.reload() }
        addressCard.addView(refresh, LinearLayout.LayoutParams(dp(38), dp(42)))
        topBar.addView(addressCard, LinearLayout.LayoutParams(0, dp(46), 1f))

        tabButton = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ink)
            background = rounded(Color.WHITE, dp(8), Color.rgb(95, 99, 104), dp(1))
            isClickable = true
            isFocusable = true
            contentDescription = "Quản lý tab"
            setOnClickListener { showTabs() }
        }
        val tabParams = LinearLayout.LayoutParams(dp(34), dp(32))
        tabParams.marginStart = dp(8)
        topBar.addView(tabButton, tabParams)

        val menuButton = smallAction("⋮", "Menu") { showBrowserMenu() }
        topBar.addView(menuButton, LinearLayout.LayoutParams(dp(34), dp(42)))
        root.addView(topBar)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(232, 234, 237))
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))

        webContainer = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(webContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        bottomBar.addView(bottomAction("‹", "Quay lại") { currentWebView()?.let { if (it.canGoBack()) it.goBack() } })
        bottomBar.addView(bottomAction("›", "Tiến") { currentWebView()?.let { if (it.canGoForward()) it.goForward() } })
        bottomBar.addView(bottomAction("⌂", "Trang chủ") { loadHome() })
        bottomBar.addView(bottomAction("☆", "Dấu trang") { showBookmarks() })
        bottomBar.addView(bottomAction("+", "Tab mới") { createTab(null, true) })
        bottomBar.addView(bottomAction("↗", "Chia sẻ") { shareCurrentPage() })
        root.addView(bottomBar)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(5))
            setBackgroundColor(Color.WHITE)
        }
        status = TextView(this).apply {
            text = "Sẵn sàng"
            textSize = 11f
            setTextColor(muted)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        statusRow.addView(status, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        proxySummary = TextView(this).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(Color.rgb(232, 245, 233), dp(16))
        }
        statusRow.addView(proxySummary)
        root.addView(statusRow)
        setContentView(root)
    }

    private fun createTab(url: String?, select: Boolean) {
        val web = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.userAgentString = if (isDesktopModeEnabled()) DESKTOP_USER_AGENT else null
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
        }
        val tab = BrowserTab(web, "Tab mới", url ?: "")
        tabs.add(tab)
        webContainer.addView(web, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                tab.url = url ?: tab.url
                tab.title = "Đang tải…"
                if (isSelected(tab)) {
                    status.text = "Đang tải trang…"
                    if (!url.isNullOrBlank()) setAddress(url)
                    progress.visibility = View.VISIBLE
                }
                updateTabButton()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                tab.url = url ?: tab.url
                tab.title = view?.title?.takeIf { it.isNotBlank() } ?: hostLabel(url)
                if (isSelected(tab)) {
                    status.text = "Sẵn sàng"
                    if (!url.isNullOrBlank()) setAddress(if (url == "https://tgfn.browser/") "" else url)
                }
                rememberHistory(tab)
                updateTabButton()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true && isSelected(tab)) {
                    status.text = "Không tải được trang: ${error?.description ?: "lỗi không xác định"}"
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (isSelected(tab)) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) tab.title = title
                updateTabButton()
            }
        }
        if (select) selectTab(tabs.lastIndex)
        if (url.isNullOrBlank() || url == "https://tgfn.browser/") loadNewTabPage(web) else web.loadUrl(url)
    }

    private fun loadNewTabPage(web: WebView) {
        web.loadDataWithBaseURL("https://tgfn.browser/", newTabHtml(), "text/html", "UTF-8", null)
    }

    private fun newTabHtml(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        *{box-sizing:border-box}body{margin:0;background:#fff;color:#202124;font:16px Roboto,'Arial',sans-serif}
        .wrap{max-width:560px;margin:0 auto;padding:12vh 22px 32px}.brand{text-align:center;font-size:34px;font-weight:700;letter-spacing:-1.5px;color:#1a73e8;margin-bottom:26px}
        .brand span{color:#188038}.search{display:flex;gap:10px;border:1px solid #dadce0;border-radius:28px;padding:7px 14px;box-shadow:0 1px 4px #0000000d}
        input{border:0;outline:0;flex:1;min-width:0;font-size:16px;background:transparent;color:#202124}.go{border:0;background:#1a73e8;color:#fff;border-radius:20px;padding:9px 14px;font-size:14px}
        .grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px 10px;margin-top:34px}.item{text-decoration:none;color:#3c4043;text-align:center;font-size:12px}
        .circle{width:52px;height:52px;border-radius:18px;background:#f1f3f4;margin:0 auto 9px;display:flex;align-items:center;justify-content:center;font-size:23px;color:#1a73e8;font-weight:600}
        .note{text-align:center;color:#80868b;font-size:12px;margin-top:36px}
        </style></head><body><div class="wrap"><div class="brand">TGFN <span>Browser</span></div>
        <form class="search" action="https://www.google.com/search" method="get"><input name="q" placeholder="Tìm kiếm trên Google" autofocus><button class="go" type="submit">Tìm</button></form>
        <div class="grid">
        <a class="item" href="https://www.google.com"><div class="circle">G</div>Google</a>
        <a class="item" href="https://www.youtube.com"><div class="circle">▶</div>YouTube</a>
        <a class="item" href="https://www.wikipedia.org"><div class="circle">W</div>Wikipedia</a>
        <a class="item" href="https://github.com"><div class="circle">⌘</div>GitHub</a>
        <a class="item" href="https://www.reddit.com"><div class="circle">r</div>Reddit</a>
        <a class="item" href="https://www.bing.com"><div class="circle">b</div>Bing</a>
        </div><div class="note">Tìm kiếm nhanh · Duyệt web theo cách của bạn</div></div></body></html>
    """.trimIndent()

    private fun isSelected(tab: BrowserTab): Boolean = tabs.getOrNull(selectedTab) === tab

    private fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        selectedTab = index
        tabs.forEachIndexed { i, tab -> tab.webView.visibility = if (i == index) View.VISIBLE else View.GONE }
        val tab = tabs[index]
        setAddress(tab.webView.url ?: tab.url)
        status.text = tab.title
        progress.visibility = View.GONE
        updateTabButton()
    }

    private fun updateTabButton() {
        if (::tabButton.isInitialized) tabButton.text = tabs.size.toString()
    }

    private fun currentWebView(): WebView? = tabs.getOrNull(selectedTab)?.webView

    private fun setAddress(value: String?) {
        if (!::address.isInitialized) return
        val text = value.orEmpty()
        if (address.text.toString() != text) address.setText(text)
    }

    private fun smallAction(symbol: String, label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = symbol
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(ink)
            contentDescription = label
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun bottomAction(symbol: String, label: String, action: () -> Unit): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(3), dp(2), dp(2))
            isClickable = true
            isFocusable = true
            contentDescription = label
            setOnClickListener { action() }
        }
        item.addView(TextView(this).apply {
            text = symbol
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(if (label == "Tab mới") accent else ink)
            typeface = Typeface.DEFAULT
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(31)))
        item.addView(TextView(this).apply {
            text = label
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(muted)
            maxLines = 1
        })
        item.layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        return item
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (stroke != null) setStroke(dp(strokeWidth), stroke)
        }

    private fun showBrowserMenu() {
        val desktopLabel = if (isDesktopModeEnabled()) "Phiên bản máy tính: đang bật" else "Phiên bản máy tính"
        val items = arrayOf(
            "Tab mới", "Dịch trang", "Tìm trong trang", "Thu phóng",
            desktopLabel, "Lưu/Bỏ dấu trang", "Dấu trang", "Lịch sử",
            "Chia sẻ trang", "Cài đặt", "Đóng tab hiện tại"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("TGFN Browser · Tiện ích")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> createTab(null, true)
                    1 -> translateCurrentPage()
                    2 -> showFindInPage()
                    3 -> showZoomControls()
                    4 -> toggleDesktopMode()
                    5 -> toggleBookmark()
                    6 -> showBookmarks()
                    7 -> showHistory()
                    8 -> shareCurrentPage()
                    9 -> showSettings()
                    10 -> closeCurrentTab()
                }
            }
            .show()
    }

    private fun showTabs() {
        if (tabs.isEmpty()) {
            createTab(null, true)
            return
        }

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(12))
        }
        val heading = TextView(this).apply {
            text = "Tab đang mở"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        }
        header.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = tabs.size.toString()
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(Color.rgb(232, 240, 254), dp(16))
            setPadding(dp(12), dp(6), dp(12), dp(6))
        })
        dialogContent.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun renderCards() {
            grid.removeAllViews()
            var index = 0
            while (index < tabs.size) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }
                for (column in 0..1) {
                    val tabIndex = index + column
                    if (tabIndex < tabs.size) {
                        val tab = tabs[tabIndex]
                        val selected = tabIndex == selectedTab
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(12), dp(10), dp(10), dp(10))
                            background = rounded(
                                if (selected) Color.rgb(232, 240, 254) else Color.rgb(248, 249, 250),
                                dp(16),
                                if (selected) accent else Color.rgb(218, 220, 224),
                                if (selected) 2 else 1
                            )
                            isClickable = true
                            isFocusable = true
                            elevation = if (selected) dp(2).toFloat() else 0f
                            setOnClickListener {
                                selectTab(tabIndex)
                                val parentDialog = dialogContent.tag as? android.app.AlertDialog
                                parentDialog?.dismiss()
                            }
                        }
                        val titleRow = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        titleRow.addView(TextView(this).apply {
                            text = if (selected) "●" else "○"
                            textSize = 14f
                            setTextColor(if (selected) accent else muted)
                            gravity = Gravity.CENTER
                        }, LinearLayout.LayoutParams(dp(20), dp(24)))
                        titleRow.addView(TextView(this).apply {
                            text = "Tab " + (tabIndex + 1)
                            textSize = 11f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(muted)
                        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        val close = TextView(this).apply {
                            text = "×"
                            textSize = 22f
                            gravity = Gravity.CENTER
                            setTextColor(muted)
                            contentDescription = "Đóng tab " + (tabIndex + 1)
                            setPadding(dp(6), 0, dp(4), 0)
                            setOnClickListener {
                                closeTabAt(tabIndex)
                                val parentDialog = dialogContent.tag as? android.app.AlertDialog
                                if (tabs.isEmpty()) createTab(null, true)
                                if (tabs.isEmpty()) parentDialog?.dismiss() else {
                                    renderCards()
                                    heading.text = "Tab đang mở"
                                    updateTabButton()
                                }
                            }
                        }
                        titleRow.addView(close, LinearLayout.LayoutParams(dp(30), dp(28)))
                        card.addView(titleRow)

                        card.addView(TextView(this).apply {
                            text = tab.title.takeIf { it.isNotBlank() } ?: "Tab mới"
                            textSize = 14f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ink)
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            setPadding(0, dp(8), 0, dp(4))
                        })
                        card.addView(TextView(this).apply {
                            text = hostLabel(tab.webView.url ?: tab.url).take(38)
                            textSize = 11f
                            setTextColor(muted)
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END
                        })
                        val params = LinearLayout.LayoutParams(0, dp(132), 1f)
                        if (column == 0) params.marginEnd = dp(6) else params.marginStart = dp(6)
                        params.bottomMargin = dp(12)
                        row.addView(card, params)
                    } else {
                        row.addView(View(this), LinearLayout.LayoutParams(0, dp(132), 1f))
                    }
                }
                grid.addView(row)
                index += 2
            }
        }
        scroll.addView(grid)
        dialogContent.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val newTab = TextView(this).apply {
            text = "+  Tab mới"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(accent, dp(24))
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setOnClickListener {
                val parentDialog = dialogContent.tag as? android.app.AlertDialog
                parentDialog?.dismiss()
                createTab(null, true)
            }
        }
        footer.addView(newTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        val done = TextView(this).apply {
            text = "Xong"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        footer.addView(done)
        dialogContent.addView(footer)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogContent)
            .create()
        dialogContent.tag = dialog
        done.setOnClickListener { dialog.dismiss() }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.78f).toInt()
            )
            dialog.window?.setGravity(Gravity.BOTTOM)
            dialog.window?.attributes = dialog.window?.attributes?.apply {
                windowAnimations = android.R.style.Animation_InputMethod
            }
            dialogContent.alpha = 0f
            dialogContent.translationY = dp(24).toFloat()
            dialogContent.animate().alpha(1f).translationY(0f).setDuration(180).start()
        }
        renderCards()
        dialog.show()
    }

    private fun closeTabAt(index: Int) {
        if (index !in tabs.indices) return
        val removed = tabs.removeAt(index)
        (removed.webView.parent as? ViewGroup)?.removeView(removed.webView)
        removed.webView.stopLoading()
        removed.webView.destroy()
        if (tabs.isEmpty()) {
            selectedTab = 0
        } else {
            if (index < selectedTab) selectedTab--
            else if (index == selectedTab) selectedTab = (index - 1).coerceAtLeast(0)
            selectedTab = selectedTab.coerceIn(0, tabs.lastIndex)
            selectTab(selectedTab)
        }
        updateTabButton()
    }

    private fun closeCurrentTab() {
        if (tabs.isEmpty()) return
        closeTabAt(selectedTab)
        if (tabs.isEmpty()) createTab(null, true)
        updateTabButton()
    }

    private fun isDesktopModeEnabled(): Boolean = prefs.getBoolean("desktop_mode", false)

    private fun toggleDesktopMode() {
        val enabled = !isDesktopModeEnabled()
        prefs.edit().putBoolean("desktop_mode", enabled).apply()
        tabs.forEach { tab ->
            tab.webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
        }
        val current = currentWebView()
        if (current != null) {
            val url = current.url ?: ""
            if (url == "https://tgfn.browser/") loadNewTabPage(current)
            else if (url.startsWith("http://") || url.startsWith("https://")) current.reload()
        }
        status.text = if (enabled) "Đã bật phiên bản máy tính" else "Đã tắt phiên bản máy tính"
        Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
    }

    private fun translateCurrentPage() {
        val web = currentWebView() ?: return
        val url = web.url.orEmpty()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Mở một trang web trước khi dịch", Toast.LENGTH_SHORT).show()
            return
        }
        val languages = arrayOf("Tiếng Việt", "English", "日本語", "한국어", "Français", "Español")
        val codes = arrayOf("vi", "en", "ja", "ko", "fr", "es")
        android.app.AlertDialog.Builder(this)
            .setTitle("Dịch ngay trên trang")
            .setItems(languages) { _, which -> translatePageInPlace(web, codes[which], languages[which]) }
            .setNeutralButton("Hiện bản gốc") { _, _ -> restoreOriginalPage(web) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun translatePageInPlace(web: WebView, targetLanguage: String, languageLabel: String) {
        val collectScript = """
            (function() {
              try {
                if (window.__tgfnTranslateNodes) {
                  window.__tgfnTranslateNodes.forEach(function(item) {
                    if (item.node && item.node.isConnected) item.node.nodeValue = item.original;
                  });
                }
                var walker = document.createTreeWalker(document.body || document.documentElement, NodeFilter.SHOW_TEXT);
                var nodes = [], node;
                var excluded = /^(SCRIPT|STYLE|NOSCRIPT|TEXTAREA|INPUT|SELECT|OPTION|CODE|PRE|SVG|BUTTON)$/;
                while ((node = walker.nextNode()) && nodes.length < 36) {
                  var parent = node.parentElement;
                  if (!parent || excluded.test(parent.tagName) ||
                      parent.closest('[contenteditable="true"],[translate="no"],[aria-hidden="true"]')) continue;
                  var text = node.nodeValue || '';
                  if (text.trim().length < 2 || text.trim().length > 350) continue;
                  if (!parent.getClientRects().length) continue;
                  nodes.push({node: node, original: text});
                }
                window.__tgfnTranslateNodes = nodes;
                return JSON.stringify(nodes.map(function(item) { return item.original; }));
              } catch (e) { return '[]'; }
            })();
        """.trimIndent()
        status.text = "Đang chuẩn bị dịch…"
        web.evaluateJavascript(collectScript) { raw ->
            try {
                val decoded = org.json.JSONTokener(raw ?: "null").nextValue() as? String ?: "[]"
                val originals = org.json.JSONArray(decoded)
                if (originals.length() == 0) {
                    status.text = "Không tìm thấy văn bản để dịch"
                    Toast.makeText(this, "Không tìm thấy văn bản phù hợp trên trang", Toast.LENGTH_LONG).show()
                    return@evaluateJavascript
                }
                status.text = "Đang dịch " + originals.length() + " đoạn…"
                Thread {
                    val translated = org.json.JSONArray()
                    for (index in 0 until originals.length()) translated.put("")
                    val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
                    try {
                        val jobs = (0 until originals.length()).map { index ->
                            pool.submit<Pair<Int, String>> {
                                index to translateText(originals.getString(index), targetLanguage)
                            }
                        }
                        jobs.forEach { job ->
                            try {
                                val (index, value) = job.get()
                                if (value.isNotBlank()) translated.put(index, value)
                            } catch (_: Exception) { }
                        }
                    } finally {
                        pool.shutdownNow()
                    }
                    runOnUiThread {
                        if (isFinishing || web !== currentWebView()) return@runOnUiThread
                        val payload = translated.toString()
                        val applyScript = """
                            (function(values) {
                              var nodes = window.__tgfnTranslateNodes || [], count = 0;
                              nodes.forEach(function(item, i) {
                                if (item.node && item.node.isConnected && values[i]) {
                                  item.node.nodeValue = values[i]; count++;
                                }
                              });
                              return count;
                            })($payload);
                        """.trimIndent()
                        web.evaluateJavascript(applyScript) { result ->
                            val count = result?.toIntOrNull() ?: 0
                            status.text = if (count > 0) "Đã dịch " + count + " đoạn · " + languageLabel else "Không dịch được nội dung"
                            Toast.makeText(this, if (count > 0) "Đã dịch ngay trên trang. Dùng Menu → Dịch trang → Hiện bản gốc để hoàn tác." else "Dịch thất bại. Kiểm tra kết nối mạng rồi thử lại.", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            } catch (e: Exception) {
                status.text = "Không thể chuẩn bị nội dung dịch"
                Toast.makeText(this, "Không thể đọc văn bản của trang này", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun translateText(text: String, targetLanguage: String): String {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val query = java.net.URLEncoder.encode(text, "UTF-8")
            val url = java.net.URL(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + targetLanguage + "&dt=t&q=" + query
            )
            connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
            }
            if (connection.responseCode !in 200..299) return ""
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = org.json.JSONArray(body)
            val segments = root.optJSONArray(0) ?: return ""
            buildString {
                for (i in 0 until segments.length()) {
                    val segment = segments.optJSONArray(i)
                    if (segment != null) append(segment.optString(0))
                }
            }
        } catch (_: Exception) {
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private fun restoreOriginalPage(web: WebView) {
        web.evaluateJavascript(
            "(function(){var nodes=window.__tgfnTranslateNodes||[],count=0;nodes.forEach(function(item){if(item.node&&item.node.isConnected){item.node.nodeValue=item.original;count++;}});window.__tgfnTranslateNodes=null;return count;})();"
        ) { result ->
            status.text = "Đã khôi phục bản gốc"
            Toast.makeText(this, "Đã khôi phục văn bản gốc", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFindInPage() {
        val web = currentWebView() ?: return
        val query = EditText(this).apply {
            hint = "Nhập từ hoặc cụm từ"
            setSingleLine(true)
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Tìm trong trang")
            .setView(query)
            .setNegativeButton("Đóng", null)
            .setPositiveButton("Tìm", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val term = query.text.toString().trim()
                if (term.isBlank()) {
                    query.error = "Nhập nội dung cần tìm"
                } else {
                    web.setFindListener { activeMatches, _, _ ->
                        Toast.makeText(
                            this,
                            if (activeMatches > 0) "Tìm thấy " + activeMatches + " kết quả" else "Không tìm thấy kết quả",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    web.findAllAsync(term)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showZoomControls() {
        val web = currentWebView() ?: return
        val options = arrayOf("Thu nhỏ", "Mức mặc định", "Phóng to")
        android.app.AlertDialog.Builder(this)
            .setTitle("Thu phóng trang")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> web.zoomOut()
                    1 -> {
                        web.settings.textZoom = 100
                        web.setInitialScale(0)
                        web.reload()
                    }
                    2 -> web.zoomIn()
                }
            }
            .setNegativeButton("Xong", null)
            .show()
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
        val desktop = android.widget.CheckBox(this).apply {
            text = "Yêu cầu phiên bản máy tính cho trang web"
            isChecked = isDesktopModeEnabled()
        }
        content.addView(desktop)
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
            text = "Proxy chỉ được dùng khi bật. Khi tắt, trình duyệt kết nối Internet trực tiếp. Proxy phải hỗ trợ HTTP CONNECT để tải HTTPS."
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
                prefs.edit().putBoolean("proxy_enabled", enabled.isChecked)
                    .putBoolean("desktop_mode", desktop.isChecked)
                    .putString("proxy_url", rawProxy).putString("home_url", normalizedHome).apply()
                tabs.forEach { tab ->
                    tab.webView.settings.userAgentString = if (desktop.isChecked) DESKTOP_USER_AGENT else null
                }
                dialog.dismiss()
                applySavedProxy { success ->
                    updateProxySummary()
                    status.text = if (success) "Đã lưu cài đặt" else "WebView không hỗ trợ cấu hình proxy"
                    Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
                    currentWebView()?.reload()
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
        val url = currentWebView()?.url ?: return
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
            Toast.makeText(this, "Chưa có dấu trang", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle("Dấu trang")
            .setItems(bookmarks.toTypedArray()) { _, which -> currentWebView()?.loadUrl(bookmarks[which]) }
            .setPositiveButton("Đóng", null).show()
    }

    private fun rememberHistory(tab: BrowserTab) {
        if (tab.url.isBlank() || !tab.url.startsWith("http")) return
        val history = (prefs.getStringSet("history", emptySet()) ?: emptySet()).toMutableSet()
        history.remove(tab.url)
        history.add(tab.url)
        prefs.edit().putStringSet("history", history.toList().takeLast(100).toSet()).apply()
    }

    private fun showHistory() {
        val history = (prefs.getStringSet("history", emptySet()) ?: emptySet()).toList().sortedDescending()
        if (history.isEmpty()) {
            Toast.makeText(this, "Chưa có lịch sử duyệt web", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle("Lịch sử")
            .setItems(history.toTypedArray()) { _, which -> currentWebView()?.loadUrl(history[which]) }
            .setPositiveButton("Xóa lịch sử") { _, _ ->
                prefs.edit().remove("history").apply()
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Đóng", null).show()
    }

    private fun shareCurrentPage() {
        val web = currentWebView() ?: return
        val url = web.url ?: return
        val title = web.title ?: url
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
            if (!uri.scheme.equals("http", true) || uri.host.isNullOrBlank()) return null
            if (uri.rawUserInfo != null || (uri.rawPath != null && uri.rawPath.isNotEmpty() && uri.rawPath != "/") ||
                uri.rawQuery != null || uri.rawFragment != null) return null
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
        if (!prefs.getBoolean("proxy_enabled", false)) {
            ProxyController.getInstance().clearProxyOverride(mainExecutor, Runnable { callback(true) })
            return
        }
        val endpoint = parseProxyUrl(prefs.getString("proxy_url", "") ?: "")
        if (endpoint == null) {
            callback(false)
            return
        }
        val config = ProxyConfig.Builder().addProxyRule("${endpoint.first}:${endpoint.second}").build()
        ProxyController.getInstance().setProxyOverride(config, mainExecutor, Runnable { callback(true) })
    }

    private fun updateProxySummary() {
        if (!::proxySummary.isInitialized) return
        val enabled = prefs.getBoolean("proxy_enabled", false)
        proxySummary.text = if (enabled) "● PROXY BẬT" else "● TRỰC TIẾP"
        proxySummary.setTextColor(if (enabled) accent else Color.rgb(24, 128, 56))
        proxySummary.background = rounded(if (enabled) Color.rgb(232, 240, 254) else Color.rgb(232, 245, 233), dp(16))
    }

    private fun loadHome() {
        val url = normalizeHttpUrl(prefs.getString("home_url", "https://www.google.com") ?: "https://www.google.com")
        if (url == null) loadNewTabPage(currentWebView() ?: return) else currentWebView()?.loadUrl(url)
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
        currentWebView()?.loadUrl(normalized)
    }

    private fun hostLabel(url: String?): String =
        try { URI(url ?: "").host ?: "TGFN Browser" } catch (_: Exception) { "TGFN Browser" }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("tab_urls", ArrayList(tabs.map { it.webView.url ?: it.url }))
        outState.putInt("selected_tab", selectedTab)
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val web = currentWebView()
        if (web != null && web.canGoBack()) web.goBack() else if (tabs.size > 1) closeCurrentTab() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { tab ->
            (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
            tab.webView.stopLoading()
            tab.webView.destroy()
        }
        tabs.clear()
        super.onDestroy()
    }
}
