package TGFN.BrowserMini

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.net.URI
import java.util.concurrent.Executor

class MainActivity : Activity() {
    private data class BrowserTab(val webView: WebView, var title: String = "Tab mới", var url: String = "", var isNewTab: Boolean = false)


    private val adDomains = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val hostsUrl = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

    private fun loadAdHosts() {
        if (!prefs.getBoolean("adblock_enabled", true)) return
        Thread {
            try {
                val cache = java.io.File(cacheDir, "stevenblack-hosts.txt")
                val fresh = !cache.exists() || System.currentTimeMillis() - cache.lastModified() > 24L * 60 * 60 * 1000
                val text = if (fresh) {
                    val conn = java.net.URL(hostsUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent", "TGFN-BrowserMini")
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    cache.writeText(body)
                    body
                } else cache.readText()
                val parsed = mutableSetOf<String>()
                text.lineSequence().forEach { line ->
                    val t = line.trim()
                    if (t.isEmpty() || t.startsWith("#")) return@forEach
                    val parts = t.split(Regex("\\s+"))
                    if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                        val host = parts[1].lowercase()
                        if (host != "localhost" && host.contains(".")) parsed.add(host)
                    }
                }
                adDomains.clear()
                adDomains.addAll(parsed)
                runOnUiThread { status.text = tr("AdBlock: ", "AdBlock: ") + parsed.size + tr(" domain", " domains") }
            } catch (_: Exception) { }
        }.start()
    }

