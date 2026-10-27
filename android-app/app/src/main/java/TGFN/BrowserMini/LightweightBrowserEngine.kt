package TGFN.BrowserMini

import android.os.Build
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Small policy layer around Android System WebView.
 *
 * This does not replace Chromium/WebView. It centralizes low-overhead settings
 * and lets inactive tabs yield rendering work and renderer priority.
 */
internal object LightweightBrowserEngine {
    fun configure(
        webView: WebView,
        desktopUserAgent: String?,
        configureTheme: (WebSettings) -> Unit,
        configureDataSaver: (WebSettings) -> Unit
    ) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        configureTheme(settings)
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        configureDataSaver(settings)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.userAgentString = desktopUserAgent
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        // Avoid rasterizing invisible tab contents ahead of time; this saves
        // memory/GPU work when the user has several tabs open.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.OFFSCREEN_PRERASTER)) {
            WebSettingsCompat.setOffscreenPreRaster(settings, false)
        }

        // On Android 8+, allow the renderer to be deprioritized when this tab
        // is hidden. The visible tab remains responsible for foreground work.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
        }
    }

    fun setTabActive(webView: WebView, active: Boolean) {
        webView.visibility = if (active) View.VISIBLE else View.GONE
        if (active) webView.onResume() else webView.onPause()
    }
}
