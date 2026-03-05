package com.example.app_edmilson

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.app_edmilson.data.model.TvCodeValidator
import com.example.app_edmilson.data.model.TvRenderContent
import com.example.app_edmilson.data.repository.TvContentRepository
import com.example.app_edmilson.ui.renderer.RendererUiState
import com.example.app_edmilson.ui.renderer.RendererViewModel
import com.example.app_edmilson.ui.renderer.RendererViewModelFactory
import kotlinx.coroutines.launch

class RendererActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var imageView: ImageView
    private lateinit var loadingOverlay: View
    private lateinit var errorOverlay: View
    private lateinit var errorMessage: TextView
    private lateinit var reloadButton: Button
    private lateinit var switchCodeButton: Button
    private lateinit var errorRetryButton: Button
    private lateinit var errorSwitchCodeButton: Button
    private lateinit var sourceHintText: TextView

    private val viewModel: RendererViewModel by viewModels {
        RendererViewModelFactory(TvContentRepository.create(this))
    }

    private var tvCode: String = ""
    private var isWebContentVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_renderer)

        tvCode = TvCodeValidator.normalize(intent.getStringExtra(EXTRA_TV_CODE).orEmpty())
        if (!TvCodeValidator.isValid(tvCode)) {
            finish()
            return
        }

        bindViews()
        configureWebView()
        configureActions()
        configureBackNavigation()
        observeUiState()

        viewModel.load(tvCode)
        reloadButton.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && isWebContentVisible && handleWebViewDpad(event.keyCode)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    private fun bindViews() {
        webView = findViewById(R.id.rendererWebView)
        imageView = findViewById(R.id.rendererImageView)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        errorOverlay = findViewById(R.id.errorOverlay)
        errorMessage = findViewById(R.id.errorMessage)
        reloadButton = findViewById(R.id.reloadButton)
        switchCodeButton = findViewById(R.id.switchCodeButton)
        errorRetryButton = findViewById(R.id.errorRetryButton)
        errorSwitchCodeButton = findViewById(R.id.errorSwitchCodeButton)
        sourceHintText = findViewById(R.id.sourceHintText)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString MyTVApp".trim()
        }

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    showError(getString(R.string.webview_load_error))
                }
            }
        }

        webView.setOnKeyListener { _, keyCode, event ->
            event.action == KeyEvent.ACTION_DOWN && handleWebViewDpad(keyCode)
        }
    }

    private fun configureActions() {
        reloadButton.setOnClickListener { viewModel.reload() }
        switchCodeButton.setOnClickListener { finish() }
        errorRetryButton.setOnClickListener { viewModel.reload() }
        errorSwitchCodeButton.setOnClickListener { finish() }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isWebContentVisible && webView.canGoBack()) {
                    webView.goBack()
                    return
                }
                finish()
            }
        })
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        RendererUiState.Loading -> showLoading()
                        is RendererUiState.Success -> showContent(state)
                        is RendererUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        loadingOverlay.isVisible = true
        errorOverlay.isVisible = false
        sourceHintText.isVisible = false
    }

    private fun showContent(state: RendererUiState.Success) {
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = false
        sourceHintText.isVisible = state.fromCache
        isWebContentVisible = false

        when (val content = state.content.content) {
            is TvRenderContent.Url -> {
                imageView.isVisible = false
                webView.isVisible = true
                isWebContentVisible = true
                webView.loadUrl(content.value)
                webView.requestFocus()
            }
            is TvRenderContent.Html -> {
                imageView.isVisible = false
                webView.isVisible = true
                isWebContentVisible = true
                webView.loadDataWithBaseURL(
                    BuildConfig.API_BASE_URL,
                    content.value,
                    "text/html",
                    "UTF-8",
                    null
                )
                webView.requestFocus()
            }
            is TvRenderContent.Image -> {
                webView.isVisible = false
                imageView.isVisible = true
                imageView.load(content.value) {
                    crossfade(true)
                    listener(
                        onError = { _, _ ->
                            showError(getString(R.string.image_load_error))
                        }
                    )
                }
            }
        }
    }

    private fun showError(message: String) {
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = true
        errorMessage.text = message
        imageView.isVisible = false
        webView.isVisible = false
        isWebContentVisible = false
        errorRetryButton.requestFocus()
    }

    private fun handleWebViewDpad(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> webView.pageUp(false)
            KeyEvent.KEYCODE_DPAD_DOWN -> webView.pageDown(false)
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                webView.evaluateJavascript("window.scrollBy(-120,0);", null)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                webView.evaluateJavascript("window.scrollBy(120,0);", null)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                webView.evaluateJavascript(
                    "(function(){var el=document.activeElement;if(el&&typeof el.click==='function'){el.click();return true;}return false;})();",
                    null
                )
                true
            }
            else -> false
        }
    }

    companion object {
        private const val EXTRA_TV_CODE = "extra_tv_code"

        fun newIntent(context: Context, code: String): Intent {
            return Intent(context, RendererActivity::class.java)
                .putExtra(EXTRA_TV_CODE, code)
        }
    }
}
