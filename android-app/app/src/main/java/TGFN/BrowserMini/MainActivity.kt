package TGFN.BrowserMini

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var proxyHost: EditText
    private lateinit var proxyPort: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                status.text = "Loading…"
                address.setText(url.orEmpty())
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = "Ready"
                address.setText(url.orEmpty())
            }
        }
        webView.webChromeClient = WebChromeClient()
        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl("https://www.google.com")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 8)
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        address = EditText(this).apply {
            hint = "Search or enter URL"
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setText("https://www.google.com")
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO) { navigate(); true } else false
            }
        }
        row.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Button(this).apply { text = "Go"; setOnClickListener { navigate() } })
        root.addView(row)

        val proxyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        proxyHost = EditText(this).apply { hint = "Proxy host (optional)"; singleLine = true }
        proxyPort = EditText(this).apply { hint = "Port"; singleLine = true; inputType = 2 }
        proxyRow.addView(proxyHost, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f))
        proxyRow.addView(proxyPort, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        proxyRow.addView(Button(this).apply { text = "Set proxy"; setOnClickListener { applyProxy() } })
        root.addView(proxyRow)
        status = TextView(this).apply { text = "Ready"; gravity = Gravity.START }
        root.addView(status)
        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun navigate() {
        var target = address.text.toString().trim()
        if (target.isBlank()) return
        if (!target.contains("://")) {
            target = if (target.contains(" ") || !target.contains(".")) {
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(target, "UTF-8")
            } else "https://$target"
        }
        if (!target.startsWith("https://") && !target.startsWith("http://")) {
            Toast.makeText(this, "Only HTTP/HTTPS URLs are supported", Toast.LENGTH_SHORT).show()
            return
        }
        webView.loadUrl(target)
    }

    private fun applyProxy() {
        val host = proxyHost.text.toString().trim()
        if (host.isEmpty()) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                Toast.makeText(this, "This WebView does not support proxy override", Toast.LENGTH_LONG).show()
                return
            }
            ProxyController.getInstance().clearProxyOverride(Runnable {
                runOnUiThread { status.text = "Proxy cleared" }
            }, Executor { it.run() })
            return
        }
        val port = proxyPort.text.toString().toIntOrNull()
        if (port == null || port !in 1..65535) {
            Toast.makeText(this, "Enter a valid proxy port (1–65535)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Toast.makeText(this, "This WebView does not support proxy override", Toast.LENGTH_LONG).show()
            return
        }
        val config = ProxyConfig.Builder().addProxyRule("$host:$port").build()
        ProxyController.getInstance().setProxyOverride(config, Runnable {
            runOnUiThread { status.text = "Proxy configured: $host:$port"; webView.reload() }
        }, Executor { it.run() })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
