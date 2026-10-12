package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.YouTubeRepository
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullScreenBrowserScreen(
    initialUrl: String = "",
    roomCode: String,
    roomName: String = "Watch Party",
    isVoiceActive: Boolean = false,
    isHost: Boolean = true,
    onBackToRoom: () -> Unit,
    onPlayVideoInRoom: (url: String, videoId: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val homeUrl = "https://duckduckgo.com"
    val defaultUrl = initialUrl.ifBlank { homeUrl }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf(defaultUrl) }
    var loadedTitle by remember { mutableStateOf("Web Browser") }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var validationNotice by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun navigateTo(raw: String) {
        validationNotice = null
        val trimmed = raw.trim()
        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
        inputUrl = targetUrl
        webViewRef?.loadUrl(targetUrl)
        focusManager.clearFocus()
    }

    fun handlePlayInRoom() {
        val currentUrl = webViewRef?.url ?: inputUrl
        val currentTitle = webViewRef?.title ?: loadedTitle

        // Check if current URL is a YouTube video
        val ytVideoId = YouTubeRepository.extractVideoId(currentUrl)
        if (ytVideoId != null) {
            onPlayVideoInRoom(currentUrl, ytVideoId, currentTitle.ifBlank { "YouTube Video" })
            return
        }

        // If user is on YouTube site but hasn't opened a video yet
        val lower = currentUrl.lowercase()
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            validationNotice = "Please tap and open a video first on YouTube before tapping 'Play in Room'."
            Toast.makeText(context, "Open a specific video first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for direct playable video formats
        val isDirectVideo = lower.endsWith(".mp4") || lower.endsWith(".webm") ||
                lower.endsWith(".m3u8") || lower.contains(".mp4?") || lower.contains(".webm?")

        if (isDirectVideo) {
            onPlayVideoInRoom(currentUrl, "", currentTitle.ifBlank { "Web Video" })
            return
        }

        // Check for Vimeo video pages
        if (lower.contains("vimeo.com/") && currentUrl.split("/").lastOrNull()?.toLongOrNull() != null) {
            onPlayVideoInRoom(currentUrl, "", currentTitle.ifBlank { "Vimeo Video" })
            return
        }

        // Check for Dailymotion video pages
        if (lower.contains("dailymotion.com/video/")) {
            onPlayVideoInRoom(currentUrl, "", currentTitle.ifBlank { "Dailymotion Video" })
            return
        }

        // Not a playable video page
        validationNotice = "Please navigate to a video page (YouTube, Vimeo, Dailymotion, or direct MP4/WebM) to play in the room."
        Toast.makeText(context, "Open a video page before tapping 'Play in Room'", Toast.LENGTH_SHORT).show()
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onBackToRoom()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MehfilMainBackground)
    ) {
        // Dedicated Full-Screen Browser Header
        Surface(
            color = MehfilSecondary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MehfilMutedGold, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Room Background State Indicator Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onBackToRoom,
                            color = MehfilSecondary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Return to Room",
                                    tint = MehfilYellowCream,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Back to Room",
                                    color = MehfilYellowCream,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Room Status Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MehfilInputField)
                                .border(1.dp, MehfilMutedGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ROOM: $roomCode",
                                color = MehfilCreamGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (isVoiceActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MehfilInputField)
                                .border(1.dp, MehfilMutedGold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = MehfilYellowCream,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice Active",
                                color = MehfilYellowCream,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // URL / Search Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navigateTo(homeUrl) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MehfilYellowCream
                        )
                    }

                    // Address & Search Text Field in Deep Maroon #4A0C20 with Muted Gold border
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 2.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = MehfilYellowCream),
                        placeholder = { Text("Search or type URL...", fontSize = 11.sp, color = MehfilYellowCream.copy(alpha = 0.7f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (inputUrl.startsWith("https")) Icons.Default.Lock else Icons.Default.Language,
                                contentDescription = null,
                                tint = MehfilYellowCream,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = {
                            if (inputUrl.isNotBlank()) {
                                IconButton(onClick = { inputUrl = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MehfilYellowCream,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { navigateTo(inputUrl) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MehfilCreamGold,
                            unfocusedBorderColor = MehfilMutedGold,
                            focusedTextColor = MehfilYellowCream,
                            unfocusedTextColor = MehfilYellowCream,
                            focusedContainerColor = MehfilInputField,
                            unfocusedContainerColor = MehfilInputField
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    IconButton(
                        onClick = { navigateTo(inputUrl) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Go",
                            tint = MehfilYellowCream
                        )
                    }
                }

                // Sub-Bar: History Navigation, Page Title, and Prominent "Play in Room" Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                            enabled = webViewRef?.canGoBack() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "History Back",
                                tint = if (webViewRef?.canGoBack() == true) MehfilYellowCream else MehfilYellowCream.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                            enabled = webViewRef?.canGoForward() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "History Forward",
                                tint = if (webViewRef?.canGoForward() == true) MehfilYellowCream else MehfilYellowCream.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = MehfilYellowCream,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = loadedTitle,
                            color = MehfilYellowCream,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                    }

                    // The Primary "Play in Room" Action Button in Maroon #701F32 with Yellow-Cream text
                    Button(
                        onClick = { handlePlayInRoom() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MehfilSecondary,
                            contentColor = MehfilYellowCream
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .border(1.dp, MehfilMutedGold, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MehfilYellowCream,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play in Room",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MehfilYellowCream
                        )
                    }
                }

                // Quick Website Jump Bookmarks
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        QuickBookmarkChip("DuckDuckGo") { navigateTo("https://duckduckgo.com") }
                    }
                    item {
                        QuickBookmarkChip("YouTube") { navigateTo("https://m.youtube.com") }
                    }
                    item {
                        QuickBookmarkChip("Vimeo") { navigateTo("https://vimeo.com/watch") }
                    }
                    item {
                        QuickBookmarkChip("Dailymotion") { navigateTo("https://www.dailymotion.com") }
                    }
                    item {
                        QuickBookmarkChip("Big Buck Bunny (MP4)") {
                            navigateTo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        }
                    }
                }
            }
        }

        // Loading Progress Indicator
        if (isLoading) {
            LinearProgressIndicator(
                progress = { pageProgress },
                color = MehfilCreamGold,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // Validation Notice Banner
        validationNotice?.let { notice ->
            Surface(
                color = MehfilInputField,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MehfilMutedGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MehfilCreamGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = notice,
                        color = MehfilYellowCream,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { validationNotice = null }, modifier = Modifier.size(24.dp)) {
                        Text("✕", color = MehfilYellowCream, fontSize = 11.sp)
                    }
                }
            }
        }

        // Full-Screen Dedicated Web View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Standard mobile Chrome user-agent: enables public video browsing without forced Google sign-in
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    loadedTitle = title
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                validationNotice = null
                                url?.let { inputUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { inputUrl = it }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                // Ignore non-fatal resource errors
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                android.util.Log.w("FullScreenBrowser", "Renderer process crashed, gracefully recovering...")
                                view?.destroy()
                                return true
                            }
                        }

                        loadUrl(inputUrl)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}

@Composable
private fun QuickBookmarkChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MehfilInputField,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
    ) {
        Text(
            text = label,
            color = MehfilYellowCream,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
