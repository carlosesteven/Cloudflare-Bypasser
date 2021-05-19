package knf.tools.bypass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.kittinunf.fuel.Fuel
import com.google.android.material.bottomsheet.BottomSheetDialog
import knf.kuma.uagen.randomUA
import knf.tools.bypass.databinding.LayWebBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BypassActivity : AppCompatActivity() {

    private val layBinding by lazy { LayWebBinding.inflate(layoutInflater) }
    private val url by lazy { intent.getStringExtra("url") ?: "about:blank" }
    private val showReload by lazy { intent.getBooleanExtra("showReload", false) }
    private val useFocus by lazy { intent.getBooleanExtra("useFocus", false) }
    private val maxTryCount by lazy { intent.getIntExtra("maxTryCount", 3) }
    private val reloadOnCaptcha by lazy { intent.getBooleanExtra("reloadOnCaptcha", false) }
    private val reloadCountdown = Handler(Looper.getMainLooper())
    private var dialog: BottomSheetDialog? = null
    private val reloadRun = Runnable {
        lifecycleScope.launch(Dispatchers.Main) {
            forceReload()
        }
    }
    private var tryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!showReload)
            setTheme(R.style.Theme_Transparent)
        super.onCreate(savedInstanceState)
        layBinding.apply {
            if (showReload){
                setContentView(root)
                if (useFocus)
                    reload.requestFocus()
            }else {
                reload.hide()
                dialog = BottomSheetDialog(this@BypassActivity).apply {
                    setContentView(layBinding.root)
                    setCanceledOnTouchOutside(false)
                    behavior.apply {
                        expandedOffset = 400
                        isDraggable = false
                    }
                    show()
                }
            }
            webview.settings.apply {
                javaScriptEnabled = true
            }
            webview.webViewClient = object : WebViewClient() {

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.url?.toString()?.let { url ->
                        if (url.contains("captcha") && reloadOnCaptcha){
                            lifecycleScope.launch(Dispatchers.Main){
                                forceReload()
                            }
                        }
                        if (url.matches(".*\\?__cf_chl_\\w+_tk__=.*".toRegex())) {
                            lifecycleScope.launch(Dispatchers.Main){
                                delay(3000)
                                Fuel.get(this@BypassActivity.url)
                                    .header("User-Agent", webview.settings.userAgentString)
                                    .header("Cookie", currentCookies())
                                    .response { _, response, _ ->
                                        Log.e("Test UA bypass", "Response code: ${response.statusCode}")
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            if (response.statusCode == 200) {
                                                setResult(Activity.RESULT_CANCELED, Intent().apply {
                                                    putExtra(
                                                        "user_agent",
                                                        webview.settings.userAgentString
                                                    )
                                                    putExtra("cookies", currentCookies())
                                                })
                                                reloadCountdown.removeCallbacks(reloadRun)
                                                dialog?.dismiss()
                                                this@BypassActivity.finish()
                                            }
                                        }
                                    }
                            }

                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    reloadCountdown.removeCallbacks(reloadRun)
                    url?.let {
                        Log.e("Finish", it)
                        val cookies = currentCookies()
                        Log.e("Cookies", cookies)
                        Fuel.get(this@BypassActivity.url)
                            .header("User-Agent", webview.settings.userAgentString)
                            .header("Cookie", cookies)
                            .response { _, response, _ ->
                                Log.e("Test UA bypass", "Response code: ${response.statusCode}")
                                lifecycleScope.launch(Dispatchers.Main) {
                                    if (response.statusCode == 200) {
                                        setResult(Activity.RESULT_OK, Intent().apply {
                                            putExtra(
                                                "user_agent",
                                                webview.settings.userAgentString
                                            )
                                            putExtra("cookies", cookies)
                                        })
                                        reloadCountdown.removeCallbacks(reloadRun)
                                        dialog?.dismiss()
                                        this@BypassActivity.finish()
                                    } else {
                                        if (view?.title?.containsAny(
                                                "Just a moment...",
                                                "Verifica que no eres un bot"
                                            ) == false
                                        ) {
                                            Log.e("Bypass", "Reload")
                                            reloadCountdown.postDelayed(reloadRun, 6000)
                                            forceReload()
                                        }
                                    }
                                }
                            }
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null) {
                        tryCount++
                        Log.e("Reload","Tries: $tryCount")
                        if (tryCount >= maxTryCount) {
                            tryCount = 0
                            webview.settings.userAgentString = randomUA()
                            Log.e("Reload","Using new UA: ${webview.settings.userAgentString}")
                        }
                        //view?.loadUrl(url)
                    }
                    return super.shouldOverrideUrlLoading(view, url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return shouldOverrideUrlLoading(view, request?.url?.toString())
                }
            }
            clearCookies()
            webview.settings.userAgentString = randomUA()
            webview.loadUrl(url)
            reload.setOnClickListener {
                forceReload()
            }
        }
    }

    private fun forceReload() {
        tryCount = 0
        layBinding.webview.settings.userAgentString = randomUA()
        layBinding.webview.loadUrl(url)
    }

    private fun currentCookies(current: String = url) = try {
        CookieManager.getInstance().getCookie(current)!!
    } catch (e: Exception) {
        e.printStackTrace()
        "Null"
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    override fun onBackPressed() {
        if (useFocus && layBinding.webview.hasFocus()) {
            layBinding.reload.requestFocus()
            return
        }
        setResult(Activity.RESULT_CANCELED, Intent().apply {
            putExtra("user_agent", layBinding.webview.settings.userAgentString)
            putExtra("cookies", currentCookies())
        })
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (useFocus)
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> layBinding.webview.requestFocus()
            }
        return super.onKeyDown(keyCode, event)
    }
}

fun String.containsAny(vararg terms: String): Boolean {
    for (term in terms) {
        if (this.contains(term, true))
            return true
    }
    return false
}

fun AppCompatActivity.startBypass(
    code: Int,
    url: String,
    showReload: Boolean,
    useFocus: Boolean = false,
    maxTryCount: Int = 3,
    reloadOnCaptcha: Boolean = false
) {
    startActivityForResult(Intent(this, BypassActivity::class.java).apply {
        putExtra("url", url)
        putExtra("showReload", showReload)
        putExtra("useFocus", useFocus)
        putExtra("maxTryCount", maxTryCount)
        putExtra("reloadOnCaptcha", reloadOnCaptcha)
    }, code)
}

fun Fragment.startBypass(
    code: Int,
    url: String,
    showReload: Boolean,
    useFocus: Boolean = false,
    maxTryCount: Int = 3,
    reloadOnCaptcha: Boolean = false
) {
    startActivityForResult(Intent(requireContext(), BypassActivity::class.java).apply {
        putExtra("url", url)
        putExtra("showReload", showReload)
        putExtra("useFocus", useFocus)
        putExtra("maxTryCount", maxTryCount)
        putExtra("reloadOnCaptcha", reloadOnCaptcha)
    }, code)
}

fun startBypass(
    activity: Activity,
    code: Int,
    url: String,
    showReload: Boolean,
    useFocus: Boolean = false,
    maxTryCount: Int = 3,
    reloadOnCaptcha: Boolean = false
) {
    activity.startActivityForResult(Intent(activity, BypassActivity::class.java).apply {
        putExtra("url", url)
        putExtra("showReload", showReload)
        putExtra("useFocus", useFocus)
        putExtra("maxTryCount", maxTryCount)
        putExtra("reloadOnCaptcha", reloadOnCaptcha)
    }, code)
}