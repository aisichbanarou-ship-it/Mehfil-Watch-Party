package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream
import kotlin.math.abs

class WebAppInterface(
    private val onPlayerReady: () -> Unit,
    private val onStateChange: (Int) -> Unit,
    private val onTimeUpdate: (Float, Float) -> Unit,
    private val onError: (Int) -> Unit,
    private val onAutoplayBlocked: () -> Unit
) {
    @JavascriptInterface
    fun onReady() {
        onPlayerReady()
    }

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        onStateChange(state)
    }

    @JavascriptInterface
    fun onCurrentTime(current: Float, duration: Float) {
        onTimeUpdate(current, duration)
    }

    @JavascriptInterface
    fun onPlayerError(errorCode: Int) {
        onError(errorCode)
    }

    @JavascriptInterface
    fun onAutoplayRestricted() {
        onAutoplayBlocked()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SynchronizedVideoPlayer(
    videoId: String = "",
    videoUrl: String = "",
    videoTitle: String = "",
    isPlaying: Boolean,
    playbackPositionMs: Long,
    lastPlaybackUpdatedMs: Long,
    isHost: Boolean,
    videoVolumeMultiplier: Float, // 0.25f when voice active, 1.0f normal
    onHostPlaybackChange: (isPlaying: Boolean, positionMs: Long) -> Unit,
    onSwitchToBrowser: (() -> Unit)? = null,
    onOpenSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // If no video selected yet, show empty stage
    if (videoId.isBlank() && videoUrl.isBlank()) {
        EmptyStageView(
            isHost = isHost,
            onOpenBrowser = onSwitchToBrowser,
            onOpenSearch = onOpenSearch,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var playerState by remember { mutableIntStateOf(-1) }
    var currentDurationSec by remember { mutableFloatStateOf(180f) }
    var localCurrentTimeSec by remember { mutableFloatStateOf(0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isEmbedRestricted by remember { mutableStateOf(false) }
    var isAutoplayBlocked by remember { mutableStateOf(false) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPositionSec by remember { mutableFloatStateOf(0f) }

    val isYouTube = videoId.isNotBlank()

    // HTML Content configured for YouTube IFrame API or HTML5 video
    // Includes strict-origin-when-cross-origin referrer policy, valid origin and proper client headers to resolve Error 153
    val htmlContent = remember(videoId, videoUrl) {
        if (isYouTube) {
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background: #5A1028; }
                    html, body { width: 100%; height: 100%; overflow: hidden; background: #5A1028; }
                    #player { width: 100%; height: 100%; border: 0; }
                </style>
            </head>
            <body>
                <iframe id="player"
                    type="text/html"
                    width="100%"
                    height="100%"
                    src="https://www.youtube.com/embed/$videoId?enablejsapi=1&origin=https://www.youtube.com&playsinline=1&controls=0&rel=0&modestbranding=1&iv_load_policy=3&fs=0"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    referrerpolicy="strict-origin-when-cross-origin"
                    allowfullscreen>
                </iframe>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        try {
                            AndroidBridge.onReady();
                        } catch(e) {}
                        setInterval(function() {
                            try {
                                if (player && player.getCurrentTime && player.getDuration) {
                                    AndroidBridge.onCurrentTime(player.getCurrentTime(), player.getDuration());
                                }
                            } catch(e) {}
                        }, 500);
                    }

                    function onPlayerStateChange(event) {
                        try {
                            AndroidBridge.onPlayerStateChange(event.data);
                        } catch(e) {}
                    }

                    function onPlayerError(event) {
                        try {
                            AndroidBridge.onPlayerError(event.data);
                        } catch(e) {}
                    }

                    function jsPlay() {
                        try {
                            if (player && player.playVideo) {
                                player.playVideo();
                            }
                        } catch(e) {
                            try { AndroidBridge.onAutoplayRestricted(); } catch(err) {}
                        }
                    }

                    function jsPause() {
                        try {
                            if (player && player.pauseVideo) {
                                player.pauseVideo();
                            }
                        } catch(e) {}
                    }

                    function jsSeek(sec) {
                        try {
                            if (player && player.seekTo) {
                                player.seekTo(sec, true);
                            }
                        } catch(e) {}
                    }

                    function jsSetVolume(vol) {
                        try {
                            if (player && player.setVolume) {
                                player.setVolume(vol);
                            }
                        } catch(e) {}
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else {
            // Direct HTML5 video player for web video links (.mp4, .webm, .m3u8)
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background: #5A1028; }
                    html, body { width: 100%; height: 100%; overflow: hidden; background: #5A1028; }
                    video { width: 100%; height: 100%; object-fit: contain; background: #5A1028; }
                </style>
            </head>
            <body>
                <video id="v" playsinline src="$videoUrl"></video>
                <script>
                    var v = document.getElementById('v');
                    v.addEventListener('loadedmetadata', function() {
                        try {
                            AndroidBridge.onReady();
                            AndroidBridge.onCurrentTime(0, v.duration || 180);
                        } catch(e) {}
                    });
                    v.addEventListener('timeupdate', function() {
                        try {
                            AndroidBridge.onCurrentTime(v.currentTime, v.duration || 180);
                        } catch(e) {}
                    });
                    v.addEventListener('play', function() { try { AndroidBridge.onPlayerStateChange(1); } catch(e){} });
                    v.addEventListener('pause', function() { try { AndroidBridge.onPlayerStateChange(2); } catch(e){} });
                    v.addEventListener('waiting', function() { try { AndroidBridge.onPlayerStateChange(3); } catch(e){} });
                    v.addEventListener('ended', function() { try { AndroidBridge.onPlayerStateChange(0); } catch(e){} });
                    v.addEventListener('error', function() { try { AndroidBridge.onPlayerError(5); } catch(e){} });

                    function jsPlay() {
                        v.play().catch(function() {
                            try { AndroidBridge.onAutoplayRestricted(); } catch(err) {}
                        });
                    }
                    function jsPause() { v.pause(); }
                    function jsSeek(sec) { v.currentTime = sec; }
                    function jsSetVolume(vol) { v.volume = Math.max(0, Math.min(1, vol / 100)); }
                </script>
            </body>
            </html>
            """.trimIndent()
        }
    }

    // Audio ducking: update video volume without reloading video
    LaunchedEffect(videoVolumeMultiplier, isPlayerReady) {
        if (isPlayerReady && webViewRef != null) {
            val vol = (videoVolumeMultiplier * 100).toInt().coerceIn(0, 100)
            webViewRef?.evaluateJavascript("jsSetVolume($vol);", null)
        }
    }

    // Detect autoplay restrictions: if isPlaying is true but player remains paused/cued for >2s
    LaunchedEffect(isPlaying, isPlayerReady, playerState) {
        if (isPlaying && isPlayerReady && (playerState == 2 || playerState == 5 || playerState == -1)) {
            kotlinx.coroutines.delay(1800)
            if (isPlaying && (playerState == 2 || playerState == 5 || playerState == -1)) {
                isAutoplayBlocked = true
            }
        } else if (playerState == 1) {
            isAutoplayBlocked = false
        }
    }

    // Synchronize host authoritative playback state to participant player
    LaunchedEffect(isPlaying, playbackPositionMs, lastPlaybackUpdatedMs, isPlayerReady, isHost) {
        if (!isPlayerReady || webViewRef == null) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val elapsedMs = if (isPlaying && lastPlaybackUpdatedMs > 0) (now - lastPlaybackUpdatedMs).coerceAtLeast(0) else 0L
        val targetMs = playbackPositionMs + elapsedMs
        val targetSec = targetMs / 1000f

        if (isHost) {
            if (isPlaying) {
                webViewRef?.evaluateJavascript("jsPlay();", null)
            } else {
                webViewRef?.evaluateJavascript("jsPause();", null)
            }
        } else {
            // Participant syncing to host
            val deltaSec = abs(localCurrentTimeSec - targetSec)
            if (deltaSec > 2.5f) {
                webViewRef?.evaluateJavascript("jsSeek($targetSec);", null)
            }
            if (isPlaying) {
                webViewRef?.evaluateJavascript("jsPlay();", null)
            } else {
                webViewRef?.evaluateJavascript("jsPause();", null)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MehfilMainBackground)
            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowContentAccess = true
                        allowFileAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    addJavascriptInterface(
                        WebAppInterface(
                            onPlayerReady = {
                                isPlayerReady = true
                                playerErrorMessage = null
                                isEmbedRestricted = false
                            },
                            onStateChange = { state ->
                                playerState = state
                                isBuffering = (state == 3)
                                if (state == 1) {
                                    isAutoplayBlocked = false
                                }
                            },
                            onTimeUpdate = { current, duration ->
                                localCurrentTimeSec = current
                                if (duration > 0) currentDurationSec = duration
                            },
                            onError = { errCode ->
                                val (desc, embedBlocked) = when (errCode) {
                                    2 -> "Invalid video ID parameter or format" to false
                                    5 -> "HTML5 player error on this device" to false
                                    100 -> "Video not found, removed, or marked private by owner" to false
                                    101, 150 -> "The owner of this video has prohibited embedded playback on external sites" to true
                                    152 -> "Embedding restricted by YouTube content policy (requires viewing on original page)" to true
                                    153 -> "Missing or blocked HTTP Referer header / client origin (Error 153)" to false
                                    else -> "Video playback error (Code: $errCode)" to false
                                }
                                Log.e("YouTubePlayer", "Player error code: $errCode - Root cause: $desc")
                                playerErrorMessage = desc
                                isEmbedRestricted = embedBlocked
                            },
                            onAutoplayBlocked = {
                                isAutoplayBlocked = true
                            }
                        ),
                        "AndroidBridge"
                    )

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            Log.e("WebView", "Error loading video: ${error?.description}")
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            Log.w("WebView", "Renderer process crashed, gracefully recovering...")
                            view?.destroy()
                            return true
                        }
                    }

                    // Load with YouTube as BaseURL and HistoryURL so YouTube's Referer and Origin checks pass
                    val baseUrl = if (isYouTube) "https://www.youtube.com" else "https://duckduckgo.com"
                    loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", baseUrl)
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering || (!isPlayerReady && playerErrorMessage == null)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MehfilMainBackground.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MehfilCreamGold, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!isPlayerReady) "Loading Video Stage..." else "Buffering...",
                        color = MehfilYellowCream,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Autoplay Restriction Overlay: When user gesture is required to start playback
        AnimatedVisibility(
            visible = isAutoplayBlocked && playerErrorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Button(
                onClick = {
                    webViewRef?.evaluateJavascript("jsPlay();", null)
                    isAutoplayBlocked = false
                    if (isHost && !isPlaying) {
                        onHostPlaybackChange(true, (localCurrentTimeSec * 1000).toLong())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MehfilSecondary,
                    contentColor = MehfilYellowCream
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MehfilYellowCream,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to Start Playback",
                    color = MehfilYellowCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Error Banner: Video owner prohibited embedding (Error 101/150) or policy restriction
        playerErrorMessage?.let { err ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = MehfilSecondary
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEmbedRestricted) "Embedding Restricted" else "Playback Notice",
                        color = MehfilCreamGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        color = MehfilYellowCream,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onSwitchToBrowser != null) {
                            Button(
                                onClick = {
                                    playerErrorMessage = null
                                    onSwitchToBrowser()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MehfilSecondary,
                                    contentColor = MehfilYellowCream
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(10.dp))
                            ) {
                                Text("Open in Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MehfilYellowCream)
                            }
                        }

                        if (isYouTube) {
                            Button(
                                onClick = {
                                    try {
                                        val externalUrl = "https://www.youtube.com/watch?v=$videoId"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MehfilSecondary,
                                    contentColor = MehfilYellowCream
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = MehfilYellowCream,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open on YouTube", fontSize = 11.sp, color = MehfilYellowCream)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Custom Control Bar for Host Authoritative Playback
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MehfilInputField.copy(alpha = 0.92f))
                .border(width = 1.dp, color = MehfilMutedGold.copy(alpha = 0.5f), shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Scrubber Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentDisplay = if (isUserScrubbing) scrubPositionSec else localCurrentTimeSec
                Text(
                    text = formatTime(currentDisplay),
                    color = MehfilYellowCream,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = currentDisplay.coerceIn(0f, currentDurationSec),
                    onValueChange = { newVal ->
                        if (isHost) {
                            isUserScrubbing = true
                            scrubPositionSec = newVal
                        }
                    },
                    onValueChangeFinished = {
                        if (isHost) {
                            isUserScrubbing = false
                            val seekMs = (scrubPositionSec * 1000).toLong()
                            webViewRef?.evaluateJavascript("jsSeek($scrubPositionSec);", null)
                            onHostPlaybackChange(isPlaying, seekMs)
                        }
                    },
                    valueRange = 0f..currentDurationSec.coerceAtLeast(1f),
                    enabled = isHost,
                    colors = SliderDefaults.colors(
                        thumbColor = MehfilCreamGold,
                        activeTrackColor = MehfilCreamGold,
                        inactiveTrackColor = MehfilSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = formatTime(currentDurationSec),
                    color = MehfilYellowCream,
                    fontSize = 11.sp
                )
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHost) {
                        IconButton(
                            onClick = {
                                val newSec = (localCurrentTimeSec - 10f).coerceAtLeast(0f)
                                webViewRef?.evaluateJavascript("jsSeek($newSec);", null)
                                onHostPlaybackChange(isPlaying, (newSec * 1000).toLong())
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind 10s",
                                tint = MehfilYellowCream
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextState = !isPlaying
                                if (nextState) {
                                    webViewRef?.evaluateJavascript("jsPlay();", null)
                                } else {
                                    webViewRef?.evaluateJavascript("jsPause();", null)
                                }
                                onHostPlaybackChange(nextState, (localCurrentTimeSec * 1000).toLong())
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MehfilSecondary, CircleShape)
                                .border(1.dp, MehfilMutedGold, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MehfilYellowCream
                            )
                        }

                        IconButton(
                            onClick = {
                                val newSec = (localCurrentTimeSec + 10f).coerceAtMost(currentDurationSec)
                                webViewRef?.evaluateJavascript("jsSeek($newSec);", null)
                                onHostPlaybackChange(isPlaying, (newSec * 1000).toLong())
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward 10s",
                                tint = MehfilYellowCream
                            )
                        }
                    } else {
                        // Participant Sync Status Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Synced with Host",
                                tint = MehfilCreamGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced to Host",
                                color = MehfilYellowCream,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Volume Ducking Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (videoVolumeMultiplier < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                        contentDescription = "Volume status",
                        tint = MehfilYellowCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (videoVolumeMultiplier < 0.5f) "Ducked (25% for Voice)" else "Audio 100%",
                        color = MehfilYellowCream,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt()
    val minutes = totalSec / 60
    val remSec = totalSec % 60
    return String.format("%02d:%02d", minutes, remSec)
}

@Composable
fun EmptyStageView(
    isHost: Boolean,
    onOpenBrowser: (() -> Unit)?,
    onOpenSearch: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MehfilMainBackground)
            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = null,
                tint = MehfilCreamGold,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHost) "Watch Party Stage is Ready" else "Waiting for Host...",
                color = MehfilCreamGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHost)
                    "Open the Full-Screen Browser to find any video link, or search YouTube."
                else
                    "The host will select a video shortly. You can chat and talk in voice chat!",
                color = MehfilYellowCream,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            if (isHost) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onOpenBrowser?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MehfilSecondary,
                            contentColor = MehfilYellowCream
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = MehfilYellowCream, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Full-Screen Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MehfilYellowCream)
                    }

                    OutlinedButton(
                        onClick = { onOpenSearch?.invoke() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MehfilYellowCream, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", color = MehfilYellowCream, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
