package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebBrowser(
    currentUrl: String = "",
    isHost: Boolean = false,
    onHostBroadcastUrl: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultUrl = currentUrl.ifBlank { "https://duckduckgo.com" }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf(defaultUrl) }
    var loadedTitle by remember { mutableStateOf("Web Page") }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotBlank() && currentUrl != inputUrl && !isHost) {
            inputUrl = currentUrl
            webViewRef?.loadUrl(currentUrl)
        }
    }

    fun navigateTo(raw: String) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MehfilMainBackground)
    ) {
        // Navigation & Address Bar in Maroon #701F32 with Muted Gold border
        Surface(
            color = MehfilSecondary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MehfilMutedGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                        enabled = webViewRef?.canGoBack() == true,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                            contentDescription = "Forward",
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

                    // Address Bar in Deep Maroon #4A0C20 with Muted Gold border
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
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
                            IconButton(onClick = { navigateTo(inputUrl) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Go",
                                    tint = MehfilYellowCream,
                                    modifier = Modifier.size(16.dp)
                                )
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
                }

                // Host "Share This Page with Room" Banner
                if (isHost) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = MehfilCreamGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Host Browsing: $loadedTitle",
                                color = MehfilYellowCream,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Button(
                            onClick = {
                                val current = webViewRef?.url ?: inputUrl
                                val title = webViewRef?.title ?: loadedTitle
                                onHostBroadcastUrl(current, title)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MehfilSecondary,
                                contentColor = MehfilYellowCream
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = "Broadcast to Party",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MehfilYellowCream
                            )
                        }
                    }
                }
            }
        }

        // Loading Progress Bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { pageProgress },
                color = MehfilCreamGold,
                trackColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
        }

        // Web Content Area
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
                                errorMessage = null
                                url?.let { inputUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { inputUrl = it }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    errorMessage = "Could not load webpage: ${error?.description}"
                                }
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                android.util.Log.w("InAppWebBrowser", "Renderer process crashed, gracefully recovering...")
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

            // Web Error Message Overlay
            errorMessage?.let { err ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .border(1.dp, MehfilMutedGold, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MehfilSecondary
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MehfilCreamGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Web Navigation Notice",
                            color = MehfilCreamGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = err,
                            color = MehfilYellowCream,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { webViewRef?.reload() },
                            colors = ButtonDefaults.buttonColors(containerColor = MehfilSecondary, contentColor = MehfilYellowCream),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(10.dp))
                        ) {
                            Text("Retry", fontSize = 12.sp, color = MehfilYellowCream)
                        }
                    }
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
