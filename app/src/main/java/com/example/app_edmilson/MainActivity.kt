package com.example.app_edmilson

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var imageView: ImageView
    private lateinit var playerView: PlayerView
    private lateinit var qrCodeImage: ImageView
    private lateinit var pairingUrlText: TextView
    private lateinit var pairingOverlay: View
    private lateinit var offlineOverlay: View
    private lateinit var retryButton: Button

    private lateinit var player: ExoPlayer

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val deviceId by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }
    private val pairingCode by lazy {
        prefs.getString(KEY_PAIRING_CODE, null) ?: UUID.randomUUID().toString().replace("-", "").take(10)
            .also { prefs.edit().putString(KEY_PAIRING_CODE, it).apply() }
    }
    private var accessToken: String? = null

    private var pollingJob: Job? = null
    private var playlistPlayerJob: Job? = null
    private var playlistItems: List<AdItem> = emptyList()
    private var currentPlaylistIndex: Int = 0
    private var activeItemIndex: Int = 0
    private var activeMediaType: MediaType? = null
    private var resumeVideoPositionMs: Long = 0L
    private var lastBackPressedAtMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bindViews()
        configureBackNavigation()
        configureWebView()
        configurePlayer()

        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        currentPlaylistIndex = prefs.getInt(KEY_LAST_PLAYLIST_INDEX, 0).coerceAtLeast(0)
        resumeVideoPositionMs = prefs.getLong(KEY_LAST_VIDEO_POSITION_MS, 0L).coerceAtLeast(0L)
        setupPairingOverlay()

        retryButton.setOnClickListener {
            startPolling()
        }
    }

    override fun onStart() {
        super.onStart()
        startPolling()
    }

    override fun onStop() {
        super.onStop()
        persistPlaybackState()
        pollingJob?.cancel()
        playlistPlayerJob?.cancel()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
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

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        webView = findViewById(R.id.webView)
        qrCodeImage = findViewById(R.id.qrCodeImage)
        pairingUrlText = findViewById(R.id.pairingUrlText)
        pairingOverlay = findViewById(R.id.pairingOverlay)
        offlineOverlay = findViewById(R.id.offlineOverlay)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun configurePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false
            }
        }
    }

    private fun setupPairingOverlay() {
        val pairingUrl = buildPairingUrl()
        pairingUrlText.text = pairingUrl
        qrCodeImage.setImageBitmap(createQrBitmap(pairingUrl, 768))
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                if (!isNetworkAvailable()) {
                    showOfflineOverlay()
                    delay(5_000)
                    continue
                }

                hideOfflineOverlay()

                if (accessToken.isNullOrBlank()) {
                    showPairingOverlay()
                    val pairStatus = fetchPairStatus()
                    if (pairStatus?.paired == true && !pairStatus.token.isNullOrBlank()) {
                        accessToken = pairStatus.token
                        prefs.edit().putString(KEY_ACCESS_TOKEN, pairStatus.token).apply()
                        hidePairingOverlay()
                    }
                } else {
                    hidePairingOverlay()
                }

                if (!accessToken.isNullOrBlank()) {
                    val playlistResponse = fetchPlaylist()
                    if (playlistResponse != null && playlistResponse.items.isNotEmpty()) {
                        playlistItems = playlistResponse.items
                        startPlaylistPlayerIfNeeded()
                        delay((playlistResponse.refreshAfterSeconds * 1000L).coerceAtLeast(5_000L))
                        continue
                    }
                }

                delay(BuildConfig.API_POLL_SECONDS * 1000L)
            }
        }
    }

    private fun startPlaylistPlayerIfNeeded() {
        if (playlistPlayerJob?.isActive == true) return
        playlistPlayerJob = lifecycleScope.launch {
            while (isActive) {
                val items = playlistItems
                if (items.isEmpty()) {
                    delay(1_500)
                    continue
                }

                val safeIndex = currentPlaylistIndex % items.size
                activeItemIndex = safeIndex
                val item = items[safeIndex]

                when (item.type) {
                    MediaType.IMAGE -> playImage(item)
                    MediaType.VIDEO -> playVideo(item)
                    MediaType.WEB -> playWeb(item)
                }

                currentPlaylistIndex = (safeIndex + 1) % items.size
                resumeVideoPositionMs = 0L
                prefs.edit()
                    .putInt(KEY_LAST_PLAYLIST_INDEX, currentPlaylistIndex)
                    .putLong(KEY_LAST_VIDEO_POSITION_MS, 0L)
                    .apply()
            }
        }
    }

    private suspend fun playImage(item: AdItem) {
        val bitmap = downloadBitmap(item.url) ?: return
        withContext(Dispatchers.Main) {
            activeMediaType = MediaType.IMAGE
            player.stop()
            showOnly(imageVisible = true)
            imageView.setImageBitmap(bitmap)
        }
        delay(item.durationMs ?: DEFAULT_IMAGE_DURATION_MS)
    }

    private suspend fun playVideo(item: AdItem) {
        val startAt = resumeVideoPositionMs
        withContext(Dispatchers.Main) {
            activeMediaType = MediaType.VIDEO
            showOnly(videoVisible = true)
            player.setMediaItem(MediaItem.fromUri(item.url))
            player.prepare()
            if (startAt > 0) {
                player.seekTo(startAt)
            }
            player.playWhenReady = true
        }

        val customDuration = item.durationMs
        if (customDuration != null && customDuration > 0) {
            delay(customDuration)
            withContext(Dispatchers.Main) { player.stop() }
            return
        }

        withTimeoutOrNull(DEFAULT_VIDEO_TIMEOUT_MS) {
            awaitPlayerEnd(player)
        }
    }

    private suspend fun playWeb(item: AdItem) {
        withContext(Dispatchers.Main) {
            activeMediaType = MediaType.WEB
            player.stop()
            showOnly(webVisible = true)
            webView.loadUrl(item.url)
        }
        delay(item.durationMs ?: DEFAULT_WEB_DURATION_MS)
    }

    private suspend fun fetchPairStatus(): PairStatus? = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.API_BASE_URL}tv/devices/$deviceId/status?code=$pairingCode")
        requestJson(url = url, authToken = null)?.let { body ->
            val paired = body.optBoolean("paired", false)
            val token = body.optString("accessToken").ifBlank { body.optString("token") }.ifBlank { null }
            PairStatus(paired = paired || !token.isNullOrBlank(), token = token)
        }
    }

    private suspend fun fetchPlaylist(): PlaylistResponse? = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext null
        val metrics = resources.displayMetrics
        val resolution = "${metrics.widthPixels}x${metrics.heightPixels}"
        val url = URL("${BuildConfig.API_BASE_URL}tv/devices/$deviceId/playlist?resolution=$resolution")
        val body = requestJson(url = url, authToken = token) ?: return@withContext null
        parsePlaylist(body)
    }

    private fun requestJson(url: URL, authToken: String?): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/json")
                if (!authToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }

            val code = connection.responseCode
            if (code !in 200..299) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePlaylist(body: JSONObject): PlaylistResponse {
        val refreshAfter = body.optInt("refreshAfterSeconds", BuildConfig.API_POLL_SECONDS)
        val itemsArray = when {
            body.has("items") -> body.optJSONArray("items")
            else -> JSONArray(body.optString("playlist", "[]"))
        } ?: JSONArray()

        val items = mutableListOf<AdItem>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(i) ?: continue
            val typeRaw = item.optString("type").ifBlank { item.optString("mediaType") }.lowercase()
            val mediaType = when (typeRaw) {
                "video" -> MediaType.VIDEO
                "web", "html", "url" -> MediaType.WEB
                else -> MediaType.IMAGE
            }
            val url = item.optString("url").ifBlank { item.optString("src") }
            if (url.isBlank()) continue
            val durationSeconds = item.optLong("durationSeconds", 0L)
            val durationMs = if (durationSeconds > 0) durationSeconds * 1000L else null
            items += AdItem(mediaType, url, durationMs)
        }

        return PlaylistResponse(items = items, refreshAfterSeconds = refreshAfter.coerceAtLeast(5))
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = url.toUri()
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
        }

        return@withContext try {
            connection.inputStream.use { stream ->
                BufferedInputStream(stream).use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun awaitPlayerEnd(exoPlayer: ExoPlayer) {
        suspendCancellableCoroutine { continuation: CancellableContinuation<Unit> ->
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED && continuation.isActive) {
                        exoPlayer.removeListener(this)
                        continuation.resume(Unit)
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (continuation.isActive) {
                        exoPlayer.removeListener(this)
                        continuation.resume(Unit)
                    }
                }
            }

            exoPlayer.addListener(listener)
            continuation.invokeOnCancellation { exoPlayer.removeListener(listener) }
        }
    }

    private fun buildPairingUrl(): String {
        return Uri.parse(BuildConfig.PAIRING_URL)
            .buildUpon()
            .appendQueryParameter("deviceId", deviceId)
            .appendQueryParameter("code", pairingCode)
            .build()
            .toString()
    }

    private fun createQrBitmap(content: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = createBitmap(size, size)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPressedAtMs <= BACK_PRESS_EXIT_WINDOW_MS) {
                    finishAffinity()
                    return
                }

                lastBackPressedAtMs = now
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.exit_hint),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun persistPlaybackState() {
        val editor = prefs.edit()
            .putInt(KEY_LAST_PLAYLIST_INDEX, activeItemIndex.coerceAtLeast(0))

        if (activeMediaType == MediaType.VIDEO && playerView.visibility == View.VISIBLE) {
            editor.putLong(KEY_LAST_VIDEO_POSITION_MS, player.currentPosition.coerceAtLeast(0L))
        } else {
            editor.putLong(KEY_LAST_VIDEO_POSITION_MS, 0L)
        }

        editor.apply()
    }

    private fun showOnly(
        videoVisible: Boolean = false,
        imageVisible: Boolean = false,
        webVisible: Boolean = false
    ) {
        playerView.visibility = if (videoVisible) View.VISIBLE else View.GONE
        imageView.visibility = if (imageVisible) View.VISIBLE else View.GONE
        webView.visibility = if (webVisible) View.VISIBLE else View.GONE
    }

    private fun showPairingOverlay() {
        pairingOverlay.visibility = View.VISIBLE
    }

    private fun hidePairingOverlay() {
        pairingOverlay.visibility = View.GONE
    }

    private fun showOfflineOverlay() {
        offlineOverlay.visibility = View.VISIBLE
    }

    private fun hideOfflineOverlay() {
        offlineOverlay.visibility = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private data class PairStatus(val paired: Boolean, val token: String?)
    private data class AdItem(val type: MediaType, val url: String, val durationMs: Long?)
    private data class PlaylistResponse(val items: List<AdItem>, val refreshAfterSeconds: Int)

    private enum class MediaType {
        IMAGE, VIDEO, WEB
    }

    companion object {
        private const val PREFS_NAME = "kiosk_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_PAIRING_CODE = "pairing_code"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LAST_PLAYLIST_INDEX = "last_playlist_index"
        private const val KEY_LAST_VIDEO_POSITION_MS = "last_video_position_ms"

        private const val DEFAULT_IMAGE_DURATION_MS = 10_000L
        private const val DEFAULT_WEB_DURATION_MS = 15_000L
        private const val DEFAULT_VIDEO_TIMEOUT_MS = 120_000L
        private const val BACK_PRESS_EXIT_WINDOW_MS = 2_000L
    }
}
