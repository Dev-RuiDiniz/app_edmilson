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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RendererActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var imageView: ImageView
    private lateinit var playerView: PlayerView
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
    private var exoPlayer: ExoPlayer? = null
    private var playlist: List<TvRenderContent> = emptyList()
    private var currentPlaylistIndex: Int = 0
    private var contentAdvanceJob: Job? = null

    private val exoPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            showError(getString(R.string.video_load_error))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                moveToNextContent()
            }
        }
    }

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
        cancelScheduledAdvance()
        if (this::playerView.isInitialized) {
            releasePlayer()
        }
        if (this::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    override fun onStop() {
        exoPlayer?.playWhenReady = false
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (this::playerView.isInitialized && playerView.isVisible) {
            exoPlayer?.playWhenReady = true
        }
    }

    private fun bindViews() {
        webView = findViewById(R.id.rendererWebView)
        imageView = findViewById(R.id.rendererImageView)
        playerView = findViewById(R.id.rendererPlayerView)
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
        reloadButton.setOnClickListener {
            cancelScheduledAdvance()
            viewModel.reload()
        }
        switchCodeButton.setOnClickListener {
            cancelScheduledAdvance()
            finish()
        }
        errorRetryButton.setOnClickListener {
            cancelScheduledAdvance()
            viewModel.reload()
        }
        errorSwitchCodeButton.setOnClickListener {
            cancelScheduledAdvance()
            finish()
        }
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
        cancelScheduledAdvance()
        loadingOverlay.isVisible = true
        errorOverlay.isVisible = false
        sourceHintText.isVisible = false
        stopVideoPlayback()
    }

    private fun showContent(state: RendererUiState.Success) {
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = false
        sourceHintText.isVisible = state.fromCache
        isWebContentVisible = false
        playlist = state.content.contents
        currentPlaylistIndex = 0

        if (playlist.isEmpty()) {
            showError(getString(R.string.error_loading_content))
            return
        }
        renderCurrentContent()
    }

    private fun renderCurrentContent() {
        if (playlist.isEmpty()) {
            showError(getString(R.string.error_loading_content))
            return
        }
        when (val content = playlist[currentPlaylistIndex]) {
            is TvRenderContent.Url -> {
                stopVideoPlayback()
                imageView.isVisible = false
                webView.isVisible = true
                isWebContentVisible = true
                webView.loadUrl(content.value)
                webView.requestFocus()
                scheduleNextContent()
            }
            is TvRenderContent.Html -> {
                stopVideoPlayback()
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
                scheduleNextContent()
            }
            is TvRenderContent.Image -> {
                stopVideoPlayback()
                webView.isVisible = false
                imageView.isVisible = true
                imageView.load(content.value) {
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ ->
                            scheduleNextContent()
                        },
                        onError = { _, _ ->
                            showError(getString(R.string.image_load_error))
                        }
                    )
                }
            }
            is TvRenderContent.Video -> {
                showVideo(content.value)
            }
        }
    }

    private fun showError(message: String) {
        cancelScheduledAdvance()
        stopVideoPlayback()
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = true
        errorMessage.text = message
        imageView.isVisible = false
        webView.isVisible = false
        isWebContentVisible = false
        errorRetryButton.requestFocus()
    }

    private fun showVideo(url: String) {
        imageView.isVisible = false
        webView.isVisible = false
        isWebContentVisible = false
        playerView.isVisible = true

        val player = getOrCreatePlayer()
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
        player.play()
        playerView.requestFocus()
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        return ExoPlayer.Builder(this).build().also { player ->
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.playWhenReady = true
            player.addListener(exoPlayerListener)
            playerView.player = player
            exoPlayer = player
        }
    }

    private fun stopVideoPlayback() {
        playerView.isVisible = false
        exoPlayer?.run {
            playWhenReady = false
            stop()
            clearMediaItems()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.removeListener(exoPlayerListener)
            player.release()
        }
        exoPlayer = null
        if (this::playerView.isInitialized) {
            playerView.player = null
        }
    }

    private fun scheduleNextContent() {
        cancelScheduledAdvance()
        if (playlist.isEmpty()) {
            return
        }
        contentAdvanceJob = lifecycleScope.launch {
            delay(IMAGE_DURATION_MS)
            moveToNextContent()
        }
    }

    private fun moveToNextContent() {
        if (playlist.isEmpty()) {
            return
        }
        cancelScheduledAdvance()
        currentPlaylistIndex = (currentPlaylistIndex + 1) % playlist.size
        renderCurrentContent()
    }

    private fun cancelScheduledAdvance() {
        contentAdvanceJob?.cancel()
        contentAdvanceJob = null
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
        private const val IMAGE_DURATION_MS = 30_000L

        fun newIntent(context: Context, code: String): Intent {
            return Intent(context, RendererActivity::class.java)
                .putExtra(EXTRA_TV_CODE, code)
        }
    }
}
