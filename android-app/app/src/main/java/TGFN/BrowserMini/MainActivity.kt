package TGFN.BrowserMini

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageView
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
    private data class BrowserTab(val webView: WebView, var title: String = "Tab mới", var url: String = "", var isNewTab: Boolean = false, var group: String = "Default", var pendingRestoreUrl: String? = null, var favicon: android.graphics.Bitmap? = null)
    private data class ClosedTab(val title: String, val url: String)


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
                runOnUiThread { if (!isFinishing) status.text = tr("AdBlock: ", "AdBlock: ") + parsed.size + tr(" domain", " domains") }
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
    private val tabGroups = linkedSetOf("Default")
    private val readerOriginalUrls = mutableMapOf<WebView, String>()
    private var selectedTab = 0
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var tabButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var refreshButton: TextView
    private lateinit var securityIndicator: TextView
    private var pageLoading = false
    private var lastClosedTabUrl: String? = null
    private lateinit var webContainer: FrameLayout
    private var browserTopBar: LinearLayout? = null
    private var browserBottomBar: LinearLayout? = null
    private var browserStatusRow: LinearLayout? = null
    private var mediaBackgroundHost: FrameLayout? = null
    private var videoBackground: android.widget.VideoView? = null
    private var gifBackgroundRenderer: WebView? = null
    private lateinit var proxySummary: TextView
    private var browserRoot: View? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var previousSystemUiVisibility: Int = 0
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

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
                val savedGroups = try { org.json.JSONArray(prefs.getString("session_tab_groups", "[]")) } catch (_: Exception) { org.json.JSONArray() }
                val knownGroups = try { org.json.JSONArray(prefs.getString("tab_group_names", org.json.JSONArray().put("Default").toString())) } catch (_: Exception) { org.json.JSONArray() }
                for (i in 0 until knownGroups.length()) knownGroups.optString(i).takeIf { it.isNotBlank() }?.let { tabGroups.add(it) }
                savedUrls.forEachIndexed { index, url ->
                    createTab(url, index == restoreIndex, deferLoad = index != restoreIndex)
                    val group = savedGroups.optString(index, "Default").ifBlank { "Default" }
                    tabs.lastOrNull()?.group = group
                    tabGroups.add(group)
                }
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
        CrashLogger.install(this)
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
        val backgroundUri = prefs.getString("app_background_uri", null)
        val isGifBackground = backgroundUri?.let { isGifUri(Uri.parse(it)) } == true
        val isVideoBackground = backgroundUri?.let { isMp4Uri(Uri.parse(it)) } == true
        val hasCustomBackground = !backgroundUri.isNullOrBlank()
        val isMediaBackground = isGifBackground || isVideoBackground
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(6), dp(6))
            setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas)
        }

        // Keep the address field wide on phones; secondary controls live in the menu.
        val addressCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(5), 0)
            background = rounded(pale, dp(28))
        }
        securityIndicator = TextView(this).apply {
            text = "⌕"
            textSize = 18f
            setTextColor(muted)
            gravity = Gravity.CENTER
            contentDescription = tr("Địa chỉ trang", "Page address")
        }
        addressCard.addView(securityIndicator, LinearLayout.LayoutParams(dp(25), dp(42)))

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
            setSelectAllOnFocus(true)
            contentDescription = tr("Địa chỉ hoặc tìm kiếm", "Address or search")
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate()
                    true
                } else false
            }
        }
        addressCard.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        refreshButton = smallAction("↻", tr("Tải lại", "Reload")) {
            val web = currentWebView() ?: return@smallAction
            if (pageLoading) web.stopLoading() else web.reload()
        }
        addressCard.addView(refreshButton, LinearLayout.LayoutParams(dp(34), dp(42)))
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
        browserTopBar = topBar
        root.addView(topBar)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(232, 234, 237))
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))

        webContainer = FrameLayout(this).apply { setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas) }
        root.addView(webContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas)
            elevation = dp(8).toFloat()
        }
        browserBottomBar = bottomBar
        bottomBar.addView(bottomAction("‹", tr("Quay lại", "Back")) { currentWebView()?.let { if (it.canGoBack()) it.goBack() } })
        bottomBar.addView(bottomAction("›", tr("Tiến", "Forward")) { currentWebView()?.let { if (it.canGoForward()) it.goForward() } })
        bottomBar.addView(bottomAction("⌂", tr("Trang chủ", "Home")) { loadHome() })
        bottomBar.addView(bottomAction("☆", tr("Dấu trang", "Bookmarks")) { showBookmarks() })
        bottomBar.addView(bottomAction("▢", tr("Tab", "Tabs")) { showTabs() })
        root.addView(bottomBar)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(5))
            setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas)
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
        browserStatusRow = statusRow
        root.addView(statusRow)
        browserRoot = root
        if (hasCustomBackground && backgroundUri != null) {
            val host = FrameLayout(this).apply { setBackgroundColor(canvas) }
            mediaBackgroundHost = host
            if (!isMediaBackground) {
                // Static images use the same contain-and-center rule as animated wallpapers.
                // Decode with sampling so very large photos do not consume excessive memory.
                val image = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(Color.TRANSPARENT)
                    try {
                        val uri = Uri.parse(backgroundUri)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, bounds)
                        }
                        var sample = 1
                        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
                        contentResolver.openInputStream(uri)?.use {
                            setImageBitmap(BitmapFactory.decodeStream(it, null, options))
                        }
                    } catch (_: Exception) {
                        setBackgroundColor(canvas)
                    }
                }
                host.addView(image, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                ))
            } else if (isVideoBackground) {
                // VideoView streams through Android's media stack; avoid decoding the whole file into app memory.
                val player = android.widget.VideoView(this).apply {
                    setZOrderMediaOverlay(true)
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f)
                        // Measure the video and fit its view inside the real available host bounds.
                        // This preserves the source aspect ratio instead of stretching/cropping it.
                        val videoWidth = mediaPlayer.videoWidth
                        val videoHeight = mediaPlayer.videoHeight
                        host.post {
                            val hostWidth = host.width
                            val hostHeight = host.height
                            if (videoWidth > 0 && videoHeight > 0 && hostWidth > 0 && hostHeight > 0) {
                                val scale = minOf(
                                    hostWidth.toFloat() / videoWidth,
                                    hostHeight.toFloat() / videoHeight
                                )
                                val fittedWidth = (videoWidth * scale).toInt().coerceAtLeast(1)
                                val fittedHeight = (videoHeight * scale).toInt().coerceAtLeast(1)
                                this@apply.layoutParams = FrameLayout.LayoutParams(
                                    fittedWidth, fittedHeight, Gravity.CENTER
                                )
                            }
                        }
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, tr("Không thể phát video nền này.", "Unable to play this video wallpaper."), Toast.LENGTH_LONG).show()
                        }
                        true
                    }
                    setVideoURI(Uri.parse(backgroundUri))
                }
                videoBackground = player
                host.addView(player, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ))
            } else if (isGifBackground) {
                // Chromium renders animated GIFs in its renderer process rather than decoding via Movie in the Activity.
                val gifRenderer = WebView(this).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    settings.allowContentAccess = true
                    settings.allowFileAccess = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    loadDataWithBaseURL(
                        "https://tgfn.browser/",
                        "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}img{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:auto;height:auto;max-width:100%;max-height:100%;object-fit:contain;display:block}</style></head><body><img src=\"" + android.text.TextUtils.htmlEncode(backgroundUri) + "\" alt=\"\"></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
                gifBackgroundRenderer = gifRenderer
                host.addView(gifRenderer, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                ))
            }
            host.addView(root, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            setContentView(host)
        } else {
            setContentView(root)
        }
        loadAdHosts()
    }

    private fun createTab(url: String?, select: Boolean, popupTransport: Message? = null, deferLoad: Boolean = false) {
        val web = WebView(this).apply {
            // A newly created background tab must not cover the currently selected page.
            visibility = if (select) View.VISIBLE else View.GONE
            setBackgroundColor(canvas)
        }
        LightweightBrowserEngine.configure(
            web,
            if (isDesktopModeEnabled()) DESKTOP_USER_AGENT else null,
            ::configureWebViewTheme,
            ::applyDataSaverSettings
        )
        // Respect the persisted third-party-cookie privacy preference for every tab.
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(
            web, prefs.getBoolean("third_party_cookies_enabled", true)
        )
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
            isNewTab = shouldShowNewTab,
            pendingRestoreUrl = if (deferLoad) {
                if (shouldShowNewTab) NEW_TAB_SESSION_URL else requestedUrl.takeIf { it.isNotBlank() }
            } else null
        )
        web.setBackgroundColor(if (shouldShowNewTab && !prefs.getString("app_background_uri", null).isNullOrBlank()) Color.TRANSPARENT else canvas)
        tabs.add(tab)
        tabGroups.add(tab.group)
        // Update the visible tab counter immediately, before WebView callbacks run.
        updateTabButton()
        webContainer.addView(web, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (!uri.host.equals("tgfn.browser", ignoreCase = true)) return false
                when (uri.path) {
                    "/__bookmarks" -> showBookmarks()
                    "/__history" -> showHistory()
                    "/__downloads" -> openDownloads()
                    else -> return false
                }
                return true
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString()
                if (isAdUrl(url)) {
                    return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                }
                fetchViaHttpsProxy(url)?.let { return it }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if (url?.startsWith(READER_BASE_URL) == true) return
                // Recompute transparency on every navigation, including Back/Forward.
                // A WebView made opaque for a website must become transparent again
                // when it returns to the built-in new-tab page.
                val showWallpaper = url?.startsWith("https://tgfn.browser/", ignoreCase = true) == true
                val hasCustomBackground = !prefs.getString("app_background_uri", null).isNullOrBlank()
                view?.setBackgroundColor(if (hasCustomBackground && showWallpaper) Color.TRANSPARENT else canvas)
                if (isSelected(tab)) updateBrowserChromeBackground(showWallpaper)
                if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                    tab.isNewTab = url == "https://tgfn.browser/"
                }
                tab.url = if (tab.isNewTab) NEW_TAB_SESSION_URL else (url ?: tab.url)
                if (!tab.isNewTab && !url.isNullOrBlank() && (url.startsWith("https://") || url.startsWith("http://"))) loadRemoteFavicon(tab, url)
                tab.title = "Đang tải…"
                if (isSelected(tab)) {
                    pageLoading = true
                    status.text = tr("Đang tải trang…", "Loading page…")
                    refreshButton.text = "×"
                    refreshButton.contentDescription = tr("Dừng tải", "Stop loading")
                    if (!url.isNullOrBlank()) setAddress(url)
                    updateAddressSecurity(url)
                    progress.progress = 0
                    progress.visibility = View.VISIBLE
                }
                updateTabButton()
            }

            // WebView can display the first committed content before every resource finishes.
            // Keep the normal streaming renderer; never buffer or truncate the page HTML here.
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                if (url?.startsWith(READER_BASE_URL) == true) return
                if (isSelected(tab)) {
                    status.text = tr("Đã hiển thị nội dung — đang tải phần còn lại…", "Content visible — loading remaining resources…")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.startsWith(READER_BASE_URL) == true) return
                if (!url.isNullOrBlank() && !url.equals("about:blank", ignoreCase = true)) {
                    tab.isNewTab = url == "https://tgfn.browser/"
                }
                tab.url = if (tab.isNewTab) NEW_TAB_SESSION_URL else (url ?: tab.url)
                tab.title = view?.title?.takeIf { it.isNotBlank() } ?: hostLabel(url)
                if (isSelected(tab)) {
                    updateBrowserChromeBackground(tab.isNewTab)
                    pageLoading = false
                    status.text = tr("Sẵn sàng", "Ready")
                    refreshButton.text = "↻"
                    refreshButton.contentDescription = tr("Tải lại", "Reload")
                    if (!url.isNullOrBlank()) {
                        setAddress(if (tab.isNewTab) "" else url)
                        updateAddressSecurity(url)
                    }
                    progress.progress = 100
                    progress.visibility = View.GONE
                }
                rememberHistory(tab)
                persistSession()
                updateTabButton()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true && fallbackProxyToFetch(view, error)) return
                if (request?.isForMainFrame == true && isSelected(tab)) {
                    pageLoading = false
                    refreshButton.text = "↻"
                    refreshButton.contentDescription = tr("Tải lại", "Reload")
                    progress.visibility = View.GONE
                    status.text = tr("Không tải được trang: ${error?.description ?: "lỗi không xác định"}", "Could not load page: ${error?.description ?: "unknown error"}")
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            // Web platform file inputs use Android's system picker, like Chromium on Android.
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                val intent = try {
                    fileChooserParams?.createIntent()
                        ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                } catch (_: Exception) {
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                }
                return try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    Toast.makeText(this@MainActivity, tr("Không thể mở trình chọn tệp", "Could not open file picker"), Toast.LENGTH_SHORT).show()
                    false
                }
            }

            // Handle JavaScript dialogs instead of silently leaving page scripts waiting.
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(hostLabel(url))
                    .setMessage(message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(hostLabel(url))
                    .setMessage(message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                val input = EditText(this@MainActivity).apply {
                    setText(defaultValue.orEmpty())
                    setSelection(text.length)
                    setSingleLine(true)
                }
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(hostLabel(url))
                    .setMessage(message.orEmpty())
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm(input.text.toString()) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (isSelected(tab)) {
                    progress.progress = newProgress
                    if (newProgress in 1..99) pageLoading = true else if (newProgress >= 100) pageLoading = false
                    refreshButton.text = if (pageLoading) "×" else "↻"
                    refreshButton.contentDescription = if (pageLoading) tr("Dừng tải", "Stop loading") else tr("Tải lại", "Reload")
                    progress.visibility = if (pageLoading) View.VISIBLE else View.GONE
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: android.graphics.Bitmap?) {
                if (icon != null) {
                    tab.favicon = icon
                    updateTabButton()
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
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            enqueueDownload(url, userAgent, contentDisposition, mimeType)
        }
        // Chromium-style link actions: intercept long-presses only when the hit target is a link.
        web.setOnLongClickListener { view ->
            val target = view as? WebView ?: return@setOnLongClickListener false
            val hit = target.hitTestResult ?: return@setOnLongClickListener false
            when (hit.type) {
                WebView.HitTestResult.ANCHOR_TYPE,
                WebView.HitTestResult.SRC_ANCHOR_TYPE,
                WebView.HitTestResult.EMAIL_TYPE,
                WebView.HitTestResult.PHONE_TYPE -> {
                    val link = hit.extra
                    if (link.isNullOrBlank()) false else { showLinkContextMenu(link); true }
                }
                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    showImageContextMenu(hit.extra, target)
                    true
                }
                // Keep Android's native text selection and editing behavior for text/input
                // targets. WebView reports ordinary text as UNKNOWN_TYPE on many versions,
                // so let its native long-press handler process unknown targets as well.
                WebView.HitTestResult.EDIT_TEXT_TYPE,
                WebView.HitTestResult.UNKNOWN_TYPE -> false
                else -> { showPageContextMenu(target); true }
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
        } else if (deferLoad) {
            // Keep restored background tabs dormant until the user selects them.
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

    /**
     * Show the custom wallpaper only on the built-in new-tab page.
     * All browser chrome and the WebView container are opaque on real sites,
     * and become transparent again when returning to the new-tab page.
     */
    private fun isGifUri(uri: Uri): Boolean {
        val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
        return mime.equals("image/gif", ignoreCase = true) ||
            uri.lastPathSegment.orEmpty().substringBefore('?').endsWith(".gif", ignoreCase = true)
    }

    private fun isMp4Uri(uri: Uri): Boolean {
        val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
        return mime.equals("video/mp4", ignoreCase = true) ||
            uri.lastPathSegment.orEmpty().substringBefore('?').endsWith(".mp4", ignoreCase = true)
    }

    private fun updateBrowserChromeBackground(showWallpaper: Boolean) {
        val rawUri = prefs.getString("app_background_uri", null)
        val hasCustomBackground = !rawUri.isNullOrBlank()
        val isGif = if (rawUri.isNullOrBlank()) false else isGifUri(Uri.parse(rawUri))
        val isVideo = if (rawUri.isNullOrBlank()) false else isMp4Uri(Uri.parse(rawUri))
        val isMedia = isGif || isVideo
        val color = if (hasCustomBackground && showWallpaper) Color.TRANSPARENT else canvas
        browserTopBar?.setBackgroundColor(color)
        browserBottomBar?.setBackgroundColor(color)
        browserStatusRow?.setBackgroundColor(color)
        if (isMedia) {
            browserRoot?.setBackgroundColor(if (showWallpaper) Color.TRANSPARENT else canvas)
        }
        if (::webContainer.isInitialized) webContainer.setBackgroundColor(color)
        mediaBackgroundHost?.setBackgroundColor(if (showWallpaper && isMedia) Color.TRANSPARENT else canvas)
        videoBackground?.let { player ->
            if (showWallpaper && isVideo) {
                player.visibility = View.VISIBLE
                if (!player.isPlaying) player.start()
            } else {
                if (player.isPlaying) player.pause()
                player.visibility = View.GONE
            }
        }
        gifBackgroundRenderer?.visibility = if (showWallpaper && isGif) View.VISIBLE else View.GONE
    }

    private fun loadNewTabPage(web: WebView) {
        tabs.firstOrNull { it.webView === web }?.let { tab ->
            tab.isNewTab = true
            tab.url = NEW_TAB_SESSION_URL
            tab.title = tr("Tab mới", "New tab")
        }
        val hasCustomBackground = !prefs.getString("app_background_uri", null).isNullOrBlank()
        web.setBackgroundColor(if (hasCustomBackground) Color.TRANSPARENT else canvas)
        if (tabs.getOrNull(selectedTab)?.webView === web) updateBrowserChromeBackground(showWallpaper = true)
        web.loadDataWithBaseURL("https://tgfn.browser/", newTabHtml(), "text/html", "UTF-8", null)
    }

    private fun newTabHtml(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>
        *{box-sizing:border-box}body{margin:0;background:${if (!prefs.getString("app_background_uri", null).isNullOrBlank()) "transparent" else if (darkMode) "#202124" else "#fff"};color:${if (darkMode) "#e8eaed" else "#202124"};font:16px Roboto,'Arial',sans-serif}
        .wrap{max-width:560px;margin:0 auto;padding:10vh 22px 32px}.brand{text-align:center;font-size:34px;font-weight:700;letter-spacing:-1.5px;color:#1a73e8;margin-bottom:24px}.brand span{color:#188038}
        .search{display:flex;gap:10px;border:1px solid #dadce0;border-radius:28px;padding:7px 14px;box-shadow:0 1px 4px #0000000d}
        input{border:0;outline:0;flex:1;min-width:0;font-size:16px;background:transparent;color:${if (darkMode) "#e8eaed" else "#202124"}}.go,.tool{border:0;background:#1a73e8;color:#fff;border-radius:20px;padding:9px 14px;font-size:13px}
        .tools{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:20px}.tools label{font-size:12px;color:#80868b}.tools select{max-width:145px;background:${if (darkMode) "#303134" else "#f8f9fa"};color:inherit;border:1px solid ${if (darkMode) "#555" else "#dadce0"};border-radius:10px;padding:8px}
        .grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:22px 10px;margin-top:22px}.item{position:relative;text-decoration:none;color:${if (darkMode) "#e8eaed" else "#3c4043"};text-align:center;font-size:12px;min-width:0;touch-action:pan-y;user-select:none}.item.dragging{opacity:.45}.item.over{outline:2px dashed #1a73e8;outline-offset:4px;border-radius:12px}
        .circle{width:52px;height:52px;border-radius:18px;background:${if (darkMode) "#303134" else "#f1f3f4"};margin:0 auto 9px;display:flex;align-items:center;justify-content:center;overflow:hidden}
        .circle img{width:28px;height:28px;object-fit:contain}.circle .letter{font-size:22px;font-weight:700;color:#1a73e8}.item .name{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;padding:0 3px}
        .quick{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;margin-top:22px}.quick a{display:block;text-align:center;text-decoration:none;color:${if (darkMode) "#e8eaed" else "#3c4043"};background:${if (darkMode) "#303134" else "#f8f9fa"};border:1px solid ${if (darkMode) "#44464a" else "#e8eaed"};border-radius:14px;padding:11px 4px;font-size:12px}
        .note{text-align:center;color:#80868b;font-size:12px;margin-top:24px}.empty{grid-column:1/-1;text-align:center;padding:22px 8px;color:#80868b;font-size:13px;border:1px dashed #9aa0a6;border-radius:14px}
        @media(max-width:340px){.wrap{padding:7vh 14px 24px}.brand{font-size:29px}.grid{gap:18px 6px}.circle{width:46px;height:46px}.quick a{font-size:11px}}
        </style></head><body><div class="wrap"><div class="brand">TGFN <span>Browser</span></div>
        <form class="search" action="https://www.google.com/search" method="get"><input name="q" placeholder="${tr("Tìm kiếm trên Google", "Search Google")}" autofocus><button class="go" type="submit">${tr("Tìm", "Search")}</button></form>
        <div class="tools"><label>${tr("Lối tắt · nhấn giữ để sửa/xóa", "Shortcuts · hold to edit/remove")}</label><select id="layout" aria-label="Shortcut layout"><option value="default">${tr("Mặc định", "Default layout")}</option><option value="custom">${tr("Tùy chỉnh", "Custom layout")}</option></select></div>
        <div id="grid" class="grid"></div>
        <div class="quick">
        <a href="https://tgfn.browser/__bookmarks">${tr("Dấu trang", "Bookmarks")}</a>
        <a href="https://tgfn.browser/__history">${tr("Lịch sử", "History")}</a>
        <a href="https://tgfn.browser/__downloads">${tr("Tải xuống", "Downloads")}</a>
        </div><div class="note">${tr("Nhấn giữ lối tắt để sửa hoặc xóa · Kéo để sắp xếp ở chế độ tùy chỉnh", "Hold a shortcut to edit or remove · Drag to reorder in custom layout")}</div></div>
        <script>
        (function(){
          var KEY='tgfn_shortcuts_v1', LAYOUT='tgfn_shortcuts_layout_v1';
          var defaults=[
            {title:'Google',url:'https://www.google.com'},
            {title:'YouTube',url:'https://www.youtube.com'},
            {title:'Wikipedia',url:'https://www.wikipedia.org'},
            {title:'GitHub',url:'https://github.com'},
            {title:'Reddit',url:'https://www.reddit.com'},
            {title:'Bing',url:'https://www.bing.com'}
          ];
          var grid=document.getElementById('grid'), layout=document.getElementById('layout'), dragIndex=-1;
          function read(key,fallback){try{var v=JSON.parse(localStorage.getItem(key));return v===null?fallback:v}catch(e){return fallback}}
          function save(){try{localStorage.setItem(KEY,JSON.stringify(items));localStorage.setItem(LAYOUT,layout.value)}catch(e){}}
          var items=read(KEY,null); if(!Array.isArray(items)) items=defaults.slice();
          items=items.filter(function(x){return x&&typeof x.url==='string'&&/^https?:\/\//i.test(x.url)}).map(function(x){return {title:String(x.title||x.url),url:x.url}});
          layout.value=read(LAYOUT,'default')==='custom'?'custom':'default';
          function addShortcut(){
            var url=prompt('${tr("Nhập địa chỉ URL (https://...)", "Enter shortcut URL (https://...)")}','https://');
            if(!url)return; url=url.trim(); if(!/^https?:\/\//i.test(url)){alert('${tr("URL phải bắt đầu bằng http:// hoặc https://", "URL must start with http:// or https://")}');return}
            var title=prompt('${tr("Tên lối tắt", "Shortcut name")}',url.replace(/^https?:\/\//i,'').split('/')[0])||url;
            items.push({title:title.trim()||url,url:url});save();render();
          }
          function render(){
            grid.innerHTML='';
            items.forEach(function(item,index){
              var a=document.createElement('a');a.className='item';a.href=item.url;a.dataset.index=index;
              a.draggable=layout.value==='custom';
              var circle=document.createElement('div');circle.className='circle';
              var img=document.createElement('img');img.alt='';img.loading='lazy';img.src='https://www.google.com/s2/favicons?domain_url='+encodeURIComponent(item.url)+'&sz=64';
              img.onerror=function(){this.remove();var letter=document.createElement('span');letter.className='letter';letter.textContent=(item.title||'?').charAt(0).toUpperCase();circle.appendChild(letter)};
              circle.appendChild(img);
              var name=document.createElement('span');name.className='name';name.textContent=item.title;
              a.appendChild(circle);a.appendChild(name);
              a.addEventListener('contextmenu',function(e){
                e.preventDefault();e.stopPropagation();
                var action=prompt('${tr("Lối tắt", "Shortcut")}: '+item.title+'\n1 = ${tr("Sửa", "Edit")}\n2 = ${tr("Xóa", "Remove")}\n3 = ${tr("Hủy", "Cancel")}','1');
                if(action==='1'){
                  var url=prompt('${tr("Địa chỉ URL", "URL")}',item.url);if(!url)return;url=url.trim();
                  if(!/^https?:\/\//i.test(url)){alert('${tr("URL không hợp lệ", "Invalid URL")}');return}
                  var title=prompt('${tr("Tên lối tắt", "Shortcut name")}',item.title)||url;
                  items[index]={title:title.trim()||url,url:url};save();render();
                }else if(action==='2'){if(confirm('${tr("Xóa lối tắt này?", "Remove this shortcut?")}')){items.splice(index,1);save();render()}}
              });
              a.addEventListener('dragstart',function(e){if(layout.value!=='custom'){e.preventDefault();return}dragIndex=index;a.classList.add('dragging');if(e.dataTransfer)e.dataTransfer.setData('text/plain',String(index))});
              a.addEventListener('dragover',function(e){if(layout.value==='custom'){e.preventDefault();a.classList.add('over')}});
              a.addEventListener('dragleave',function(){a.classList.remove('over')});
              a.addEventListener('drop',function(e){if(layout.value!=='custom')return;e.preventDefault();var from=dragIndex,to=Number(a.dataset.index);if(from>=0&&from!==to){var moved=items.splice(from,1)[0];items.splice(to,0,moved);save();render()}});
              a.addEventListener('dragend',function(){dragIndex=-1;render()});
              grid.appendChild(a);
            });
            if(items.length===0){var empty=document.createElement('div');empty.className='empty';empty.textContent='${tr("Chưa có lối tắt nào. Nhấn + Thêm để tạo lối tắt.", "No shortcuts yet. Tap + Add to create one.")}';grid.appendChild(empty)}
            var add=document.createElement('button');add.className='item';add.type='button';add.style.border='0';add.style.background='transparent';add.style.color='inherit';add.innerHTML='<span class="circle" style="font-size:28px;color:#1a73e8">+</span><span class="name">${tr("Thêm lối tắt", "Add shortcut")}</span>';add.addEventListener('click',addShortcut);add.addEventListener('contextmenu',function(e){e.preventDefault();addShortcut()});grid.appendChild(add);
          }
          layout.addEventListener('change',function(){save();render()});render();
        })();
        </script></body></html>
    """.trimIndent()

    private fun isSelected(tab: BrowserTab): Boolean = tabs.getOrNull(selectedTab) === tab

    private fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        selectedTab = index
        tabs.forEachIndexed { i, tab ->
            val selected = i == index
            // Centralized lightweight engine pauses hidden tabs and yields their renderer priority.
            LightweightBrowserEngine.setTabActive(tab.webView, selected)
        }
        val tab = tabs[index]
        loadDeferredRestore(tab)
        updateBrowserChromeBackground(tab.isNewTab || tab.webView.url?.startsWith("https://tgfn.browser/", ignoreCase = true) == true)
        setAddress(tab.webView.url ?: tab.url)
        updateAddressSecurity(tab.webView.url ?: tab.url)
        pageLoading = tab.webView.progress in 1..99
        refreshButton.text = if (pageLoading) "×" else "↻"
        refreshButton.contentDescription = if (pageLoading) tr("Dừng tải", "Stop loading") else tr("Tải lại", "Reload")
        progress.progress = tab.webView.progress
        progress.visibility = if (pageLoading) View.VISIBLE else View.GONE
        status.text = if (pageLoading) tr("Đang tải trang…", "Loading page…") else tab.title
        updateTabButton()
        persistSession()
    }

    private fun loadDeferredRestore(tab: BrowserTab) {
        val restoreUrl = tab.pendingRestoreUrl ?: return
        tab.pendingRestoreUrl = null
        if (restoreUrl == NEW_TAB_SESSION_URL || tab.isNewTab) {
            loadNewTabPage(tab.webView)
        } else if (restoreUrl.startsWith("https://", true) || restoreUrl.startsWith("http://", true)) {
            tab.webView.loadUrl(restoreUrl)
        }
    }

    private fun loadRemoteFavicon(tab: BrowserTab, pageUrl: String) {
        val requestedHost = try { URI(pageUrl).host?.lowercase() } catch (_: Exception) { null } ?: return
        Thread {
            var bitmap: android.graphics.Bitmap? = null
            try {
                val iconUrl = "https://www.google.com/s2/favicons?domain_url=" + android.net.Uri.encode(pageUrl) + "&sz=64"
                val connection = java.net.URL(iconUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "TGFN-BrowserMini")
                connection.inputStream.use { bitmap = BitmapFactory.decodeStream(it) }
                connection.disconnect()
            } catch (_: Exception) { }
            val icon = bitmap ?: return@Thread
            runOnUiThread {
                val currentHost = try { URI(tab.webView.url ?: tab.url).host?.lowercase() } catch (_: Exception) { null }
                if (currentHost == requestedHost) tab.favicon = icon
            }
        }.start()
    }

    private fun showPageContextMenu(web: WebView) {
        val pageUrl = web.url?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        val actions = arrayOf(
            tr("Tải lại trang", "Reload page"),
            tr("Quay lại", "Go back"),
            tr("Tiến tới", "Go forward"),
            tr("Mở trang trong tab mới", "Open page in new tab"),
            tr("Sao chép địa chỉ trang", "Copy page address"),
            tr("Chia sẻ trang", "Share page")
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(web.title?.takeIf { it.isNotBlank() } ?: tr("Trang web", "Web page"))
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> web.reload()
                    1 -> if (web.canGoBack()) web.goBack()
                    2 -> if (web.canGoForward()) web.goForward()
                    3 -> pageUrl?.let { createTab(it, true) }
                    4 -> pageUrl?.let {
                        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Page URL", it))
                        Toast.makeText(this, tr("Đã sao chép địa chỉ trang", "Page address copied"), Toast.LENGTH_SHORT).show()
                    }
                    5 -> shareCurrentPage()
                }
            }
            .setNegativeButton(tr("Hủy", "Cancel"), null)
            .show()
    }

    private fun showImageContextMenu(rawUrl: String?, web: WebView) {
        val imageUrl = rawUrl?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        val actions = if (imageUrl == null) arrayOf(tr("Tải lại trang", "Reload page"), tr("Chia sẻ trang", "Share page")) else arrayOf(
            tr("Mở hình ảnh", "Open image"), tr("Mở hình ảnh trong tab mới", "Open image in new tab"),
            tr("Sao chép địa chỉ hình ảnh", "Copy image address"), tr("Chia sẻ hình ảnh", "Share image"),
            tr("Tải hình ảnh xuống", "Download image"), tr("Tải lại trang", "Reload page")
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Hình ảnh", "Image"))
            .setItems(actions) { _, which ->
                if (imageUrl == null) {
                    if (which == 0) web.reload() else shareCurrentPage()
                } else when (which) {
                    0 -> web.loadUrl(imageUrl)
                    1 -> createTab(imageUrl, true)
                    2 -> {
                        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Image URL", imageUrl))
                        Toast.makeText(this, tr("Đã sao chép địa chỉ ảnh", "Image address copied"), Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, imageUrl) }
                        startActivity(Intent.createChooser(send, tr("Chia sẻ hình ảnh", "Share image")))
                    }
                    4 -> enqueueDownload(imageUrl, web.settings.userAgentString, null, "image/*")
                    5 -> web.reload()
                }
            }
            .setNegativeButton(tr("Hủy", "Cancel"), null)
            .show()
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

    private fun updateAddressSecurity(url: String?) {
        if (!::securityIndicator.isInitialized) return
        val secure = url?.startsWith("https://", ignoreCase = true) == true
        securityIndicator.text = if (secure) "🔒" else if (url?.startsWith("http://", ignoreCase = true) == true) "ⓘ" else "⌕"
        securityIndicator.setTextColor(if (secure) Color.rgb(24, 128, 56) else muted)
        securityIndicator.contentDescription = when {
            secure -> tr("Kết nối HTTPS", "HTTPS connection")
            url?.startsWith("http://", ignoreCase = true) == true -> tr("Kết nối HTTP không mã hóa", "Unencrypted HTTP connection")
            else -> tr("Tìm kiếm hoặc địa chỉ", "Search or address")
        }
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
            setPadding(dp(2), dp(4), dp(2), dp(4))
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
            textSize = 10f
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

    /** Hand WebView downloads to Android's download service, preserving page cookies. */
    private fun enqueueDownload(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (url.isNullOrBlank() || !(url.startsWith("https://", true) || url.startsWith("http://", true))) {
            Toast.makeText(this, tr("Liên kết tải xuống không được hỗ trợ", "Unsupported download URL"), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
                .trim()
                .take(180)
                .ifBlank { "download" }
            val request = DownloadManager.Request(android.net.Uri.parse(url))
                .setTitle(filename)
                .setDescription(url)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            android.webkit.CookieManager.getInstance().getCookie(url)
                ?.takeIf { it.isNotBlank() }
                ?.let { request.addRequestHeader("Cookie", it) }
            val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, tr("Đã thêm tải xuống #", "Download queued #") + id, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                tr("Không thể tải xuống: ", "Could not start download: ") + (e.message ?: "error"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = -1
        while (value >= 1024 && unit < units.lastIndex) { value /= 1024.0; unit++ }
        return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[unit])
    }

    private fun openDownloads() {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val rows = mutableListOf<Pair<Long, String>>()
        try {
            manager.query(DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or
                    DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or
                    DownloadManager.STATUS_FAILED
            )).use { cursor ->
                val idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val titleCol = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                while (cursor.moveToNext() && rows.size < 30) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: ("Download #" + id)
                    val state = when (cursor.getInt(statusCol)) {
                        DownloadManager.STATUS_PENDING -> tr("Đang chờ", "Pending")
                        DownloadManager.STATUS_RUNNING -> tr("Đang tải", "Downloading")
                        DownloadManager.STATUS_PAUSED -> tr("Tạm dừng", "Paused")
                        DownloadManager.STATUS_SUCCESSFUL -> tr("Hoàn tất", "Complete")
                        DownloadManager.STATUS_FAILED -> tr("Thất bại", "Failed")
                        else -> tr("Không rõ", "Unknown")
                    }
                    val downloaded = cursor.getLong(downloadedCol).coerceAtLeast(0L)
                    val total = cursor.getLong(totalCol)
                    val percent = if (total > 0) " · " + (downloaded * 100 / total).coerceIn(0, 100) + "%" else ""
                    val size = if (total > 0) " · " + formatBytes(downloaded) + "/" + formatBytes(total) else if (downloaded > 0) " · " + formatBytes(downloaded) else ""
                    rows.add(id to "$title\n$state$percent$size")
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, tr("Không đọc được danh sách tải xuống", "Could not read downloads"), Toast.LENGTH_SHORT).show()
            return
        }
        if (rows.isEmpty()) {
            android.app.AlertDialog.Builder(this)
                .setTitle(tr("Tải xuống", "Downloads"))
                .setMessage(tr("Chưa có tệp tải xuống.", "No downloads yet."))
                .setPositiveButton(tr("Đóng", "Close"), null)
                .show()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Tải xuống gần đây", "Recent downloads"))
            .setItems(rows.map { it.second }.toTypedArray()) { _, which ->
                val id = rows[which].first
                val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                cursor.use {
                    if (!it.moveToFirst()) return@use
                    val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (it.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                        try {
                            val uriCol = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = it.getString(uriCol)
                            if (!localUri.isNullOrBlank()) {
                                val mimeCol = it.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                                val mime = it.getString(mimeCol) ?: "*/*"
                                startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(android.net.Uri.parse(localUri), mime)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        } catch (_: Exception) {
                            Toast.makeText(this, tr("Không tìm thấy ứng dụng mở tệp", "No app can open this file"), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (it.getInt(statusCol) == DownloadManager.STATUS_FAILED) {
                            val uriCol = it.getColumnIndex(DownloadManager.COLUMN_URI)
                            val failedUrl = if (uriCol >= 0) it.getString(uriCol) else null
                            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
                            val failedTitle = if (titleCol >= 0) it.getString(titleCol) else "download"
                            android.app.AlertDialog.Builder(this)
                                .setTitle(tr("Tải xuống thất bại", "Download failed"))
                                .setMessage(tr("Kiểm tra kết nối rồi thử tải lại.", "Check your connection, then retry the download."))
                                .setPositiveButton(tr("Thử lại", "Retry")) { _, _ -> enqueueDownload(failedUrl, null, failedTitle, null) }
                                .setNegativeButton(tr("Đóng", "Close"), null).show()
                        } else {
                            Toast.makeText(this, tr("Tệp vẫn đang tải. Mở mục này lại sau khi hoàn tất.", "This download is still in progress. Open it after completion."), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNeutralButton(tr("Mở trình quản lý hệ thống", "System download manager")) { _, _ ->
                try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                catch (_: Exception) { Toast.makeText(this, tr("Không có trình quản lý tải xuống", "No downloads app is available"), Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .show()
    }

    private fun toggleAdBlock() {
        val enabled = !prefs.getBoolean("adblock_enabled", true)
        prefs.edit().putBoolean("adblock_enabled", enabled).apply()
        if (enabled) loadAdHosts()
        currentWebView()?.reload()
        Toast.makeText(
            this,
            if (enabled) tr("Đã bật AdBlock", "AdBlock enabled") else tr("Đã tắt AdBlock", "AdBlock disabled"),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showBrowserMenu() {
        val adBlockLabel = if (prefs.getBoolean("adblock_enabled", true)) tr("AdBlock: đang bật", "AdBlock: on") else tr("AdBlock: đang tắt", "AdBlock: off")
        val desktopLabel = if (isDesktopModeEnabled()) tr("Phiên bản máy tính: đang bật", "Desktop site: on") else tr("Phiên bản máy tính", "Desktop site")
        val dataSaverLabel = if (isDataSaverEnabled()) "Data Saver: " + tr("bật", "on") else "Data Saver: " + tr("tắt", "off")
        val items = arrayOf(
            tr("Tab mới", "New tab"), tr("Tab ẩn danh mới", "New incognito tab"), tr("Dịch trang", "Translate page"), tr("Tìm trong trang", "Find in page"), tr("Thu phóng", "Zoom"),
            desktopLabel, tr("Lưu/Bỏ dấu trang", "Add/remove bookmark"), tr("Dấu trang", "Bookmarks"), tr("Lịch sử", "History"),
            tr("Chia sẻ trang", "Share page"), tr("Cài đặt", "Settings"), tr("Đóng tab hiện tại", "Close current tab"), dataSaverLabel,
            tr("Phân tích Data Saver", "Data Saver analytics"), tr("Tải xuống", "Downloads"),
            tr("Quản lý quyền riêng tư", "Privacy and site data"), tr("Chế độ đọc", "Reader mode"),
            tr("Nhóm tab", "Tab groups"), tr("Thao tác nhanh", "Quick actions"), adBlockLabel,
            tr("Tab đã đóng gần đây", "Recently closed tabs")
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
                    14 -> openDownloads()
                    15 -> showPrivacyManagement()
                    16 -> toggleReaderMode()
                    17 -> manageTabGroups()
                    18 -> showQuickActions()
                    19 -> toggleAdBlock()
                    20 -> showRecentlyClosedTabs()
                }
            }
            .show()
    }

    private fun showQuickActions() {
        val items = arrayOf(
            tr("Chế độ đọc", "Reader mode"), tr("Tải xuống", "Downloads"),
            tr("Dịch trang", "Translate page"),
            if (isDesktopModeEnabled()) tr("Tắt chế độ máy tính", "Turn desktop site off") else tr("Bật chế độ máy tính", "Turn desktop site on"),
            tr("Quản lý quyền riêng tư", "Privacy and site data"), tr("Nhóm tab", "Tab groups"),
            tr("Chia sẻ trang", "Share page"), tr("Khôi phục tab vừa đóng", "Restore recently closed tab")
        )
        android.app.AlertDialog.Builder(this).setTitle(tr("Thao tác nhanh", "Quick actions"))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> toggleReaderMode()
                    1 -> openDownloads()
                    2 -> translateCurrentPage()
                    3 -> toggleDesktopMode()
                    4 -> showPrivacyManagement()
                    5 -> manageTabGroups()
                    6 -> shareCurrentPage()
                    7 -> restoreLastClosedTab()
                }
            }
            .setNegativeButton(tr("Đóng", "Close"), null).show()
    }

    private fun readRecentlyClosedTabs(): MutableList<ClosedTab> {
        val result = mutableListOf<ClosedTab>()
        try {
            val stored = org.json.JSONArray(prefs.getString("recently_closed_tabs", "[]") ?: "[]")
            for (i in 0 until stored.length()) {
                val item = stored.optJSONObject(i) ?: continue
                val url = item.optString("url").trim()
                if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                    result.add(ClosedTab(item.optString("title"), url))
                }
            }
        } catch (_: Exception) { }
        if (result.isEmpty()) {
            val legacyUrl = prefs.getString("last_closed_tab_url", null)
            if (!legacyUrl.isNullOrBlank() &&
                (legacyUrl.startsWith("http://", true) || legacyUrl.startsWith("https://", true))) {
                result.add(ClosedTab("", legacyUrl))
            }
        }
        return result
    }

    private fun saveRecentlyClosedTabs(items: List<ClosedTab>) {
        val json = org.json.JSONArray()
        items.take(10).forEach { item ->
            json.put(org.json.JSONObject().put("title", item.title).put("url", item.url))
        }
        prefs.edit().putString("recently_closed_tabs", json.toString()).apply()
    }

    private fun rememberClosedTab(title: String, url: String) {
        val items = readRecentlyClosedTabs()
        items.add(0, ClosedTab(title, url))
        saveRecentlyClosedTabs(items.take(10))
        lastClosedTabUrl = url
        prefs.edit().putString("last_closed_tab_url", url).apply()
    }

    private fun restoreLastClosedTab() {
        val items = readRecentlyClosedTabs()
        val item = items.firstOrNull()
        if (item == null) {
            Toast.makeText(this, tr("Không có tab nào để khôi phục", "No recently closed tab to restore"), Toast.LENGTH_SHORT).show()
            return
        }
        saveRecentlyClosedTabs(items.drop(1))
        lastClosedTabUrl = null
        prefs.edit().remove("last_closed_tab_url").apply()
        createTab(item.url, true)
    }

    private fun showRecentlyClosedTabs() {
        val items = readRecentlyClosedTabs()
        if (items.isEmpty()) {
            Toast.makeText(this, tr("Không có tab nào để khôi phục", "No recently closed tabs"), Toast.LENGTH_SHORT).show()
            return
        }
        val labels = items.map { item ->
            val title = item.title.ifBlank {
                try { java.net.URI(item.url).host ?: item.url } catch (_: Exception) { item.url }
            }
            "$title\n${item.url}"
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Tab đã đóng gần đây", "Recently closed tabs"))
            .setItems(labels) { _, which ->
                val current = readRecentlyClosedTabs()
                if (which in current.indices) {
                    val item = current.removeAt(which)
                    saveRecentlyClosedTabs(current)
                    lastClosedTabUrl = null
                    prefs.edit().remove("last_closed_tab_url").apply()
                    createTab(item.url, true)
                }
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .setNeutralButton(tr("Xóa danh sách", "Clear list")) { _, _ ->
                saveRecentlyClosedTabs(emptyList())
                lastClosedTabUrl = null
                prefs.edit().remove("last_closed_tab_url").apply()
            }
            .show()
    }

    private fun closeOtherTabs(keep: BrowserTab) {
        tabs.toList().filter { it !== keep }.forEach { tab ->
            (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
            tab.webView.stopLoading()
            tab.webView.onPause()
            tab.webView.destroy()
            tabs.remove(tab)
        }
        selectedTab = 0
        selectTab(0)
        persistSession()
        Toast.makeText(this, tr("Đã đóng các tab khác", "Closed other tabs"), Toast.LENGTH_SHORT).show()
    }

    private fun showPrivacyManagement() {
        val choices = arrayOf(
            tr("Xóa cache WebView", "Clear WebView cache"),
            tr("Xóa cookie và phiên đăng nhập", "Clear cookies and sign-in sessions"),
            tr("Xóa dữ liệu website (local storage)", "Clear website data (local storage)"),
            tr("Xóa lịch sử duyệt web", "Clear browsing history"),
            tr("Xóa toàn bộ dữ liệu duyệt web", "Clear all browsing data"),
            if (prefs.getBoolean("third_party_cookies_enabled", true))
                tr("Cookie bên thứ ba: Đang cho phép", "Third-party cookies: Allowed")
            else
                tr("Cookie bên thứ ba: Đang chặn", "Third-party cookies: Blocked")
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Quyền riêng tư và dữ liệu website", "Privacy and site data"))
            .setItems(choices) { _, which ->
                if (which == 5) {
                    val enabled = !prefs.getBoolean("third_party_cookies_enabled", true)
                    prefs.edit().putBoolean("third_party_cookies_enabled", enabled).apply()
                    tabs.forEach { tab ->
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(tab.webView, enabled)
                    }
                    Toast.makeText(
                        this,
                        if (enabled) tr("Đã cho phép cookie bên thứ ba", "Third-party cookies allowed")
                        else tr("Đã chặn cookie bên thứ ba", "Third-party cookies blocked"),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setItems
                }
                android.app.AlertDialog.Builder(this)
                    .setTitle(tr("Xác nhận xóa dữ liệu?", "Confirm data deletion?"))
                    .setMessage(tr(
                        "Thao tác này không thể hoàn tác. Xóa cookie sẽ đăng xuất khỏi các website; cài đặt, dấu trang và các tab đang mở được giữ lại.",
                        "This cannot be undone. Clearing cookies signs you out of websites; settings, bookmarks, and open tabs are preserved."
                    ))
                    .setNegativeButton(tr("Hủy", "Cancel"), null)
                    .setPositiveButton(tr("Xóa", "Clear")) { _, _ ->
                        val clearCache = which == 0 || which == 4
                        val clearCookies = which == 1 || which == 4
                        val clearStorage = which == 2 || which == 4
                        val clearHistory = which == 3 || which == 4
                        if (clearCache) tabs.forEach { it.webView.clearCache(true) }
                        if (clearHistory) {
                            prefs.edit().remove("history").apply()
                            tabs.forEach { it.webView.clearHistory() }
                        }
                        if (clearStorage) android.webkit.WebStorage.getInstance().deleteAllData()
                        if (clearCookies) {
                            android.webkit.CookieManager.getInstance().removeAllCookies {
                                android.webkit.CookieManager.getInstance().flush()
                                runOnUiThread {
                                    Toast.makeText(this, tr("Đã xóa cookie và phiên đăng nhập", "Cookies and sign-in sessions cleared"), Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, tr("Đã xóa dữ liệu đã chọn", "Selected browsing data cleared"), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .show()
    }

    private fun toggleReaderMode() {
        val web = currentWebView() ?: return
        val original = readerOriginalUrls[web]
        if (original != null) {
            readerOriginalUrls.remove(web)
            web.loadUrl(original)
            status.text = tr("Đang khôi phục trang gốc…", "Restoring original page…")
            return
        }
        val url = web.url.orEmpty()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, tr("Hãy mở một bài viết trước", "Open an article first"), Toast.LENGTH_SHORT).show()
            return
        }
        status.text = tr("Đang tạo chế độ đọc…", "Preparing reader mode…")
        val script = """
            (function() {
              var root = document.querySelector('article') ||
                         document.querySelector('[role="main"]') ||
                         document.querySelector('main') || document.body;
              var title = document.querySelector('h1') || document.querySelector('title');
              return JSON.stringify({
                title: title ? (title.innerText || title.textContent || '') : document.title,
                text: root ? (root.innerText || root.textContent || '') : ''
              });
            })();
        """.trimIndent()
        web.evaluateJavascript(script) { raw ->
            try {
                val decoded = org.json.JSONTokener(raw ?: "null").nextValue() as? String ?: "{}"
                val article = org.json.JSONObject(decoded)
                val title = article.optString("title", tr("Bài viết", "Article"))
                val text = article.optString("text", "").trim()
                if (text.length < 80) {
                    status.text = tr("Không tìm thấy đủ nội dung để đọc", "Not enough article text found")
                    Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
                    return@evaluateJavascript
                }
                fun escapeHtml(value: String): String = value
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;")
                val paragraphs = text.split(Regex("\\n\\s*\\n+"))
                    .map { it.trim() }.filter { it.isNotEmpty() }
                    .joinToString("\n") { "<p>" + escapeHtml(it).replace("\n", "<br>") + "</p>" }
                val html = """
                    <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>${escapeHtml(title)}</title><style>
                    body{max-width:720px;margin:0 auto;padding:24px 20px 48px;background:${if (darkMode) "#202124" else "#fff"};color:${if (darkMode) "#e8eaed" else "#202124"};font:18px/1.8 Georgia,serif}
                    h1{font:700 30px/1.25 Arial,sans-serif;letter-spacing:-.4px;margin:10px 0 28px}
                    p{margin:0 0 1.25em;overflow-wrap:anywhere} .tag{font:12px Arial,sans-serif;color:${if (darkMode) "#9aa0a6" else "#5f6368"};margin-bottom:22px}
                    </style></head><body><div class="tag">TGFN Browser · Reader Mode</div><h1>${escapeHtml(title)}</h1>$paragraphs</body></html>
                """.trimIndent()
                readerOriginalUrls[web] = url
                web.loadDataWithBaseURL(READER_BASE_URL, html, "text/html", "UTF-8", null)
                status.text = tr("Chế độ đọc · Menu để trở về trang gốc", "Reader mode · Use Menu to restore the original")
            } catch (_: Exception) {
                status.text = tr("Không thể tạo chế độ đọc", "Could not create reader mode")
                Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun manageTabGroups() {
        val options = mutableListOf(tr("Tạo nhóm mới", "Create a group"), tr("Chuyển tab hiện tại vào nhóm…", "Move current tab to a group…"))
        val groups = tabGroups.toList().ifEmpty { listOf("Default") }
        groups.forEach { group -> options.add("${group} (${tabs.count { it.group == group }})") }
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Nhóm tab", "Tab groups"))
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        val input = EditText(this).apply {
                            hint = tr("Tên nhóm", "Group name")
                            setSingleLine(true)
                        }
                        android.app.AlertDialog.Builder(this)
                            .setTitle(tr("Tạo nhóm tab", "Create tab group"))
                            .setView(input)
                            .setNegativeButton(tr("Hủy", "Cancel"), null)
                            .setPositiveButton(tr("Tạo và chuyển tab", "Create and move tab")) { _, _ ->
                                val name = input.text.toString().trim().take(40)
                                if (name.isBlank()) {
                                    Toast.makeText(this, tr("Tên nhóm không được để trống", "Group name cannot be empty"), Toast.LENGTH_SHORT).show()
                                } else {
                                    tabGroups.add(name)
                                    currentTab()?.group = name
                                    persistSession()
                                    Toast.makeText(this, tr("Đã chuyển tab vào nhóm: ", "Moved tab to group: ") + name, Toast.LENGTH_SHORT).show()
                                }
                            }.show()
                    }
                    1 -> chooseTabGroup(currentTab())
                    else -> {
                        val group = groups[which - 2]
                        val members = tabs.indices.filter { tabs[it].group == group }
                        if (members.isEmpty()) {
                            Toast.makeText(this, tr("Nhóm này chưa có tab", "This group has no tabs"), Toast.LENGTH_SHORT).show()
                        } else {
                            val labels = members.map { index ->
                                (if (index == selectedTab) "● " else "○ ") +
                                    (tabs[index].title.takeIf { it.isNotBlank() } ?: tr("Tab mới", "New tab"))
                            }.toTypedArray()
                            android.app.AlertDialog.Builder(this)
                                .setTitle(group)
                                .setItems(labels) { _, selected -> selectTab(members[selected]) }
                                .setPositiveButton(tr("Đóng", "Close"), null)
                                .show()
                        }
                    }
                }
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .show()
    }

    private fun currentTab(): BrowserTab? = tabs.getOrNull(selectedTab)

    private fun chooseTabGroup(tab: BrowserTab?) {
        if (tab == null) return
        val groups = tabGroups.toList().ifEmpty { listOf("Default") }
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Chuyển tab vào nhóm", "Move tab to group"))
            .setItems(groups.toTypedArray()) { _, which ->
                tab.group = groups[which]
                persistSession()
                Toast.makeText(this, tr("Đã chuyển vào nhóm: ", "Moved to group: ") + tab.group, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(tr("Hủy", "Cancel"), null)
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
                        val siteIcon = tab.favicon
                        if (siteIcon != null) {
                            titleRow.addView(ImageView(this).apply {
                                setImageBitmap(siteIcon)
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                contentDescription = tr("Biểu tượng website", "Website icon")
                            }, LinearLayout.LayoutParams(dp(22), dp(22)))
                        }
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
                        card.setOnLongClickListener {
                            val actions = arrayOf(tr("Chuyển đến tab", "Switch to tab"), tr("Đóng tab này", "Close this tab"), tr("Đóng các tab khác", "Close other tabs"))
                            android.app.AlertDialog.Builder(this)
                                .setTitle(tab.title.takeIf { it.isNotBlank() } ?: tr("Tab mới", "New tab"))
                                .setItems(actions) { _, action ->
                                    when (action) {
                                        0 -> selectTab(tabs.indexOf(tab).takeIf { it >= 0 } ?: selectedTab)
                                        1 -> closeTabAt(tabs.indexOf(tab))
                                        2 -> closeOtherTabs(tab)
                                    }
                                    (dialogContent.tag as? android.app.AlertDialog)?.dismiss()
                                }.show()
                            true
                        }
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
        val removedUrl = removed.webView.url ?: removed.url
        if (removedUrl.isNotBlank() && !removedUrl.equals("about:blank", ignoreCase = true) && !removed.isNewTab) {
            rememberClosedTab(removed.title, removedUrl)
        }
        (removed.webView.parent as? ViewGroup)?.removeView(removed.webView)
        removed.webView.stopLoading()
        removed.webView.onPause()
        removed.webView.destroy()
        if (tabs.isEmpty()) {
            selectedTab = 0
            createTab(null, true)
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
                        val url = java.net.URI(if (endpoint.scheme == "https") "https" else "http", null, endpoint.host, endpoint.port, "/__tgfn/register", query, null).toURL()
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
                    val url = java.net.URI(if (endpoint.scheme == "https") "https" else "http", null, endpoint.host, endpoint.port, "/__tgfn/stats", query, null).toURL()
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
                val decoded = try {
                    org.json.JSONTokener(raw ?: "null").nextValue() as? String
                } catch (_: Exception) { null } ?: "[]"
                val originals = try { org.json.JSONArray(decoded) } catch (_: Exception) { org.json.JSONArray() }
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


    private fun showPrivacyPolicy() {
        val message = tr(
            """TGFN BrowserMini không thu thập, không lưu trên server và không bán dữ liệu người dùng.

Dữ liệu chỉ nằm trên máy của bạn:
• Lịch sử, dấu trang, tab, cài đặt
• Log crash (chỉ khi bạn mở trong Cài đặt)

Tính năng tùy chọn:
• Dịch trang: đoạn chữ được gửi tới Google Dịch
• Proxy: lưu lượng đi qua proxy bạn tự nhập
• AdBlock: tải danh sách hosts công khai (StevenBlack)

Không có tài khoản, không analytics, không quảng cáo.""",
            """TGFN BrowserMini does not collect, store on a server, or sell user data.

Data stays on your device:
• History, bookmarks, tabs, settings
• Crash log (only if you open it in Settings)

Optional features:
• Translate: text is sent to Google Translate
• Proxy: traffic goes through the proxy you enter
• AdBlock: downloads a public hosts list (StevenBlack)

No account, no analytics, no ads."""
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Chính sách quyền riêng tư", "Privacy policy"))
            .setMessage(message)
            .setPositiveButton(tr("Đóng", "Close"), null)
            .show()
    }


    private fun showProxyGuide() {
        val repo = "https://github.com/nam348tnh3gp/Browser-Proxy/tree/main/go-proxy"
        val message = tr(
            """Bạn có thể tự chạy proxy Go của TGFN.

Máy tính
1. Cài Go: https://go.dev/dl/
2. Tải mã: $repo
3. Trong go-proxy:
   PROXY_LISTEN=0.0.0.0:8080 go run .
4. Trên điện thoại nhập: IP-máy-tính:8080

Termux (chạy ngay trên Android)
1. pkg update && pkg install golang git
2. git clone https://github.com/nam348tnh3gp/Browser-Proxy.git
3. cd Browser-Proxy/go-proxy
4. PROXY_LISTEN=0.0.0.0:8080 go run .
5. Trong app nhập: 127.0.0.1:8080
   (cùng máy) hoặc IP điện thoại:8080 nếu máy khác cùng Wi-Fi

Không dùng 127.0.0.1 khi proxy chạy trên máy khác.""",
            """You can run the TGFN Go proxy yourself.

Computer
1. Install Go: https://go.dev/dl/
2. Get the code: $repo
3. In go-proxy:
   PROXY_LISTEN=0.0.0.0:8080 go run .
4. On the phone enter: computer-ip:8080

Termux (run on Android)
1. pkg update && pkg install golang git
2. git clone https://github.com/nam348tnh3gp/Browser-Proxy.git
3. cd Browser-Proxy/go-proxy
4. PROXY_LISTEN=0.0.0.0:8080 go run .
5. In the app enter: 127.0.0.1:8080
   (same phone) or the phone IP:8080 for another device on the same Wi-Fi

Do not use 127.0.0.1 when the proxy runs on another device."""
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Tự chạy proxy", "Run your own proxy"))
            .setMessage(message)
            .setPositiveButton(tr("Mở mã nguồn", "Open source")) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(repo)))
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .show()
    }

    private fun applyCustomAppBackground(root: View) {
        val rawUri = prefs.getString("app_background_uri", null)
        if (rawUri.isNullOrBlank()) {
            root.setBackgroundColor(canvas)
            return
        }
        try {
            val uri = android.net.Uri.parse(rawUri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (bitmap != null) {
                root.background = BitmapDrawable(resources, bitmap).apply { gravity = Gravity.FILL }
            } else root.setBackgroundColor(canvas)
        } catch (_: Exception) {
            root.setBackgroundColor(canvas)
        }
    }

    private fun dialogSafeRecreate() {
        recreate()
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
        val backgroundButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val chooseBackground = android.widget.Button(this).apply {
            text = tr("Chọn nền ảnh/GIF/MP4", "Choose image/GIF/MP4 background")
            setOnClickListener {
                try {
                    val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/mp4"))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    }
                    startActivityForResult(picker, 2302)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, tr("Không mở được bộ chọn ảnh", "Could not open image picker"), Toast.LENGTH_SHORT).show()
                }
            }
        }
        backgroundButtons.addView(chooseBackground, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val clearBackground = android.widget.Button(this).apply {
            text = tr("Xóa nền", "Clear")
            setOnClickListener {
                prefs.edit().remove("app_background_uri").apply()
                dialogSafeRecreate()
            }
        }
        backgroundButtons.addView(clearBackground, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(TextView(this).apply {
            text = tr("Nền ứng dụng: ảnh tĩnh, GIF động hoặc video MP4 lặp lại; chỉ hiện ở trang chủ, không che website", "App background: still image, animated GIF, or looping MP4; shown only on the home page, never over website content")
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(backgroundButtons)
        val restoreSession = android.widget.CheckBox(this).apply {
            text = tr("Khôi phục các tab khi mở lại trình duyệt", "Restore tabs when reopening the browser")
            isChecked = prefs.getBoolean("restore_session_enabled", true)
        }
        content.addView(restoreSession)
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
        val crashLog = android.widget.Button(this).apply {
            text = tr("Xem log crash", "View crash log")
            setOnClickListener {
                val log = CrashLogger.lastCrash(this@MainActivity)
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(tr("Log crash", "Crash log"))
                    .setMessage(log ?: tr("Chưa có crash", "No crash logged"))
                    .setPositiveButton(tr("Đóng", "Close"), null)
                    .show()
            }
        }
        
        content.addView(crashLog)
        val privacy = android.widget.Button(this).apply {
            text = tr("Chính sách quyền riêng tư", "Privacy policy")
            setOnClickListener { showPrivacyPolicy() }
        }
        content.addView(privacy)
        val proxyUrl = EditText(this).apply {
            hint = tr("Proxy: host:port, http(s)://host:port, socks://host:port", "Proxy: host:port, http(s)://host:port, socks://host:port")
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
            text = tr("Thử CONNECT trước (Cloudflare tunnel thường chuyển được). Nếu proxy không chuyển CONNECT, app tự fallback sang /__tgfn/fetch như J2ME.", "When enabled, HTTP uses the proxy and HTTPS uses CONNECT tunneling. When disabled, the browser connects directly. The proxy must support HTTP CONNECT for HTTPS.")
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(8))
        })
        val proxyGuide = android.widget.Button(this).apply {
            text = tr("Tự chạy proxy (hướng dẫn)", "Run your own proxy")
            setOnClickListener { showProxyGuide() }
        }
        content.addView(proxyGuide)
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
                    proxyUrl.error = tr("Dạng: host:port, http://, https:// hoặc socks://", "Use host:port, http://, https:// or socks://")
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
            // Session snapshots are versioned so malformed or incompatible data safely
            // falls back to a fresh tab instead of crashing browser startup.
            val version = prefs.getInt("session_snapshot_version", 1)
            if (version !in 1..1) return arrayListOf()
            val array = org.json.JSONArray(raw)
            ArrayList<String>(array.length()).apply {
                for (i in 0 until array.length()) {
                    val url = array.optString(i, "").trim()
                    if (url == NEW_TAB_SESSION_URL || url.startsWith("https://", true) || url.startsWith("http://", true)) add(url)
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
            .putInt("session_snapshot_version", 1)
            .putString("session_tab_urls", urls.toString())
            .putString("session_tab_groups", org.json.JSONArray().apply { tabs.forEach { put(it.group) } }.toString())
            .putString("tab_group_names", org.json.JSONArray().apply { tabGroups.forEach { put(it) } }.toString())
            .putInt("session_selected_tab", if (tabs.isEmpty()) 0 else selectedTab.coerceIn(0, tabs.lastIndex))
            .apply()
    }

    private fun rememberHistory(tab: BrowserTab) {
        if (tab.url.isBlank() || !tab.url.startsWith("http")) return

        val history = (prefs.getStringSet("history", emptySet()) ?: emptySet()).toMutableSet()
        history.add(tab.url)
        val timestamps = try {
            org.json.JSONObject(prefs.getString("history_timestamps", "{}") ?: "{}")
        } catch (_: Exception) {
            org.json.JSONObject()
        }
        timestamps.put(tab.url, System.currentTimeMillis())

        // Keep the complete browsing history. The history UI pages through it as the user scrolls.
        prefs.edit()
            .putStringSet("history", history)
            .putString("history_timestamps", timestamps.toString())
            .apply()
    }

    private fun showHistory() {
        val savedHistory = (prefs.getStringSet("history", emptySet()) ?: emptySet()).toList()
        val timestamps = try {
            org.json.JSONObject(prefs.getString("history_timestamps", "{}") ?: "{}")
        } catch (_: Exception) {
            org.json.JSONObject()
        }
        val history = savedHistory.sortedWith(
            compareByDescending<String> { timestamps.optLong(it, 0L) }.thenByDescending { it }
        )
        if (history.isEmpty()) {
            Toast.makeText(this, tr("Chưa có lịch sử duyệt web", "No browsing history yet"), Toast.LENGTH_SHORT).show()
            return
        }

        val pageSize = 50
        var loadedCount = minOf(pageSize, history.size)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        fun labelFor(url: String): String {
            val visitedAt = timestamps.optLong(url, 0L)
            return if (visitedAt > 0L) "${formatter.format(java.util.Date(visitedAt))}\n$url" else url
        }

        val adapter = android.widget.ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1,
            history.take(loadedCount).map(::labelFor)
        )
        val list = android.widget.ListView(this).apply {
            this.adapter = adapter
            dividerHeight = dp(1)
            setOnItemClickListener { _, _, position, _ ->
                if (position in history.indices) currentWebView()?.loadUrl(history[position])
            }
            setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) = Unit
                override fun onScroll(
                    view: android.widget.AbsListView?,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    if (loadedCount < history.size && firstVisibleItem + visibleItemCount >= totalItemCount - 5) {
                        val nextCount = minOf(loadedCount + pageSize, history.size)
                        if (nextCount > loadedCount) {
                            adapter.addAll(history.subList(loadedCount, nextCount).map(::labelFor))
                            loadedCount = nextCount
                        }
                    }
                }
            })
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(tr("Lịch sử", "History"))
            .setView(list)
            .setPositiveButton(tr("Xóa lịch sử", "Clear history")) { _, _ ->
                prefs.edit().remove("history").remove("history_timestamps").apply()
                Toast.makeText(this, tr("Đã xóa lịch sử", "History cleared"), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(tr("Đóng", "Close"), null)
            .create()
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (resources.displayMetrics.heightPixels * 0.8f).toInt()
        )
    }

    /** Show browser actions for a long-pressed web link, similar to Chromium's link menu. */
    private fun showLinkContextMenu(rawUrl: String) {
        val url = normalizeHttpUrl(rawUrl)
        if (url == null) {
            Toast.makeText(this, tr("Liên kết này chưa được hỗ trợ", "This link type is not supported"), Toast.LENGTH_SHORT).show()
            return
        }
        val actions = arrayOf(
            tr("Mở liên kết", "Open link"),
            tr("Mở trong tab mới", "Open in new tab"),
            tr("Mở trong tab nền", "Open in background tab"),
            tr("Sao chép địa chỉ liên kết", "Copy link address"),
            tr("Chia sẻ liên kết", "Share link"),
            tr("Tải liên kết xuống", "Download link")
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(hostLabel(url))
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> currentWebView()?.loadUrl(url)
                    1 -> createTab(url, true)
                    2 -> {
                        createTab(url, false)
                        Toast.makeText(this, tr("Đã mở liên kết trong tab nền", "Opened link in background tab"), Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
                        Toast.makeText(this, tr("Đã sao chép liên kết", "Link copied"), Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        startActivity(Intent.createChooser(send, tr("Chia sẻ liên kết bằng", "Share link using")))
                    }
                    5 -> enqueueDownload(url, currentWebView()?.settings?.userAgentString, null, null)
                }
            }
            .setNegativeButton(tr("Hủy", "Cancel"), null)
            .show()
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

    private data class ProxyEndpoint(val scheme: String, val host: String, val port: Int) {
        fun rule(): String = "$scheme://$host:$port"
        fun registerBase(): String = if (scheme == "https") "https://$host:$port" else "http://$host:$port"
    }

    private fun parseProxyUrl(raw: String): ProxyEndpoint? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        return try {
            val uri = URI(withScheme)
            val scheme = (uri.scheme ?: "http").lowercase()
            if (scheme !in setOf("http", "https", "socks", "socks5")) return null
            val host = uri.host ?: return null
            if (host.isBlank()) return null
            if (uri.rawPath != null && uri.rawPath.isNotEmpty() && uri.rawPath != "/") return null
            if (uri.rawQuery != null || uri.rawFragment != null) return null
            val defaultPort = when (scheme) {
                "https" -> 443
                "socks", "socks5" -> 1080
                else -> 8080
            }
            val port = if (uri.port == -1) defaultPort else uri.port
            if (port !in 1..65535) return null
            val normalized = if (scheme == "socks5") "socks" else scheme
            ProxyEndpoint(normalized, host, port)
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
        proxyFetchFallback = false
        val config = ProxyConfig.Builder().addProxyRule(endpoint.rule()).build()
        ProxyController.getInstance().setProxyOverride(config, mainExecutor, Runnable {
            Thread {
                val connectOk = proxyConnectWorks(endpoint)
                runOnUiThread {
                    if (connectOk) {
                        proxyFetchFallback = false
                        registerClient(endpoint.host, endpoint.port, endpoint.scheme) { callback(true) }
                    } else {
                        proxyFetchFallback = true
                        ProxyController.getInstance().clearProxyOverride(mainExecutor, Runnable {
                            registerClient(endpoint.host, endpoint.port, endpoint.scheme) { callback(true) }
                        })
                    }
                }
            }.start()
        })
    }

    @Volatile private var proxyFetchFallback = false

    private fun proxyConnectWorks(endpoint: ProxyEndpoint): Boolean {
        return try {
            if (endpoint.scheme == "https" || endpoint.scheme == "http") {
                val socket = if (endpoint.scheme == "https") {
                    javax.net.ssl.SSLSocketFactory.getDefault().createSocket(endpoint.host, endpoint.port)
                } else {
                    java.net.Socket(endpoint.host, endpoint.port)
                }
                socket.soTimeout = 6000
                socket.getOutputStream().apply {
                    write("CONNECT example.com:443 HTTP/1.1\r\nHost: example.com:443\r\nProxy-Connection: keep-alive\r\n\r\n".toByteArray())
                    flush()
                }
                val status = socket.getInputStream().bufferedReader().readLine() ?: ""
                socket.close()
                status.contains(" 200")
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun fallbackProxyToFetch(view: WebView?, error: WebResourceError?): Boolean {
        if (proxyFetchFallback || !prefs.getBoolean("proxy_enabled", false)) return false
        val desc = error?.description?.toString()?.lowercase() ?: ""
        val code = error?.errorCode ?: 0
        val tunnelFailed = code == WebViewClient.ERROR_CONNECT ||
            code == WebViewClient.ERROR_PROXY_AUTHENTICATION ||
            code == WebViewClient.ERROR_IO ||
            code == WebViewClient.ERROR_TIMEOUT ||
            code == WebViewClient.ERROR_FAILED_SSL_HANDSHAKE ||
            desc.contains("empty") || desc.contains("proxy") || desc.contains("tunnel") || desc.contains("connect")
        if (!tunnelFailed) return false
        proxyFetchFallback = true
        ProxyController.getInstance().clearProxyOverride(mainExecutor, Runnable { view?.reload() })
        return true
    }

    private fun fetchViaHttpsProxy(target: String?): WebResourceResponse? {
        if (target.isNullOrBlank()) return null
        if (!target.startsWith("http://") && !target.startsWith("https://")) return null
        if (target.startsWith("https://tgfn.browser", ignoreCase = true)) return null
        if (!prefs.getBoolean("proxy_enabled", false)) return null
        val endpoint = parseProxyUrl(prefs.getString("proxy_url", "") ?: "") ?: return null
        if (!proxyFetchFallback) return null
        if (target.contains(endpoint.host) && target.contains("/__tgfn/")) return null
        return try {
            val base = if (endpoint.port == 443) "https://${endpoint.host}" else "https://${endpoint.host}:${endpoint.port}"
            val proxyUrl = base + "/__tgfn/fetch?url=" + java.net.URLEncoder.encode(target, "UTF-8")
            val connection = java.net.URL(proxyUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 35000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "TGFN-BrowserMini-Android")
            connection.setRequestProperty("ngrok-skip-browser-warning", "1")
            connection.setRequestProperty("Accept", "*/*")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream ?: java.io.ByteArrayInputStream(ByteArray(0))
            val mime = connection.contentType?.substringBefore(";")?.trim().takeUnless { it.isNullOrBlank() } ?: "text/html"
            WebResourceResponse(mime, "utf-8", stream)
        } catch (_: Exception) {
            null
        }
    }

    private fun registerClient(host: String, port: Int, scheme: String, callback: (Boolean) -> Unit) {
        Thread {
            var registered = false
            var connection: java.net.HttpURLConnection? = null
            try {
                val query = "client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                val url = java.net.URI(if (scheme == "https") "https" else "http", null, host, port, "/__tgfn/register", query, null).toURL()
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

    private fun saveCustomBackgroundResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val mime = try { contentResolver.getType(uri) } catch (_: Exception) { null }
        val path = uri.lastPathSegment.orEmpty().substringBefore('?')
        val isGif = mime.equals("image/gif", ignoreCase = true) || path.endsWith(".gif", ignoreCase = true)
        val isMp4 = mime.equals("video/mp4", ignoreCase = true) || path.endsWith(".mp4", ignoreCase = true)
        val supported = mime?.startsWith("image/") == true || isMp4 || isGif
        if (!supported) {
            Toast.makeText(this, tr("Chỉ hỗ trợ ảnh, GIF và video MP4", "Only images, GIF, and MP4 video are supported"), Toast.LENGTH_LONG).show()
            return
        }
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
        prefs.edit().putString("app_background_uri", uri.toString()).apply()
        recreate()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2302) {
            saveCustomBackgroundResult(resultCode, data)
            return
        }
        if (requestCode != FILE_CHOOSER_REQUEST) return
        val callback = fileChooserCallback ?: return
        fileChooserCallback = null
        val results = if (resultCode == Activity.RESULT_OK) {
            try {
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                    ?: data?.data?.let { arrayOf(it) }
            } catch (_: Exception) {
                data?.data?.let { arrayOf(it) }
            }
        } else null
        callback.onReceiveValue(results)
    }

    private fun hostLabel(url: String?): String =
        try { URI(url ?: "").host ?: "TGFN Browser" } catch (_: Exception) { "TGFN Browser" }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val FILE_CHOOSER_REQUEST = 2301
        private const val NEW_TAB_SESSION_URL = "tgfn-internal://newtab"
        private const val READER_BASE_URL = "https://tgfn-reader.local/"
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

    override fun onPause() {
        // Suspend WebView rendering/processing while the browser Activity is backgrounded.
        tabs.forEach { it.webView.onPause() }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Resume only the selected tab; keep other tabs suspended.
        tabs.forEachIndexed { index, tab ->
            if (index == selectedTab) tab.webView.onResume() else tab.webView.onPause()
        }
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        hideFullscreenView()
        gifBackgroundRenderer?.let { renderer ->
            (renderer.parent as? ViewGroup)?.removeView(renderer)
            renderer.stopLoading()
            renderer.destroy()
        }
        gifBackgroundRenderer = null
        videoBackground?.stopPlayback()
        videoBackground = null
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
