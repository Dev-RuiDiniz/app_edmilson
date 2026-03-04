package com.example.app_edmilson

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var offlineOverlay: View
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = findViewById(R.id.webView)
        offlineOverlay = findViewById(R.id.offlineOverlay)
        retryButton = findViewById(R.id.retryButton)

        configureWebView()

        retryButton.setOnClickListener {
            loadConfiguredUrl()
        }

        loadConfiguredUrl()
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!webView.hasFocus()) {
                    webView.requestFocus()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        with(webView) {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                view?.loadUrl(url)
                return true
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val targetUrl = url ?: return false
                view?.loadUrl(targetUrl)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                showWebView()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showOfflineOverlay()
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                    showOfflineOverlay()
                }
            }
        }
    }

    private fun loadConfiguredUrl() {
        if (!isNetworkAvailable()) {
            showOfflineOverlay()
            return
        }

        showWebView()
        webView.loadUrl(BuildConfig.WEB_URL)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showOfflineOverlay() {
        webView.visibility = View.GONE
        offlineOverlay.visibility = View.VISIBLE
    }

    private fun showWebView() {
        offlineOverlay.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.requestFocus()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
