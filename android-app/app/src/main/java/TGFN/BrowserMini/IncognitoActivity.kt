package TGFN.BrowserMini
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.webkit.*
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import android.widget.*
import java.net.URI

class IncognitoActivity : Activity() {
 private data class Tab(val web: WebView, var title: String = "Tab ẩn danh", var url: String = "")
 private val tabs = mutableListOf<Tab>()
 private var selected = -1
 private lateinit var address: EditText
 private lateinit var box: FrameLayout
 private lateinit var count: TextView
 private lateinit var status: TextView
 private var browserRoot: View? = null
 private var fullscreenView: View? = null
 private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
 private var previousSystemUiVisibility: Int = 0
 private val prefs by lazy { getSharedPreferences("browser_settings", MODE_PRIVATE) }
 private val darkMode get() = prefs.getBoolean("dark_mode", false)
 private val isEnglish get() = prefs.getString("language", "vi") == "en"
 private val surface: Int get() = if (darkMode) Color.rgb(32,33,36) else Color.WHITE
 private val pageBackground get() = if (darkMode) Color.rgb(32,33,36) else Color.rgb(248,245,255)
 private val textColor get() = if (darkMode) Color.rgb(232,234,237) else Color.rgb(32,33,36)
 private val purple = Color.rgb(103,58,183)
 private fun tr(vi:String,en:String)=if(isEnglish) en else vi