    private fun isAdUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (!prefs.getBoolean("adblock_enabled", true)) return false
        val host = try { java.net.URI(url).host?.lowercase() } catch (_: Exception) { null } ?: return false
        if (adDomains.contains(host)) return true
        // parent domain match
        val parts = host.split(".")
        for (i in 0 until parts.size - 1) {
            if (adDomains.contains(parts.subList(i, parts.size).joinToString("."))) return true
        }
        return false
    }

    private val tabs = mutableListOf<BrowserTab>()
    private var selectedTab = 0
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var tabButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var webContainer: FrameLayout
    private lateinit var proxySummary: TextView
    private var browserRoot: View? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var previousSystemUiVisibility: Int = 0

    private val prefs by lazy { getSharedPreferences("browser_settings", Context.MODE_PRIVATE) }
    private val clientId: String by lazy {
        prefs.getString("client_id", null)?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{8,80}")) }
            ?: java.util.UUID.randomUUID().toString().also {
                prefs.edit().putString("client_id", it).apply()
            }
    }
    private val mainExecutor = Executor { command -> runOnUiThread(command) }
    private val darkMode: Boolean get() = prefs.getBoolean("dark_mode", false)
    private val isEnglish: Boolean get() = prefs.getString("language", "vi") == "en"
    private val ink: Int get() = if (darkMode) Color.rgb(232, 234, 237) else Color.rgb(32, 33, 36)
    private val muted: Int get() = if (darkMode) Color.rgb(176, 179, 184) else Color.rgb(95, 99, 104)
    private val accent = Color.rgb(26, 115, 232)
    private val canvas: Int get() = if (darkMode) Color.rgb(32, 33, 36) else Color.WHITE
    private val pale: Int get() = if (darkMode) Color.rgb(48, 49, 52) else Color.rgb(241, 243, 244)

    private fun tr(vietnamese: String, english: String): String = if (isEnglish) english else vietnamese

    private fun initializeBrowser(savedInstanceState: Bundle?) {
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()

        val savedUrls = savedInstanceState?.getStringArrayList("tab_urls")
            ?: loadPersistedSessionUrls()
        val restoreIndex = savedInstanceState?.getInt("selected_tab")
            ?: prefs.getInt("session_selected_tab", 0)
        applySavedProxy { success ->
            updateProxySummary()
            if (!success && prefs.getBoolean("proxy_enabled", false)) {
                status.text = tr("WebView không hỗ trợ cấu hình proxy", "This WebView does not support proxy configuration")
            }
            if (!savedUrls.isNullOrEmpty()) {
                savedUrls.forEachIndexed { index, url -> createTab(url, index == restoreIndex) }
                selectedTab = restoreIndex.coerceIn(0, tabs.lastIndex)
                selectTab(selectedTab)
                persistSession()
            } else {
                createTab(null, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // WebView derives prefers-color-scheme from the host application theme.
        // Select the matching theme before Activity/WebView initialization.
        setTheme(
            if (getSharedPreferences("browser_settings", Context.MODE_PRIVATE).getBoolean("dark_mode", false))
                android.R.style.Theme_Material_NoActionBar
            else android.R.style.Theme_Material_Light_NoActionBar
        )
        super.onCreate(savedInstanceState)
        if (!prefs.contains("language")) {
            var selectedLanguage = "vi"
            val languageContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(8), dp(24), dp(8))
            }
            languageContent.addView(TextView(this).apply {
                text = "Choose your language / Chọn ngôn ngữ"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, 0, 0, dp(12))
            })
            val languageChoices = android.widget.RadioGroup(this).apply {
                orientation = android.widget.RadioGroup.VERTICAL
            }
            val vietnameseOption = android.widget.RadioButton(this).apply {
                id = View.generateViewId()
                text = "Tiếng Việt"
                isChecked = true
            }
            val englishOption = android.widget.RadioButton(this).apply {
                id = View.generateViewId()
                text = "English"
            }
            languageChoices.addView(vietnameseOption)
            languageChoices.addView(englishOption)
            languageChoices.setOnCheckedChangeListener { _, checkedId ->
                selectedLanguage = if (checkedId == englishOption.id) "en" else "vi"
            }
            languageContent.addView(languageChoices)
            val languageDialog = android.app.AlertDialog.Builder(this)
                .setView(languageContent)
                .setPositiveButton("Continue / Tiếp tục") { _, _ ->
                    prefs.edit().putString("language", selectedLanguage).apply()
                    initializeBrowser(savedInstanceState)
                }
                .setCancelable(false)
                .create()
            languageDialog.setTitle("TGFN Browser")
            languageDialog.show()
        } else {
            initializeBrowser(savedInstanceState)
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
            setBackgroundColor(canvas)
        }

        val logo = TextView(this).apply {
            text = "◉"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            gravity = Gravity.CENTER
            contentDescription = "TGFN Browser"
        }
        lateinit var adblockBtn: TextView
        adblockBtn = smallAction("Ad", tr("AdBlock", "AdBlock")) {
            val on = !prefs.getBoolean("adblock_enabled", true)
            prefs.edit().putBoolean("adblock_enabled", on).apply()
            Toast.makeText(this, if (on) tr("Đã bật AdBlock", "AdBlock on") else tr("Đã tắt AdBlock", "AdBlock off"), Toast.LENGTH_SHORT).show()
            currentWebView()?.reload()
            adblockBtn.paintFlags = if (on) adblockBtn.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG else adblockBtn.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            adblockBtn.setTextColor(if (on) accent else muted)
        }
        // Ad with slash when enabled
        if (prefs.getBoolean("adblock_enabled", true)) {
            adblockBtn.paintFlags = adblockBtn.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        }
        adblockBtn.setTextColor(if (prefs.getBoolean("adblock_enabled", true)) accent else muted)
        adblockBtn.textSize = 12f
        adblockBtn.typeface = Typeface.DEFAULT_BOLD
        topBar.addView(adblockBtn, LinearLayout.LayoutParams(dp(36), dp(42)))

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
            contentDescription = tr("Địa chỉ trang", "Page address")
        }
        addressCard.addView(security, LinearLayout.LayoutParams(dp(25), dp(42)))

        address = EditText(this).apply {
            hint = tr("Tìm kiếm hoặc nhập URL", "Search or enter URL")
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
        val refresh = smallAction("↻", tr("Tải lại", "Reload")) { currentWebView()?.reload() }
        addressCard.addView(refresh, LinearLayout.LayoutParams(dp(38), dp(42)))
        topBar.addView(addressCard, LinearLayout.LayoutParams(0, dp(46), 1f))

        tabButton = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(ink)
            background = rounded(canvas, dp(8), Color.rgb(95, 99, 104), dp(1))
            isClickable = true
            isFocusable = true
            contentDescription = tr("Quản lý tab", "Manage tabs")
            setOnClickListener { showTabs() }
        }
        val tabParams = LinearLayout.LayoutParams(dp(34), dp(32))
        tabParams.marginStart = dp(8)
        topBar.addView(tabButton, tabParams)

        val menuButton = smallAction("⋮", tr("Menu", "Menu")) { showBrowserMenu() }
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

        webContainer = FrameLayout(this).apply { setBackgroundColor(canvas) }
        root.addView(webContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(canvas)
            elevation = dp(8).toFloat()
        }
        bottomBar.addView(bottomAction("‹", tr("Quay lại", "Back")) { currentWebView()?.let { if (it.canGoBack()) it.goBack() } })
        bottomBar.addView(bottomAction("›", tr("Tiến", "Forward")) { currentWebView()?.let { if (it.canGoForward()) it.goForward() } })
        bottomBar.addView(bottomAction("⌂", tr("Trang chủ", "Home")) { loadHome() })
        bottomBar.addView(bottomAction("☆", tr("Dấu trang", "Bookmarks")) { showBookmarks() })
        bottomBar.addView(bottomAction("+", tr("Tab mới", "New tab")) { createTab(null, true) })
        bottomBar.addView(bottomAction("↗", tr("Chia sẻ", "Share")) { shareCurrentPage() })
        root.addView(bottomBar)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(5))
            setBackgroundColor(canvas)
        }
        status = TextView(this).apply {
            text = tr("Sẵn sàng", "Ready")
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
        browserRoot = root
        setContentView(root)
        loadAdHosts()
    }

    private fun createTab(url: String?, select: Boolean, popupTransport: Message? = null) {
        val web = WebView(this).apply {
            setBackgroundColor(canvas)
            settings.javaScriptEnabled = true
            configureWebViewTheme(settings)
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            applyDataSaverSettings(settings)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.userAgentString = if (isDesktopModeEnabled()) DESKTOP_USER_AGENT else null
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
        }
        val requestedUrl = url?.trim().orEmpty()
        val shouldShowNewTab = popupTransport == null && (
            requestedUrl.isBlank() ||
                requestedUrl.equals("about:blank", ignoreCase = true) ||
                requestedUrl == NEW_TAB_SESSION_URL ||
                requestedUrl == "https://tgfn.browser/"
            )
        val tab = BrowserTab(
            webView = web,
            title = tr("Tab mới", "New tab"),
            url = if (shouldShowNewTab) NEW_TAB_SESSION_URL else requestedUrl.ifBlank { "about:blank" },
            isNewTab = shouldShowNewTab
        )
        tabs.add(tab)
        // Update the visible tab counter immediately, before WebView callbacks run.
        updateTabButton()
        webContainer.addView(web, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString()
                if (isAdUrl(url)) {
                    return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                    tab.isNewTab = url == "https://tgfn.browser/"
                }
                tab.url = if (tab.isNewTab) NEW_TAB_SESSION_URL else (url ?: tab.url)
                tab.title = "Đang tải…"
                if (isSelected(tab)) {
                    status.text = tr("Đang tải trang…", "Loading page…")
                    if (!url.isNullOrBlank()) setAddress(url)
                    progress.visibility = View.VISIBLE
                }
                updateTabButton()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                    tab.isNewTab = url == "https://tgfn.browser/"
                }
                tab.url = if (tab.isNewTab) NEW_TAB_SESSION_URL else (url ?: tab.url)
                tab.title = view?.title?.takeIf { it.isNotBlank() } ?: hostLabel(url)
                if (isSelected(tab)) {
                    status.text = tr("Sẵn sàng", "Ready")
                    if (!url.isNullOrBlank()) setAddress(if (tab.isNewTab) "" else url)
                }
                rememberHistory(tab)
                persistSession()
                updateTabButton()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true && isSelected(tab)) {
                    status.text = tr("Không tải được trang: ${error?.description ?: "lỗi không xác định"}", "Could not load page: ${error?.description ?: "unknown error"}")
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

            // Route target="_blank" and window.open() into a managed browser tab.
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (resultMsg == null) return false
                createTab(null, true, resultMsg)
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                val index = tabs.indexOfFirst { it.webView === window }
                if (index >= 0) {
                    closeTabAt(index)
                    if (tabs.isEmpty()) createTab(null, true)
                }
            }

            // Support HTML5 fullscreen requests (for example, video players).
            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (view == null || fullscreenView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                val decor = window.decorView as? FrameLayout
                if (decor == null) {
                    callback?.onCustomViewHidden()
                    return
                }

                previousSystemUiVisibility = decor.systemUiVisibility
                fullscreenView = view
                fullscreenCallback = callback
                browserRoot?.visibility = View.GONE
                decor.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                @Suppress("DEPRECATION")
                decor.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
            }

            override fun onHideCustomView() {
                hideFullscreenView()
            }
        }
        if (select) selectTab(tabs.lastIndex)
        if (popupTransport != null) {
            val transport = popupTransport.obj as? WebView.WebViewTransport
            if (transport == null) {
                closeTabAt(tabs.indexOf(tab))
                return
            }
            transport.webView = web
            popupTransport.sendToTarget()
        } else if (shouldShowNewTab) {
            loadNewTabPage(web)
        } else if (requestedUrl.isNotBlank()) {
            web.loadUrl(requestedUrl)
        }
    }

    /** Match web content darkening to the browser's own theme preference. */
    private fun configureWebViewTheme(settings: android.webkit.WebSettings) {
        if (darkMode) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }
    }

    private fun loadNewTabPage(web: WebView) {
        tabs.firstOrNull { it.webView === web }?.let { tab ->
            tab.isNewTab = true
            tab.url = NEW_TAB_SESSION_URL
            tab.title = tr("Tab mới", "New tab")
        }
        web.loadDataWithBaseURL("https://tgfn.browser/", newTabHtml(), "text/html", "UTF-8", null)
    }

    private fun newTabHtml(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        *{box-sizing:border-box}body{margin:0;background:${if (darkMode) "#202124" else "#fff"};color:${if (darkMode) "#e8eaed" else "#202124"};font:16px Roboto,'Arial',sans-serif}
        .wrap{max-width:560px;margin:0 auto;padding:12vh 22px 32px}.brand{text-align:center;font-size:34px;font-weight:700;letter-spacing:-1.5px;color:#1a73e8;margin-bottom:26px}
        .brand span{color:#188038}.search{display:flex;gap:10px;border:1px solid #dadce0;border-radius:28px;padding:7px 14px;box-shadow:0 1px 4px #0000000d}
        input{border:0;outline:0;flex:1;min-width:0;font-size:16px;background:transparent;color:${if (darkMode) "#e8eaed" else "#202124"}}.go{border:0;background:#1a73e8;color:#fff;border-radius:20px;padding:9px 14px;font-size:14px}
        .grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px 10px;margin-top:34px}.item{text-decoration:none;color:${if (darkMode) "#e8eaed" else "#3c4043"};text-align:center;font-size:12px}
        .circle{width:52px;height:52px;border-radius:18px;background:${if (darkMode) "#303134" else "#f1f3f4"};margin:0 auto 9px;display:flex;align-items:center;justify-content:center;overflow:hidden}.circle img{width:32px;height:32px;object-fit:contain}
        .note{text-align:center;color:#80868b;font-size:12px;margin-top:36px}
        </style></head><body><div class="wrap"><div class="brand">TGFN <span>Browser</span></div>
        <form class="search" action="https://www.google.com/search" method="get"><input name="q" placeholder="${tr("Tìm kiếm trên Google", "Search Google")}" autofocus><button class="go" type="submit">${tr("Tìm", "Search")}</button></form>
        <div class="grid">
        <a class="item" href="https://www.google.com"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=www.google.com&amp;sz=128" alt="Google"></div>Google</a>
        <a class="item" href="https://www.youtube.com"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=www.youtube.com&amp;sz=128" alt="YouTube"></div>YouTube</a>
        <a class="item" href="https://www.wikipedia.org"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=www.wikipedia.org&amp;sz=128" alt="Wikipedia"></div>Wikipedia</a>
        <a class="item" href="https://github.com"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=github.com&amp;sz=128" alt="GitHub"></div>GitHub</a>
        <a class="item" href="https://www.reddit.com"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=www.reddit.com&amp;sz=128" alt="Reddit"></div>Reddit</a>
        <a class="item" href="https://www.bing.com"><div class="circle"><img src="https://www.google.com/s2/favicons?domain=www.bing.com&amp;sz=128" alt="Bing"></div>Bing</a>
        </div><div class="note">${tr("Tìm kiếm nhanh · Duyệt web theo cách của bạn", "Quick search · Browse your way")}</div></div></body></html>
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
        persistSession()
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
            setTextColor(if (label == tr("Tab mới", "New tab")) accent else ink)
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
        val desktopLabel = if (isDesktopModeEnabled()) tr("Phiên bản máy tính: đang bật", "Desktop site: on") else tr("Phiên bản máy tính", "Desktop site")
        val dataSaverLabel = if (isDataSaverEnabled()) "Data Saver: " + tr("bật", "on") else "Data Saver: " + tr("tắt", "off")
        val items = arrayOf(
            tr("Tab mới", "New tab"), tr("Tab ẩn danh mới", "New incognito tab"), tr("Dịch trang", "Translate page"), tr("Tìm trong trang", "Find in page"), tr("Thu phóng", "Zoom"),
            desktopLabel, tr("Lưu/Bỏ dấu trang", "Add/remove bookmark"), tr("Dấu trang", "Bookmarks"), tr("Lịch sử", "History"),
            tr("Chia sẻ trang", "Share page"), tr("Cài đặt", "Settings"), tr("Đóng tab hiện tại", "Close current tab"), dataSaverLabel,
            tr("Phân tích Data Saver", "Data Saver analytics")
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("TGFN Browser · Tiện ích", "TGFN Browser · Tools"))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> createTab(null, true)
                    1 -> startActivity(Intent(this, IncognitoActivity::class.java))
                    2 -> translateCurrentPage()
                    3 -> showFindInPage()
                    4 -> showZoomControls()
                    5 -> toggleDesktopMode()
                    6 -> toggleBookmark()
                    7 -> showBookmarks()
                    8 -> showHistory()
                    9 -> shareCurrentPage()
                    10 -> showSettings()
                    11 -> closeCurrentTab()
                    12 -> toggleDataSaver()
                    13 -> showDataSaverAnalytics()
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
            setBackgroundColor(canvas)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(12))
        }
        val heading = TextView(this).apply {
            text = tr("Tab đang mở", "Open tabs")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        }
        header.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val countLabel = TextView(this).apply {
            text = tabs.size.toString()
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(Color.rgb(232, 240, 254), dp(16))
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        header.addView(countLabel)
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
                            text = tr("Tab ", "Tab ") + (tabIndex + 1)
                            textSize = 11f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(muted)
                        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        val close = TextView(this).apply {
                            text = "×"
                            textSize = 22f
                            gravity = Gravity.CENTER
                            setTextColor(muted)
                            contentDescription = tr("Đóng tab ", "Close tab ") + (tabIndex + 1)
                            setPadding(dp(6), 0, dp(4), 0)
                            setOnClickListener {
                                closeTabAt(tabIndex)
                                val parentDialog = dialogContent.tag as? android.app.AlertDialog
                                if (tabs.isEmpty()) createTab(null, true)
                                countLabel.text = tabs.size.toString()
                                updateTabButton()
                                if (tabs.isEmpty()) parentDialog?.dismiss() else {
                                    renderCards()
                                    heading.text = tr("Tab đang mở", "Open tabs")
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
            text = "+  " + tr("Tab mới", "New tab")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(canvas)
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
            text = tr("Xong", "Done")
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
        persistSession()
    }

    private fun closeCurrentTab() {
        if (tabs.isEmpty()) return
        closeTabAt(selectedTab)
        if (tabs.isEmpty()) createTab(null, true)
        updateTabButton()
    }

    private fun isDesktopModeEnabled(): Boolean = prefs.getBoolean("desktop_mode", false)

    private fun isDataSaverEnabled(): Boolean = prefs.getBoolean("data_saver_enabled", false)

    /**
     * Keep normal WebView cache validation in both modes. Forcing
     * LOAD_CACHE_ELSE_NETWORK can serve expired HTML/JavaScript and break
     * dynamic sites (for example Google Search). Data Saver must not disable
     * images, network access, desktop mode, or force a page reload.
     *
     * Actual transfer compression needs proxy/server support; WebView cache
     * settings alone cannot provide Chrome-Lite-style savings.
     */
    private fun applyDataSaverSettings(settings: android.webkit.WebSettings) {
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
    }


    private fun showDataSaverAnalytics() {
        if (!prefs.getBoolean("proxy_enabled", false)) {
            android.app.AlertDialog.Builder(this)
                .setTitle(tr("Phân tích Data Saver", "Data Saver analytics"))
                .setMessage(tr(
                    "Hãy bật và cấu hình Go proxy để xem số liệu lưu lượng thực đo. Khi duyệt trực tiếp, ứng dụng không thể đọc bộ đếm của proxy.",
                    "Enable and configure the Go proxy to view measured traffic. Direct browsing cannot read proxy counters."
                ))
                .setPositiveButton(tr("Đóng", "Close"), null)
                .show()
            return
        }
        val endpoint = parseProxyUrl(prefs.getString("proxy_url", "") ?: "")
        if (endpoint == null) {
            Toast.makeText(this, tr("Hãy cấu hình URL proxy hợp lệ trước", "Configure a valid proxy URL first"), Toast.LENGTH_LONG).show()
            return
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setBackgroundColor(canvas)
        }
        val explanation = TextView(this).apply {
            text = tr(
                "Số liệu gồm thống kê thiết bị này và tổng toàn proxy. ID được tạo một lần và lưu trên thiết bị; không tạo ID cho từng request. Do WebView không gửi được header tùy chỉnh cho mọi request proxy, bản này ghép ID theo IP nguồn đã đăng ký; nhiều thiết bị đi qua cùng một IP công khai có thể bị gộp nhầm. HTTP đếm byte thân phản hồi; HTTPS CONNECT đếm byte tunnel mã hóa; chỉ gzip thực sự nhỏ hơn mới tính tiết kiệm.",
                "Shows this device and whole-proxy totals. The ID is generated once and persisted; it is not created per request. Because WebView cannot attach a custom header to every proxy request, this version attributes traffic using the registered source IP; devices sharing one public IP may be conflated. HTTP counts response-body bytes; HTTPS CONNECT counts encrypted tunnel bytes; savings are counted only for actually smaller gzip responses."
            )
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(12))
        }
        val statsText = TextView(this).apply {
            text = tr("Đang tải số liệu…", "Loading analytics…")
            textSize = 14f
            setTextColor(ink)
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        content.addView(explanation)
        content.addView(statsText)
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(tr("Phân tích Data Saver", "Data Saver analytics"))
            .setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton(tr("Đóng", "Close"), null)
            .setNeutralButton(tr("Đặt lại mốc", "Reset baseline"), null)
            .setPositiveButton(tr("Làm mới", "Refresh"), null)
            .create()

        fun formatBytes(value: Long): String {
            if (value < 1024L) return "$value B"
            val units = arrayOf("KB", "MB", "GB", "TB")
            var amount = value.toDouble()
            var unit = -1
            do {
                amount /= 1024.0
                unit++
            } while (amount >= 1024.0 && unit < units.lastIndex)
            return String.format(java.util.Locale.ROOT, "%.2f %s", amount, units[unit])
        }

        fun loadStats(resetBaseline: Boolean) {
            statsText.text = tr("Đang đọc bộ đếm proxy…", "Reading proxy counters…")
            Thread {
                // Re-register on every analytics refresh. Proxy counters and client
                // registrations live in memory, so a proxy restart otherwise leaves
                // this still-running browser with network totals but no device attribution.
                fun registerCurrentClient() {
                    var connection: java.net.HttpURLConnection? = null
                    try {
                        val query = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                        val url = java.net.URI("http", null, endpoint.first, endpoint.second, "/__tgfn/register", query, null).toURL()
                        connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 3500
                            readTimeout = 3500
                            requestMethod = "GET"
                            useCaches = false
                        }
                        connection.responseCode // registration is best-effort for older proxy versions
                    } catch (_: Exception) {
                        // Keep analytics usable even when the proxy has no registration endpoint.
                    } finally {
                        connection?.disconnect()
                    }
                }
                fun fetchStats(forClient: Boolean): org.json.JSONObject {
                    val query = if (forClient) "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8") else null
                    val url = java.net.URI("http", null, endpoint.first, endpoint.second, "/__tgfn/stats", query, null).toURL()
                    val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 3500
                        readTimeout = 3500
                        requestMethod = "GET"
                        useCaches = false
                    }
                    try {
                        val code = connection.responseCode
                        if (code != 200) throw IllegalStateException("HTTP $code")
                        return org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    } finally {
                        connection.disconnect()
                    }
                }
                try {
                    registerCurrentClient()
                    val current = fetchStats(true)
                    val network = fetchStats(false)
                    runOnUiThread {
                        if (resetBaseline) {
                            // Reset only this installation's baseline. The network-wide
                            // baseline must remain unchanged when the user resets device stats.
                            prefs.edit()
                                .putString("data_saver_client_baseline", current.toString())
                                .apply()
                        }
                        val storedClient = prefs.getString("data_saver_client_baseline", null)
                        val storedNetwork = prefs.getString("data_saver_network_baseline", null)
                        fun delta(snapshot: org.json.JSONObject, stored: String?, key: String): Long {
                            val baseline = try {
                                if (stored.isNullOrBlank()) org.json.JSONObject() else org.json.JSONObject(stored)
                            } catch (_: Exception) {
                                org.json.JSONObject()
                            }
                            val sameProcess = baseline.optString("process_started_at", "") == snapshot.optString("process_started_at", "")
                            val base = if (sameProcess && !stored.isNullOrBlank()) baseline.optLong(key, 0L) else 0L
                            return (snapshot.optLong(key, 0L) - base).coerceAtLeast(0L)
                        }
                        fun summary(snapshot: org.json.JSONObject, stored: String?): String {
                            val received = delta(snapshot, stored, "http_response_bytes") + delta(snapshot, stored, "connect_upstream_to_client_bytes")
                            val sent = delta(snapshot, stored, "connect_client_to_upstream_bytes")
                            return buildString {
                                append(tr("Dữ liệu nhận: ", "Data received: ")).append(formatBytes(received)).append('\n')
                                append(tr("Dữ liệu gửi qua HTTPS tunnel: ", "Data sent through HTTPS tunnels: ")).append(formatBytes(sent)).append('\n')
                                append(tr("Gzip tiết kiệm thực đo: ", "Measured gzip saved: ")).append(formatBytes(delta(snapshot, stored, "gzip_saved_bytes"))).append('\n')
                                append(tr("Yêu cầu HTTP: ", "HTTP requests: ")).append(delta(snapshot, stored, "http_requests")).append('\n')
                                append(tr("Tunnel HTTPS đã đóng: ", "Closed HTTPS tunnels: ")).append(delta(snapshot, stored, "connect_tunnels"))
                            }
                        }
                        statsText.text = buildString {
                            append("━━ ").append(tr("THIẾT BỊ NÀY", "THIS DEVICE")).append(" ━━\n")
                            val attributionStatus = current.optString("client_attribution_status", "")
                            val attributionMessage = when (attributionStatus) {
                                "matched_source_ip" -> tr("Định danh: IP nguồn khớp thiết bị", "Attribution: source IP matched")
                                "single_client_fallback" -> tr("Định danh: dự phòng vì chỉ có một thiết bị đăng ký", "Attribution: fallback (only one registered client)")
                                "ambiguous_source_ip" -> tr("Cảnh báo: nhiều thiết bị dùng chung IP; proxy không thể phân biệt chính xác bộ đếm riêng.", "Warning: multiple clients share this IP; per-device attribution is ambiguous.")
                                "source_ip_mapped_to_other_client" -> tr("Cảnh báo: IP nguồn hiện được ánh xạ sang thiết bị khác.", "Warning: this source IP is mapped to another client.")
                                "source_ip_unmapped" -> tr("Cảnh báo: chưa ánh xạ được IP nguồn; bộ đếm riêng có thể thiếu.", "Warning: source IP is unmapped; per-device counters may be incomplete.")
                                else -> ""
                            }
                            if (attributionMessage.isNotEmpty()) append(attributionMessage).append('\n')
                            append(summary(current, storedClient)).append("\n\n")
                            append("━━ ").append(tr("TOÀN MẠNG PROXY", "WHOLE PROXY NETWORK")).append(" ━━\n")
                            append(tr("Thiết bị đã đăng ký: ", "Registered devices: "))
                                .append(network.optInt("registered_clients", 0)).append('\n')
                            append(summary(network, storedNetwork)).append('\n')
                            append(tr("Proxy khởi động: ", "Proxy started: "))
                                .append(network.optString("process_started_at", "—"))
                            if (resetBaseline) append("\n\n").append(tr(
                                "Đã đặt lại mốc thống kê riêng của thiết bị này; mốc toàn mạng được giữ nguyên.",
                                "This device's baseline was reset; the network-wide baseline was preserved."
                            ))
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        statsText.text = tr(
                            "Không đọc được số liệu. Hãy kiểm tra proxy hỗ trợ client analytics, URL/port truy cập được và xác thực proxy không bật. Chi tiết: ",
                            "Could not read analytics. Check that the proxy supports client analytics, its URL/port is reachable, and proxy authentication is disabled. Details: "
                        ) + (e.message ?: e.javaClass.simpleName)
                    }
                }
            }.start()
        }
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener { loadStats(false) }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener { loadStats(true) }
        }
        dialog.show()
        loadStats(false)
    }

    private fun toggleDataSaver() {
        val enabled = !isDataSaverEnabled()
        prefs.edit().putBoolean("data_saver_enabled", enabled).apply()
        tabs.forEach { tab -> applyDataSaverSettings(tab.webView.settings) }
        // Do not reload: changing this preference must not interrupt a page,
        // reset its desktop-site request, or trigger dynamic-page reload loops.
        status.text = if (enabled) {
            tr("Data Saver bật: cache an toàn, giữ nguyên chức năng trang", "Data Saver on: safe cache, preserve site functionality")
        } else {
            tr("Data Saver đã tắt", "Data Saver turned off")
        }
        Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
    }

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
        status.text = if (enabled) tr("Đã bật phiên bản máy tính", "Desktop site enabled") else tr("Đã tắt phiên bản máy tính", "Desktop site disabled")
        Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
    }

    private fun translateCurrentPage() {
        val web = currentWebView() ?: return
        val url = web.url.orEmpty()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, tr("Mở một trang web trước khi dịch", "Open a website before translating"), Toast.LENGTH_SHORT).show()
            return
        }
        val languages = arrayOf(
            "Tiếng Việt", "English", "日本語", "한국어", "中文", "Français",
            "Español", "Deutsch", "Português", "Русский", "ไทย", "Indonesia",
            "Italiano", "العربية", "हिन्दी"
        )
        val codes = arrayOf(
            "vi", "en", "ja", "ko", "zh", "fr",
            "es", "de", "pt", "ru", "th", "id",
            "it", "ar", "hi"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Dịch ngay trên trang", "Translate page"))
            .setItems(languages) { _, which -> translatePageInPlace(web, codes[which], languages[which]) }
            .setNeutralButton(tr("Hiện bản gốc", "Show original")) { _, _ -> restoreOriginalPage(web) }
            .setNegativeButton(tr("Hủy", "Cancel"), null)
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
                var excluded = /^(SCRIPT|STYLE|NOSCRIPT|TEXTAREA|SELECT|OPTION|CODE|PRE|SVG)$/;
                while ((node = walker.nextNode()) && nodes.length < 250) {
                  var parent = node.parentElement;
                  if (!parent || excluded.test(parent.tagName) ||
                      parent.closest('[contenteditable="true"],[translate="no"],[aria-hidden="true"]')) continue;
                  var text = node.nodeValue || '';
                  if (text.trim().length < 2 || text.trim().length > 1200) continue;
                  if (!parent.getClientRects().length) continue;
                  nodes.push({node: node, original: text});
                }
                window.__tgfnTranslateNodes = nodes;
                return JSON.stringify(nodes.map(function(item) { return item.original; }));
              } catch (e) { return '[]'; }
            })();
        """.trimIndent()
        status.text = tr("Đang chuẩn bị dịch…", "Preparing translation…")
        web.evaluateJavascript(collectScript) { raw ->
            try {
                val decoded = org.json.JSONTokener(raw ?: "null").nextValue() as? String ?: "[]"
                val originals = org.json.JSONArray(decoded)
                if (originals.length() == 0) {
                    status.text = tr("Không tìm thấy văn bản để dịch", "No text found to translate")
                    Toast.makeText(this, tr("Không tìm thấy văn bản phù hợp trên trang", "No suitable text found on this page"), Toast.LENGTH_LONG).show()
                    return@evaluateJavascript
                }
                status.text = tr("Đang dịch ", "Translating ") + originals.length() + tr(" đoạn…", " text segments…")
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
                            status.text = if (count > 0) tr("Đã dịch ", "Translated ") + count + tr(" đoạn · ", " segments · ") + languageLabel else tr("Không dịch được nội dung", "Could not translate content")
                            Toast.makeText(this, if (count > 0) tr("Đã dịch ngay trên trang. Dùng Menu → Dịch trang → Hiện bản gốc để hoàn tác.", "Page translated. Use Menu → Translate page → Show original to undo.") else tr("Dịch thất bại. Kiểm tra kết nối mạng rồi thử lại.", "Translation failed. Check your connection and try again."), Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            } catch (e: Exception) {
                status.text = tr("Không thể chuẩn bị nội dung dịch", "Could not prepare page for translation")
                Toast.makeText(this, tr("Không thể đọc văn bản của trang này", "Could not read text on this page"), Toast.LENGTH_LONG).show()
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
            status.text = tr("Đã khôi phục bản gốc", "Original text restored")
            Toast.makeText(this, tr("Đã khôi phục văn bản gốc", "Original text restored"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFindInPage() {
        val web = currentWebView() ?: return
        val query = EditText(this).apply {
            hint = tr("Nhập từ hoặc cụm từ", "Enter a word or phrase")
            setSingleLine(true)
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(tr("Tìm trong trang", "Find in page"))
            .setView(query)
            .setNegativeButton(tr("Đóng", "Close"), null)
            .setPositiveButton(tr("Tìm", "Find"), null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val term = query.text.toString().trim()
                if (term.isBlank()) {
                    query.error = tr("Nhập nội dung cần tìm", "Enter text to find")
                } else {
                    web.setFindListener { activeMatches, _, _ ->
                        Toast.makeText(
                            this,
                            if (activeMatches > 0) tr("Tìm thấy ", "Found ") + activeMatches + tr(" kết quả", " matches") else tr("Không tìm thấy kết quả", "No matches found"),
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
        val options = arrayOf(tr("Thu nhỏ", "Zoom out"), tr("Mức mặc định", "Default zoom"), tr("Phóng to", "Zoom in"))
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Thu phóng trang", "Page zoom"))
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
            .setNegativeButton(tr("Xong", "Done"), null)
            .show()
    }

    private fun showSettings() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        content.addView(TextView(this).apply {
            text = tr("Cài đặt trình duyệt", "Browser settings")
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, 0, 0, dp(12))
        })
        val enabled = android.widget.CheckBox(this).apply {
            text = tr("Bật HTTP/HTTPS proxy", "Enable HTTP/HTTPS proxy")
            isChecked = prefs.getBoolean("proxy_enabled", false)
        }
        content.addView(enabled)
        val dark = android.widget.CheckBox(this).apply {
            text = tr("Chế độ tối", "Dark mode")
            isChecked = darkMode
        }
        content.addView(dark)
        content.addView(TextView(this).apply {
            text = tr("Ngôn ngữ / Language", "Language / Ngôn ngữ")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, dp(10), 0, dp(4))
        })
        val languageGroup = android.widget.RadioGroup(this).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }
        val languageVietnamese = android.widget.RadioButton(this).apply {
            id = View.generateViewId()
            text = "Tiếng Việt"
            setTextColor(ink)
            isChecked = !isEnglish
        }
        val languageEnglish = android.widget.RadioButton(this).apply {
            id = View.generateViewId()
            text = "English"
            setTextColor(ink)
            isChecked = isEnglish
        }
        languageGroup.addView(languageVietnamese)
        languageGroup.addView(languageEnglish)
        content.addView(languageGroup)
        val dataSaver = android.widget.CheckBox(this).apply {
            text = tr("Data Saver: giữ ảnh, cache an toàn", "Data Saver: keep images, safe cache")
            isChecked = isDataSaverEnabled()
        }
        content.addView(dataSaver)
        content.addView(TextView(this).apply {
            text = tr("Không chặn ảnh hoặc kết nối mạng; dùng cache mặc định để tránh HTML/JavaScript cũ và không tự tải lại trang khi bật/tắt. Nếu dùng Go HTTP proxy, proxy có thể nén gzip các phản hồi văn bản HTTP chưa nén; HTTPS qua CONNECT vẫn được giữ nguyên để bảo toàn bảo mật và tương thích.", "Images and network access stay enabled; the browser uses the default cache policy to avoid stale HTML/JavaScript and does not reload pages when toggled. When using the Go HTTP proxy, eligible uncompressed HTTP text responses can be gzip-compressed; HTTPS CONNECT traffic remains untouched for security and compatibility.")
            textSize = 12f
            setTextColor(muted)
            setPadding(dp(28), 0, 0, dp(8))
        })
        val desktop = android.widget.CheckBox(this).apply {
            text = tr("Yêu cầu phiên bản máy tính cho trang web", "Request desktop version of websites")
            isChecked = isDesktopModeEnabled()
        }
        content.addView(desktop)
        val adblock = android.widget.CheckBox(this).apply {
            text = tr("Chặn quảng cáo (AdBlock)", "Block ads (AdBlock)")
            isChecked = prefs.getBoolean("adblock_enabled", true)
        }
        content.addView(adblock)
        val proxyUrl = EditText(this).apply {
            hint = tr("Proxy URL: http://host:port", "Proxy URL: http://host:port")
            setSingleLine(true)
            setText(prefs.getString("proxy_url", "") ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        content.addView(proxyUrl)
        val home = EditText(this).apply {
            hint = tr("Trang chủ URL", "Homepage URL")
            setSingleLine(true)
            setText(prefs.getString("home_url", "https://www.google.com") ?: "")
        }
        content.addView(home)
        content.addView(TextView(this).apply {
            text = tr("Khi bật, HTTP đi qua proxy và HTTPS dùng đường hầm CONNECT. Khi tắt, trình duyệt kết nối trực tiếp. Proxy cần hỗ trợ HTTP CONNECT để tải HTTPS.", "When enabled, HTTP uses the proxy and HTTPS uses CONNECT tunneling. When disabled, the browser connects directly. The proxy must support HTTP CONNECT for HTTPS.")
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(8))
        })
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton(tr("Hủy", "Cancel"), null)
            .setNeutralButton(tr("Tắt proxy", "Disable proxy"), null)
            .setPositiveButton(tr("Lưu", "Save"), null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val normalizedHome = normalizeHttpUrl(home.text.toString().trim())
                if (normalizedHome == null) {
                    home.error = tr("Nhập URL HTTP/HTTPS hợp lệ", "Enter a valid HTTP/HTTPS URL")
                    return@setOnClickListener
                }
                val rawProxy = proxyUrl.text.toString().trim()
                if (enabled.isChecked && parseProxyUrl(rawProxy) == null) {
                    proxyUrl.error = tr("Dùng dạng http://host:port", "Use format http://host:port")
                    return@setOnClickListener
                }
                val previousDarkMode = darkMode
                val previousLanguage = prefs.getString("language", "vi") ?: "vi"
                val selectedLanguage = if (languageEnglish.isChecked) "en" else "vi"
                prefs.edit().putBoolean("proxy_enabled", enabled.isChecked)
                    .putBoolean("dark_mode", dark.isChecked)
                    .putBoolean("desktop_mode", desktop.isChecked)
                    .putBoolean("data_saver_enabled", dataSaver.isChecked)
                    .putBoolean("adblock_enabled", adblock.isChecked)
                    .putString("language", selectedLanguage)
                    .putString("proxy_url", rawProxy).putString("home_url", normalizedHome).apply()
                tabs.forEach { tab ->
                    tab.webView.settings.userAgentString = if (desktop.isChecked) DESKTOP_USER_AGENT else null
                    applyDataSaverSettings(tab.webView.settings)
                }
                dialog.dismiss()
                if (dark.isChecked != previousDarkMode || selectedLanguage != previousLanguage) { recreate(); return@setOnClickListener }
                applySavedProxy { success ->
                    updateProxySummary()
                    status.text = if (success) tr("Đã lưu cài đặt", "Settings saved") else tr("WebView không hỗ trợ cấu hình proxy", "This WebView does not support proxy configuration")
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
                    status.text = if (success) tr("Đã tắt proxy", "Proxy disabled") else tr("Không thể đổi proxy trên WebView này", "Could not change proxy in this WebView")
                }
            }
        }
        dialog.show()
    }

    private fun dialogLanguageRestart() { recreate() }

    private fun toggleBookmark() {
        val url = currentWebView()?.url ?: return
        val bookmarks = (prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()).toMutableSet()
        if (bookmarks.remove(url)) {
            prefs.edit().putStringSet("bookmarks", bookmarks).apply()
            Toast.makeText(this, tr("Đã xóa dấu trang", "Bookmark removed"), Toast.LENGTH_SHORT).show()
        } else {
            bookmarks.add(url)
            prefs.edit().putStringSet("bookmarks", bookmarks).apply()
            Toast.makeText(this, tr("Đã lưu dấu trang", "Bookmark saved"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        val bookmarks = (prefs.getStringSet("bookmarks", emptySet()) ?: emptySet()).toList().sorted()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, tr("Chưa có dấu trang", "No bookmarks yet"), Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle(tr("Dấu trang", "Bookmarks"))
            .setItems(bookmarks.toTypedArray()) { _, which -> currentWebView()?.loadUrl(bookmarks[which]) }
            .setPositiveButton(tr("Đóng", "Close"), null).show()
    }

    /**
     * Persist open tabs independently of Activity instance state. Android may kill the
     * task/process without calling onSaveInstanceState, so that callback alone is not
     * sufficient for restoring the browsing session.
     */
    private fun loadPersistedSessionUrls(): ArrayList<String> {
        val raw = prefs.getString("session_tab_urls", null) ?: return arrayListOf()
        return try {
            val array = org.json.JSONArray(raw)
            ArrayList<String>(array.length()).apply {
                for (i in 0 until array.length()) {
                    val url = array.optString(i, "")
                    if (url.isNotBlank()) add(url)
                }
            }
        } catch (_: Exception) {
            arrayListOf()
        }
    }

    private fun persistSession() {
        val urls = org.json.JSONArray()
        tabs.forEach { tab ->
            val webUrl = tab.webView.url.orEmpty()
            val savedUrl = when {
                tab.isNewTab -> NEW_TAB_SESSION_URL
                webUrl.isBlank() || webUrl.equals("about:blank", ignoreCase = true) ->
                    tab.url.takeIf { it.isNotBlank() && !it.equals("about:blank", ignoreCase = true) }
                        ?: NEW_TAB_SESSION_URL
                else -> webUrl
            }
            urls.put(savedUrl)
        }
        prefs.edit()
            .putString("session_tab_urls", urls.toString())
            .putInt("session_selected_tab", if (tabs.isEmpty()) 0 else selectedTab.coerceIn(0, tabs.lastIndex))
            .apply()
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
            Toast.makeText(this, tr("Chưa có lịch sử duyệt web", "No browsing history yet"), Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle(tr("Lịch sử", "History"))
            .setItems(history.toTypedArray()) { _, which -> currentWebView()?.loadUrl(history[which]) }
            .setPositiveButton(tr("Xóa lịch sử", "Clear history")) { _, _ ->
                prefs.edit().remove("history").apply()
                Toast.makeText(this, tr("Đã xóa lịch sử", "History cleared"), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(tr("Đóng", "Close"), null).show()
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
        startActivity(Intent.createChooser(send, tr("Chia sẻ trang bằng", "Share page using")))
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
        ProxyController.getInstance().setProxyOverride(config, mainExecutor, Runnable {
            registerClient(endpoint) {
                // Registration is best-effort: browsing still works with older proxy versions.
                callback(true)
            }
        })
    }

    private fun registerClient(endpoint: Pair<String, Int>, callback: (Boolean) -> Unit) {
        Thread {
            var registered = false
            var connection: java.net.HttpURLConnection? = null
            try {
                val query = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                val url = java.net.URI("http", null, endpoint.first, endpoint.second, "/__tgfn/register", query, null).toURL()
                connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    requestMethod = "GET"
                    useCaches = false
                }
                registered = connection.responseCode == 200
            } catch (_: Exception) {
                registered = false
            } finally {
                connection?.disconnect()
            }
            runOnUiThread { callback(registered) }
        }.start()
    }

    private fun updateProxySummary() {
        if (!::proxySummary.isInitialized) return
        val enabled = prefs.getBoolean("proxy_enabled", false)
        proxySummary.text = if (enabled) tr("● PROXY BẬT", "● PROXY ON") else tr("● TRỰC TIẾP", "● DIRECT")
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
            Toast.makeText(this, tr("Chỉ hỗ trợ URL HTTP/HTTPS hợp lệ", "Only valid HTTP/HTTPS URLs are supported"), Toast.LENGTH_SHORT).show()
            return
        }
        currentWebView()?.loadUrl(normalized)
    }

    private fun hostLabel(url: String?): String =
        try { URI(url ?: "").host ?: "TGFN Browser" } catch (_: Exception) { "TGFN Browser" }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val NEW_TAB_SESSION_URL = "tgfn-internal://newtab"
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("tab_urls", ArrayList(tabs.map { tab ->
            if (tab.isNewTab) NEW_TAB_SESSION_URL else tab.webView.url
                ?.takeIf { it.isNotBlank() && !it.equals("about:blank", ignoreCase = true) }
                ?: tab.url
        }))
        outState.putInt("selected_tab", selectedTab)
        persistSession()
        super.onSaveInstanceState(outState)
    }

    private fun hideFullscreenView() {
        val view = fullscreenView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        fullscreenView = null
        browserRoot?.visibility = View.VISIBLE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = previousSystemUiVisibility
        val callback = fullscreenCallback
        fullscreenCallback = null
        callback?.onCustomViewHidden()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (fullscreenView != null) {
            hideFullscreenView()
            return
        }
        val web = currentWebView()
        if (web != null && web.canGoBack()) web.goBack() else if (tabs.size > 1) closeCurrentTab() else super.onBackPressed()
    }

    override fun onDestroy() {
        hideFullscreenView()
        persistSession()
        tabs.forEach { tab ->
            (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
            tab.webView.stopLoading()
            tab.webView.destroy()
        }
        persistSession()
        tabs.clear()
        super.onDestroy()
    }
}
