package com.hotspottv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.graphics.Rect
import android.view.View
import android.net.http.SslError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
import coil.request.CachePolicy
import com.hotspottv.data.model.TvCodeValidator
import com.hotspottv.data.model.TvRenderContent
import com.hotspottv.data.repository.TvContentRepository
import com.hotspottv.ui.renderer.RendererErrorClassifier
import com.hotspottv.ui.renderer.RendererErrorType
import com.hotspottv.ui.renderer.RendererPlaylistReconciler
import com.hotspottv.ui.renderer.RendererPlaylistRefreshResult
import com.hotspottv.ui.renderer.RendererPlaylistSnapshot
import com.hotspottv.ui.renderer.RendererUiState
import com.hotspottv.ui.renderer.RendererViewModel
import com.hotspottv.ui.renderer.RendererViewModelFactory
import com.hotspottv.ui.renderer.renderContentKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.util.Locale

class RendererActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var imageView: ImageView
    private lateinit var playerView: PlayerView
    private lateinit var loadingOverlay: View
    private lateinit var errorOverlay: View
    private lateinit var controlsOverlay: View
    private lateinit var errorMessage: TextView
    private lateinit var swapContentButton: Button
    private lateinit var displayDurationText: TextView
    private lateinit var countdownText: TextView
    private lateinit var homeButton: Button
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
    private var controlsHideJob: Job? = null
    private var countdownJob: Job? = null
    private var webLoadTimeoutJob: Job? = null
    private var currentRenderToken: Long = 0L
    private var activeRenderToken: Long = -1L
    private var activeContent: TvRenderContent? = null
    private var lastReportedRenderToken: Long = -1L
    private val failedContentKeys = linkedSetOf<String>()
    private var pollingJob: Job? = null
    private var initialContentResolved: Boolean = false

    private val exoPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            handleContentFailure(getString(R.string.video_load_error))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && activeContent is TvRenderContent.Video) {
                markCurrentContentHealthy()
                reportCurrentContentIfNeeded()
            }
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
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            this::controlsOverlay.isInitialized &&
            event.action == KeyEvent.ACTION_DOWN &&
            shouldShowControlsFromKey(event.keyCode)
        ) {
            if (!controlsOverlay.isVisible && shouldAllowControlsOverlay()) {
                showControlsTemporarily(requestInitialFocus = true)
                return true
            }
            if (controlsOverlay.isVisible && event.keyCode == KeyEvent.KEYCODE_MENU) {
                showControlsTemporarily()
                return true
            }
        }
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            isWebContentVisible &&
            !controlsOverlay.isVisible &&
            handleWebViewDpad(event.keyCode)
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (
            event.action == MotionEvent.ACTION_DOWN &&
            this::controlsOverlay.isInitialized &&
            controlsOverlay.isVisible &&
            isTouchInsideView(event, controlsOverlay)
        ) {
            return super.dispatchTouchEvent(event)
        }
        if (event.action == MotionEvent.ACTION_DOWN && shouldAllowControlsOverlay()) {
            if (!controlsOverlay.isVisible) {
                showControlsTemporarily(requestInitialFocus = true)
                return true
            }
            showControlsTemporarily()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() {
        stopContentPolling()
        cancelScheduledAdvance()
        cancelControlsAutoHide()
        cancelCountdown()
        cancelWebLoadTimeout()
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
        stopContentPolling()
        exoPlayer?.playWhenReady = false
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        maybeStartContentPolling()
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
        controlsOverlay = findViewById(R.id.controlsOverlay)
        errorMessage = findViewById(R.id.errorMessage)
        swapContentButton = findViewById(R.id.swapContentButton)
        displayDurationText = findViewById(R.id.displayDurationText)
        countdownText = findViewById(R.id.countdownText)
        homeButton = findViewById(R.id.homeButton)
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
            cacheMode = WebSettings.LOAD_NO_CACHE
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

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (isWebContentVisible) {
                    cancelWebLoadTimeout()
                    markCurrentContentHealthy()
                    reportCurrentContentIfNeeded()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    handleContentFailure(getString(R.string.webview_load_error))
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    handleContentFailure(getString(R.string.webview_load_error))
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                handleContentFailure(getString(R.string.webview_load_error))
            }
        }

        webView.setOnKeyListener { _, keyCode, event ->
            event.action == KeyEvent.ACTION_DOWN && handleWebViewDpad(keyCode)
        }
    }

    private fun configureActions() {
        homeButton.setOnClickListener {
            cancelScheduledAdvance()
            hideControls()
            goToHome()
        }
        swapContentButton.setOnClickListener {
            moveToNextContent()
            showControlsTemporarily()
        }
        errorRetryButton.setOnClickListener {
            cancelScheduledAdvance()
            viewModel.reload()
        }
        errorSwitchCodeButton.setOnClickListener {
            cancelScheduledAdvance()
            goToHome()
        }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (controlsOverlay.isVisible) {
                    hideControls()
                    return
                }
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
                        RendererUiState.Loading -> {
                            initialContentResolved = false
                            stopContentPolling()
                            showLoading()
                        }
                        is RendererUiState.Success -> {
                            showContent(state)
                            initialContentResolved = true
                            maybeStartContentPolling()
                        }
                        is RendererUiState.Error -> {
                            showError(resolveRendererErrorMessage(state))
                            initialContentResolved = true
                            maybeStartContentPolling()
                        }
                    }
                }
            }
        }
    }

    private fun startContentPolling() {
        if (pollingJob?.isActive == true) {
            return
        }
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                refreshContentSilently()
                delay(CONTENT_POLL_INTERVAL_MS)
            }
        }
    }

    private fun maybeStartContentPolling() {
        if (!initialContentResolved) {
            return
        }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        startContentPolling()
    }

    private fun stopContentPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refreshContentSilently() {
        val result = viewModel.refresh()
        result.onSuccess { fetchResult ->
            applyFreshContent(fetchResult.content.contents)
        }.onFailure { error ->
            if (error !is kotlinx.coroutines.CancellationException) {
                android.util.Log.d("RendererActivity", "Atualização silenciosa falhou: ${error.message}")
            }
        }
    }

    private fun showLoading() {
        cancelScheduledAdvance()
        cancelCountdown()
        cancelWebLoadTimeout()
        initialContentResolved = false
        hideControls()
        loadingOverlay.isVisible = true
        errorOverlay.isVisible = false
        sourceHintText.isVisible = false
        failedContentKeys.clear()
        stopVideoPlayback()
    }

    private fun showContent(state: RendererUiState.Success) {
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = false
        sourceHintText.isVisible = state.fromCache
        isWebContentVisible = false
        playlist = state.content.contents
        currentPlaylistIndex = 0
        failedContentKeys.clear()
        hideControls()
        updateControlsState()

        if (playlist.isEmpty()) {
            showError(getString(R.string.error_loading_content))
            return
        }
        renderCurrentContent()
    }

    private fun applyFreshContent(content: List<TvRenderContent>) {
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = false
        sourceHintText.isVisible = false

        if (content.isEmpty()) {
            showError(getString(R.string.error_loading_content))
            return
        }

        val currentPlaylist = playlist
        val snapshot = RendererPlaylistSnapshot(
            playlist = currentPlaylist,
            currentIndex = currentPlaylistIndex,
            activeContent = activeContent,
            failedContentKeys = failedContentKeys
        )
        when (val result = RendererPlaylistReconciler.reconcile(snapshot, content)) {
            RendererPlaylistRefreshResult.Empty -> {
                showError(getString(R.string.error_loading_content))
            }
            is RendererPlaylistRefreshResult.Keep -> {
                playlist = result.playlist
                currentPlaylistIndex = result.currentIndex
                failedContentKeys.clear()
                failedContentKeys.addAll(result.failedContentKeys)
                updateControlsState()
            }
            is RendererPlaylistRefreshResult.Rerender -> {
                playlist = result.playlist
                currentPlaylistIndex = result.currentIndex
                failedContentKeys.clear()
                failedContentKeys.addAll(result.failedContentKeys)
                hideControls()
                renderCurrentContent()
            }
        }
    }

    private fun renderCurrentContent() {
        if (playlist.isEmpty()) {
            showError(getString(R.string.error_loading_content))
            return
        }
        cancelScheduledAdvance()
        cancelCountdown()
        cancelWebLoadTimeout()
        val renderToken = ++currentRenderToken
        val content = playlist[currentPlaylistIndex]
        activeRenderToken = renderToken
        activeContent = content
        lastReportedRenderToken = -1L
        when (content) {
            is TvRenderContent.Url -> {
                if (content.value.isBlank()) {
                    handleContentFailure(getString(R.string.webview_load_error))
                    return
                }
                stopVideoPlayback()
                imageView.isVisible = false
                webView.isVisible = true
                isWebContentVisible = true
                startWebLoadTimeout(renderToken)
                webView.loadUrl(
                    content.value,
                    mapOf(
                        "Cache-Control" to "no-cache, no-store, must-revalidate",
                        "Pragma" to "no-cache"
                    )
                )
                webView.requestFocus()
                scheduleNextContent(content, renderToken)
            }
            is TvRenderContent.Html -> {
                if (content.value.isBlank()) {
                    handleContentFailure(getString(R.string.webview_load_error))
                    return
                }
                stopVideoPlayback()
                imageView.isVisible = false
                webView.isVisible = true
                isWebContentVisible = true
                startWebLoadTimeout(renderToken)
                webView.loadDataWithBaseURL(
                    BuildConfig.API_BASE_URL,
                    content.value,
                    "text/html",
                    "UTF-8",
                    null
                )
                webView.requestFocus()
                scheduleNextContent(content, renderToken)
            }
            is TvRenderContent.Image -> {
                if (content.value.isBlank()) {
                    handleContentFailure(getString(R.string.image_load_error))
                    return
                }
                stopVideoPlayback()
                webView.isVisible = false
                imageView.isVisible = true
                imageView.load(content.value) {
                    crossfade(true)
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                    listener(
                        onSuccess = { _, _ ->
                            if (renderToken == currentRenderToken) {
                                markCurrentContentHealthy()
                                reportCurrentContentIfNeeded(renderToken, content)
                                scheduleNextContent(content, renderToken)
                            }
                        },
                        onError = { _, _ ->
                            if (renderToken == currentRenderToken) {
                                handleContentFailure(getString(R.string.image_load_error))
                            }
                        }
                    )
                }
            }
            is TvRenderContent.Video -> {
                if (content.value.isBlank()) {
                    handleContentFailure(getString(R.string.video_load_error))
                    return
                }
                showVideo(content.value, renderToken)
            }
        }
    }

    private fun showError(message: String) {
        cancelScheduledAdvance()
        cancelCountdown()
        cancelWebLoadTimeout()
        stopVideoPlayback()
        hideControls()
        activeContent = null
        activeRenderToken = -1L
        lastReportedRenderToken = -1L
        loadingOverlay.isVisible = false
        errorOverlay.isVisible = true
        errorMessage.text = message
        imageView.isVisible = false
        webView.stopLoading()
        webView.isVisible = false
        isWebContentVisible = false
        errorRetryButton.requestFocus()
    }

    private fun resolveRendererErrorMessage(state: RendererUiState.Error): String {
        return when (RendererErrorClassifier.classify(state.cause)) {
            RendererErrorType.DEVICE_ID_REQUIRED -> getString(R.string.error_device_id_required)
            RendererErrorType.TV_LIMIT_REACHED -> getString(R.string.error_tv_limit_reached)
            RendererErrorType.DEFAULT -> state.message
        }
    }

    private fun showVideo(url: String, renderToken: Long) {
        imageView.isVisible = false
        webView.isVisible = false
        isWebContentVisible = false
        playerView.isVisible = true
        updateControlsState()

        val player = getOrCreatePlayer()
        player.setMediaItem(createVideoMediaItem(url))
        player.prepare()
        player.playWhenReady = true
        player.play()
        playerView.requestFocus()
        startVideoCountdown(renderToken)
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

    private fun scheduleNextContent(content: TvRenderContent, renderToken: Long) {
        cancelScheduledAdvance()
        cancelCountdown()
        cancelWebLoadTimeout()
        if (playlist.isEmpty()) {
            return
        }
        if (content is TvRenderContent.Video) {
            return
        }
        val displayDurationMs = resolveDisplayDurationMs(content)
        startCountdown(displayDurationMs, renderToken)
        contentAdvanceJob = lifecycleScope.launch {
            delay(displayDurationMs)
            if (renderToken == currentRenderToken) {
                moveToNextContent()
            }
        }
        updateControlsState()
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

    private fun startWebLoadTimeout(renderToken: Long) {
        cancelWebLoadTimeout()
        webLoadTimeoutJob = lifecycleScope.launch {
            delay(WEB_LOAD_TIMEOUT_MS)
            if (renderToken == currentRenderToken && isWebContentVisible) {
                handleContentFailure(getString(R.string.webview_load_error))
            }
        }
    }

    private fun cancelWebLoadTimeout() {
        webLoadTimeoutJob?.cancel()
        webLoadTimeoutJob = null
    }

    private fun startCountdown(durationMs: Long, renderToken: Long) {
        val endAt = System.currentTimeMillis() + durationMs
        updateCountdownLabel(durationMs)
        countdownJob = lifecycleScope.launch {
            while (renderToken == currentRenderToken) {
                val remainingMs = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
                updateCountdownLabel(remainingMs)
                if (remainingMs <= 0L) {
                    break
                }
                delay(1_000L)
            }
        }
    }

    private fun startVideoCountdown(renderToken: Long) {
        cancelCountdown()
        countdownJob = lifecycleScope.launch {
            while (renderToken == currentRenderToken) {
                val player = exoPlayer
                val durationMs = player?.duration ?: 0L
                val positionMs = player?.currentPosition ?: 0L
                if (durationMs > 0L) {
                    displayDurationText.text = getString(
                        R.string.display_duration_video_seconds,
                        millisToSeconds(durationMs)
                    )
                } else {
                    displayDurationText.text = getString(R.string.display_duration_video)
                }
                countdownText.text = if (durationMs > 0L) {
                    getString(
                        R.string.countdown_remaining,
                        millisToSeconds((durationMs - positionMs).coerceAtLeast(0L))
                    )
                } else {
                    getString(R.string.countdown_video_live)
                }
                delay(1_000L)
            }
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (this::countdownText.isInitialized) {
            countdownText.text = getString(R.string.countdown_unavailable)
        }
    }

    private fun resolveDisplayDurationMs(content: TvRenderContent): Long {
        return content.displayDurationMs
            ?.takeIf { it > 0 }
            ?: DEFAULT_DISPLAY_DURATION_MS
    }

    private fun createVideoMediaItem(url: String): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(url)
        inferVideoMimeType(url)?.let { mimeType ->
            builder.setMimeType(mimeType)
        }
        return builder.build()
    }

    private fun inferVideoMimeType(url: String): String? {
        val normalizedUrl = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        return when {
            normalizedUrl.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
            normalizedUrl.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            normalizedUrl.endsWith(".ism") || normalizedUrl.endsWith(".isml") -> MimeTypes.APPLICATION_SS
            normalizedUrl.endsWith(".mp4") || normalizedUrl.endsWith(".m4v") -> MimeTypes.VIDEO_MP4
            normalizedUrl.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            else -> null
        }
    }

    private fun updateControlsState() {
        if (!this::controlsOverlay.isInitialized) {
            return
        }
        val hasItems = playlist.isNotEmpty()
        swapContentButton.isEnabled = hasItems

        when (val currentContent = playlist.getOrNull(currentPlaylistIndex)) {
            is TvRenderContent.Video -> {
                displayDurationText.text = getString(R.string.display_duration_video)
            }
            is TvRenderContent.Url,
            is TvRenderContent.Html,
            is TvRenderContent.Image -> {
                displayDurationText.text = getString(
                    R.string.display_duration_seconds,
                    millisToSeconds(resolveDisplayDurationMs(currentContent))
                )
            }
            null -> {
                displayDurationText.text = getString(R.string.display_duration_default)
            }
        }
    }

    private fun updateCountdownLabel(remainingMs: Long) {
        countdownText.text = getString(
            R.string.countdown_remaining,
            millisToSeconds(remainingMs)
        )
    }

    private fun millisToSeconds(durationMs: Long): Long {
        return (durationMs + 999L) / 1_000L
    }

    private fun shouldAllowControlsOverlay(): Boolean {
        return playlist.isNotEmpty() && !loadingOverlay.isVisible && !errorOverlay.isVisible
    }

    private fun shouldShowControlsFromKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MENU -> true
            else -> false
        }
    }

    private fun showControlsTemporarily(requestInitialFocus: Boolean = false) {
        if (!shouldAllowControlsOverlay()) {
            return
        }
        updateControlsState()
        val wasVisible = controlsOverlay.isVisible
        controlsOverlay.isVisible = true
        if (requestInitialFocus || !wasVisible) {
            when {
                swapContentButton.isEnabled -> swapContentButton.requestFocus()
                else -> homeButton.requestFocus()
            }
        }
        cancelControlsAutoHide()
        controlsHideJob = lifecycleScope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsOverlay.isVisible = false
        }
    }

    private fun hideControls() {
        cancelControlsAutoHide()
        if (this::controlsOverlay.isInitialized) {
            controlsOverlay.isVisible = false
        }
    }

    private fun cancelControlsAutoHide() {
        controlsHideJob?.cancel()
        controlsHideJob = null
    }

    private fun isTouchInsideView(event: MotionEvent, view: View): Boolean {
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    private fun goToHome() {
        startActivity(MainActivity.newHomeIntent(this))
        finish()
    }

    private fun markCurrentContentHealthy() {
        activeContent?.let { failedContentKeys.remove(renderContentKey(it)) }
    }

    private fun handleContentFailure(message: String) {
        cancelScheduledAdvance()
        cancelCountdown()
        cancelWebLoadTimeout()
        stopVideoPlayback()
        activeContent?.let { failedContentKeys += renderContentKey(it) }
        if (playlist.size > 1 && failedContentKeys.size < playlist.size) {
            moveToNextAvailableContent()
            return
        }
        showError(message)
    }

    private fun moveToNextAvailableContent() {
        if (playlist.isEmpty()) {
            return
        }
        seekAndRenderNextAvailableContent(startIndex = currentPlaylistIndex + 1)
    }

    private fun seekAndRenderNextAvailableContent(startIndex: Int) {
        if (playlist.isEmpty()) {
            return
        }
        val firstIndex = if (startIndex in playlist.indices) startIndex else 0
        repeat(playlist.size) { offset ->
            val candidateIndex = if (offset == 0) {
                firstIndex
            } else {
                (firstIndex + offset) % playlist.size
            }
            if (renderContentKey(playlist[candidateIndex]) !in failedContentKeys) {
                currentPlaylistIndex = candidateIndex
                renderCurrentContent()
                return
            }
        }
        showError(getString(R.string.error_loading_content))
    }

    private fun reportCurrentContentIfNeeded(
        renderToken: Long = activeRenderToken,
        content: TvRenderContent? = activeContent
    ) {
        if (content == null) {
            return
        }
        if (renderToken != activeRenderToken || renderToken == -1L) {
            return
        }
        if (lastReportedRenderToken == renderToken) {
            return
        }
        lastReportedRenderToken = renderToken
        viewModel.reportDisplay(content)
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
        private const val DEFAULT_DISPLAY_DURATION_MS =
            BuildConfig.TV_DEFAULT_DISPLAY_DURATION_SECONDS * 1_000L
        private const val CONTENT_POLL_INTERVAL_MS = 5_000L
        private const val CONTROLS_AUTO_HIDE_MS = 4_000L
        private const val WEB_LOAD_TIMEOUT_MS = 20_000L

        fun newIntent(context: Context, code: String): Intent {
            return Intent(context, RendererActivity::class.java)
                .putExtra(EXTRA_TV_CODE, code)
        }
    }
}