 override fun onCreate(savedInstanceState: Bundle?) {
  // Match WebView prefers-color-scheme to the saved browser theme before initialization.
  setTheme(if (getSharedPreferences("browser_settings", MODE_PRIVATE).getBoolean("dark_mode", false))
   android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
  if (Build.VERSION.SDK_INT >= 28) WebView.setDataDirectorySuffix("tgfn_incognito")
  super.onCreate(savedInstanceState)
  window.statusBarColor = pageBackground
  @Suppress("DEPRECATION")
  window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
  clearPrivateData()
  val root = LinearLayout(this).apply { orientation=1; setBackgroundColor(pageBackground) }
  root.addView(TextView(this).apply { text=tr("◉  Ẩn danh · Không lưu lịch sử","◉  Incognito · No history saved"); textSize=13f; setTextColor(purple); setPadding(dp(12),dp(10),dp(12),dp(8)) })
  val bar=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(4),dp(8),dp(8)); setBackgroundColor(surface) }
  address=EditText(this).apply { hint=tr("Tìm kiếm hoặc nhập URL","Search or enter URL"); setSingleLine(); imeOptions=EditorInfo.IME_ACTION_GO
   setOnEditorActionListener { _, a, _ -> if(a==EditorInfo.IME_ACTION_GO || a==EditorInfo.IME_ACTION_DONE){navigate();true}else false } }
  bar.addView(address,LinearLayout.LayoutParams(0,dp(44),1f))
  count=button("1"){showTabs()}; bar.addView(count,LinearLayout.LayoutParams(dp(40),dp(42)))
  bar.addView(button("+"){addTab(null)},LinearLayout.LayoutParams(dp(40),dp(42)))
  root.addView(bar)
  box=FrameLayout(this).apply { setBackgroundColor(surface) }; root.addView(box,LinearLayout.LayoutParams(-1,0,1f))
  val foot=LinearLayout(this).apply { gravity=Gravity.CENTER; setBackgroundColor(surface) }
  foot.addView(footButton("‹",tr("Quay lại","Back")){current()?.let{if(it.canGoBack())it.goBack()}})
  foot.addView(footButton("›",tr("Tiến","Forward")){current()?.let{if(it.canGoForward())it.goForward()}})
  foot.addView(footButton("↻",tr("Tải lại","Reload")){current()?.reload()})
  foot.addView(footButton("×",tr("Đóng tab","Close tab")){closeTab(selected)})
  foot.addView(footButton(tr("Xong","Done"),tr("Thoát","Exit")){finish()})
  root.addView(foot,LinearLayout.LayoutParams(-1,dp(48)))
  status=TextView(this).apply { text=tr("Tab riêng tư không lưu vào lịch sử","Private tabs are not saved to history"); textSize=11f; setPadding(dp(12),dp(6),dp(12),dp(8)); setBackgroundColor(surface) }; root.addView(status)
  browserRoot = root
  setContentView(root); addTab(null)
 }
 private fun addTab(url:String?) {
  val w=WebView(this); w.settings.javaScriptEnabled=true; w.settings.domStorageEnabled=true
  configureWebViewTheme(w.settings)
  w.settings.loadsImagesAutomatically=true; w.settings.cacheMode=WebSettings.LOAD_NO_CACHE
  w.settings.setSupportZoom(true); w.settings.builtInZoomControls=true; w.settings.displayZoomControls=false
  val t=Tab(w,url=url.orEmpty()); tabs.add(t); box.addView(w,FrameLayout.LayoutParams(-1,-1))
  w.webViewClient=object:WebViewClient(){
   override fun onPageStarted(v:WebView?,u:String?,f:android.graphics.Bitmap?){t.url=u?:t.url;if(isSelected(t)){status.text=tr("Đang tải riêng tư…","Loading privately…");if(!u.isNullOrBlank())setAddress(u)};updateCount()}
   override fun onPageFinished(v:WebView?,u:String?){t.url=u?:t.url;t.title=v?.title?.takeIf{it.isNotBlank()}?:host(t.url);if(isSelected(t)){status.text=tr("Ẩn danh · Không lưu lịch sử","Incognito · No history saved");if(!u.isNullOrBlank())setAddress(u)};updateCount()}
  }
  w.webChromeClient=object:WebChromeClient(){
   override fun onReceivedTitle(v:WebView?,title:String?){if(!title.isNullOrBlank())t.title=title;updateCount()}
   override fun onShowCustomView(view:View?,callback:WebChromeClient.CustomViewCallback?){
    if(view==null||fullscreenView!=null){callback?.onCustomViewHidden();return}
    val decor=window.decorView as? FrameLayout
    if(decor==null){callback?.onCustomViewHidden();return}
    previousSystemUiVisibility=decor.systemUiVisibility
    fullscreenView=view;fullscreenCallback=callback
    browserRoot?.visibility=View.GONE
    decor.addView(view,FrameLayout.LayoutParams(-1,-1))
    @Suppress("DEPRECATION")
    decor.systemUiVisibility=(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
     View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
     View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
   }
   override fun onHideCustomView(){hideFullscreenView()}
  }
  selectTab(tabs.lastIndex)
  if(url.isNullOrBlank()) w.loadDataWithBaseURL("https://tgfn.browser/","<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>body{font:16px Arial;background:${if (darkMode) "#202124;color:#e8eaed" else "#f8f5ff;color:#202124"};padding:15vh 20px}h2{text-align:center;color:#673ab7}form{display:flex}input{flex:1;min-width:0;padding:12px;border-radius:24px;border:1px solid #ddd}button{background:#673ab7;color:white;border:0;border-radius:24px}</style></head><body><h2>◉ ${tr("Ẩn danh","Incognito")}</h2><form action=\"https://www.google.com/search\"><input name=\"q\" placeholder=\"${tr("Tìm kiếm riêng tư","Private search")}\"><button>${tr("Tìm","Search")}</button></form></body></html>","text/html","UTF-8",null) else w.loadUrl(url)
 }
 private fun configureWebViewTheme(settings: WebSettings) {
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
 private fun showTabs(){if(tabs.isEmpty())return;android.app.AlertDialog.Builder(this).setTitle(tr("Tab ẩn danh (","Incognito tabs (")+tabs.size+")").setItems(tabs.mapIndexed{i,t->(i+1).toString()+". "+t.title+" · "+host(t.web.url?:t.url)}.toTypedArray()){_,i->selectTab(i)}.setPositiveButton(tr("Tab mới","New tab")){_,_->addTab(null)}.setNegativeButton(tr("Đóng","Close"),null).show()}
 private fun selectTab(i:Int){if(i !in tabs.indices)return;selected=i;tabs.forEachIndexed{n,t->t.web.visibility=if(n==i)View.VISIBLE else View.GONE};setAddress(tabs[i].web.url?:tabs[i].url);status.text=tabs[i].title;updateCount()}
 private fun closeTab(i:Int){if(i !in tabs.indices){if(tabs.isEmpty())finish();return};val t=tabs.removeAt(i);(t.web.parent as? ViewGroup)?.removeView(t.web);t.web.stopLoading();t.web.clearHistory();t.web.clearCache(true);t.web.clearFormData();t.web.destroy();if(tabs.isEmpty()){selected=-1;addTab(null)}else{if(i<selected)selected-- else if(i==selected)selected=(i-1).coerceAtLeast(0);selectTab(selected.coerceIn(0,tabs.lastIndex))};updateCount()}
 private fun navigate(){val raw=address.text.toString().trim();if(raw.isBlank())return;val u=if(raw.contains("://"))raw else if(raw.contains(" ")||!raw.contains("."))"https://www.google.com/search?q="+java.net.URLEncoder.encode(raw,"UTF-8") else "https://"+raw;try{val p=URI(u);if((p.scheme.equals("http",true)||p.scheme.equals("https",true))&&!p.host.isNullOrBlank())current()?.loadUrl(u)else Toast.makeText(this,tr("Chỉ hỗ trợ HTTP/HTTPS","Only HTTP/HTTPS is supported"),Toast.LENGTH_SHORT).show()}catch(_:Exception){Toast.makeText(this,tr("URL không hợp lệ","Invalid URL"),Toast.LENGTH_SHORT).show()}}
 private fun current()=tabs.getOrNull(selected)?.web
 private fun isSelected(t:Tab)=tabs.getOrNull(selected)===t
 private fun setAddress(s:String?){val v=s.orEmpty();if(::address.isInitialized&&address.text.toString()!=v)address.setText(v)}
 private fun updateCount(){if(::count.isInitialized)count.text=tabs.size.toString()}
 private fun host(s:String?)=try{URI(s.orEmpty()).host?:tr("Trang mới","New tab")}catch(_:Exception){tr("Trang mới","New tab")}
 private fun button(s:String,click:()->Unit)=TextView(this).apply{text=s;textSize=20f;gravity=Gravity.CENTER;setTextColor(if (darkMode) Color.rgb(187,134,252) else purple);setOnClickListener{click()}}
 private fun footButton(sym:String,label:String,click:()->Unit):View{val v=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setOnClickListener{click()};contentDescription=label};v.addView(TextView(this).apply{text=sym;textSize=21f;gravity=Gravity.CENTER;setTextColor(if (darkMode) Color.rgb(187,134,252) else purple)},LinearLayout.LayoutParams(-1,dp(28)));v.addView(TextView(this).apply{text=label;textSize=9f;gravity=Gravity.CENTER;maxLines=1},LinearLayout.LayoutParams(-1,dp(18)));v.layoutParams=LinearLayout.LayoutParams(0,dp(48),1f);return v}
 private fun hideFullscreenView(){
  val view=fullscreenView?:return
  (view.parent as? ViewGroup)?.removeView(view)
  fullscreenView=null
  browserRoot?.visibility=View.VISIBLE
  @Suppress("DEPRECATION")
  window.decorView.systemUiVisibility=previousSystemUiVisibility
  val callback=fullscreenCallback
  fullscreenCallback=null
  callback?.onCustomViewHidden()
 }
 @Suppress("DEPRECATION")
 override fun onBackPressed(){if(fullscreenView!=null)hideFullscreenView() else super.onBackPressed()}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun clearPrivateData(){try{CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData()}catch(_:Exception){}}
 override fun onDestroy(){hideFullscreenView();tabs.forEach{t->(t.web.parent as? ViewGroup)?.removeView(t.web);t.web.stopLoading();t.web.clearHistory();t.web.clearCache(true);t.web.clearFormData();t.web.destroy()};tabs.clear();clearPrivateData();super.onDestroy()}
}