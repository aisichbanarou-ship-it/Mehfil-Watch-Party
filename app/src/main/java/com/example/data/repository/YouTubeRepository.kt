package com.example.data.repository

import com.example.data.model.YouTubeVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

class YouTubeRepository(private val client: OkHttpClient = OkHttpClient()) {

    // Curated high-quality watch party videos for initial display and instant playback
    private val curatedPartyVideos = listOf(
        YouTubeVideoItem(
            id = "jfKfPfyJRdk",
            title = "lofi hip hop radio 📚 - beats to relax/study to",
            channelTitle = "Lofi Girl",
            thumbnailUrl = "https://img.youtube.com/vi/jfKfPfyJRdk/hqdefault.jpg",
            durationText = "LIVE"
        ),
        YouTubeVideoItem(
            id = "aqz-KE-bpKQ",
            title = "Big Buck Bunny 60fps 4K - Open Movie",
            channelTitle = "Blender Animation",
            thumbnailUrl = "https://img.youtube.com/vi/aqz-KE-bpKQ/hqdefault.jpg",
            durationText = "10:34"
        ),
        YouTubeVideoItem(
            id = "LXb3EKWsInQ",
            title = "COSTA RICA IN 4K 60fps HDR (ULTRA HD)",
            channelTitle = "Jacob + Katie Schwarz",
            thumbnailUrl = "https://img.youtube.com/vi/LXb3EKWsInQ/hqdefault.jpg",
            durationText = "5:14"
        ),
        YouTubeVideoItem(
            id = "kJQP7kiw5Fk",
            title = "Luis Fonsi - Despacito ft. Daddy Yankee",
            channelTitle = "Luis Fonsi",
            thumbnailUrl = "https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
            durationText = "4:42"
        ),
        YouTubeVideoItem(
            id = "fJ9rUzIMcZQ",
            title = "Queen – Bohemian Rhapsody (Official Video Remastered)",
            channelTitle = "Queen Official",
            thumbnailUrl = "https://img.youtube.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
            durationText = "5:59"
        ),
        YouTubeVideoItem(
            id = "dQw4w9WgXcQ",
            title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
            channelTitle = "Rick Astley",
            thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            durationText = "3:32"
        ),
        YouTubeVideoItem(
            id = "jNQXAC9IVRw",
            title = "Me at the zoo (First YouTube Video)",
            channelTitle = "jawed",
            thumbnailUrl = "https://img.youtube.com/vi/jNQXAC9IVRw/hqdefault.jpg",
            durationText = "0:19"
        )
    )

    suspend fun getCuratedVideos(): List<YouTubeVideoItem> {
        return curatedPartyVideos
    }

    suspend fun searchVideos(query: String): List<YouTubeVideoItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext curatedPartyVideos

        // If user entered a direct YouTube link or video ID
        val directId = extractVideoId(trimmed)
        if (directId != null) {
            return@withContext listOf(
                YouTubeVideoItem(
                    id = directId,
                    title = "Selected Video ($directId)",
                    channelTitle = "YouTube Video",
                    thumbnailUrl = "https://img.youtube.com/vi/$directId/hqdefault.jpg",
                    durationText = "Video"
                )
            )
        }

        // Live search using YouTube public suggestion and search endpoint
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val request = Request.Builder()
                .url("https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encoded")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            val results = mutableListOf<YouTubeVideoItem>()
            // Always match curated by query
            val localMatches = curatedPartyVideos.filter {
                it.title.contains(trimmed, ignoreCase = true) || it.channelTitle.contains(trimmed, ignoreCase = true)
            }
            results.addAll(localMatches)

            // Extract suggestions to generate dynamic video entries
            val pattern = Pattern.compile("\\[\"([^\"]+)\"")
            val matcher = pattern.matcher(body)
            var count = 0
            while (matcher.find() && count < 8) {
                val suggestion = matcher.group(1) ?: continue
                if (suggestion != trimmed && results.none { it.title.equals(suggestion, ignoreCase = true) }) {
                    // Generate searchable item
                    results.add(
                        YouTubeVideoItem(
                            id = "search_${count}_${suggestion.hashCode()}",
                            title = suggestion,
                            channelTitle = "Suggested for '$trimmed'",
                            thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                            durationText = "Watch"
                        )
                    )
                    count++
                }
            }

            if (results.isEmpty()) {
                curatedPartyVideos
            } else {
                results
            }
        } catch (e: Exception) {
            curatedPartyVideos.filter {
                it.title.contains(trimmed, ignoreCase = true) || it.channelTitle.contains(trimmed, ignoreCase = true)
            }.ifEmpty { curatedPartyVideos }
        }
    }

    companion object {
        fun extractVideoId(urlOrId: String): String? {
            val trimmed = urlOrId.trim()
            if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?")) {
                return trimmed
            }
            val patterns = listOf(
                "v=([a-zA-Z0-9_-]{11})",
                "youtu\\.be/([a-zA-Z0-9_-]{11})",
                "embed/([a-zA-Z0-9_-]{11})",
                "shorts/([a-zA-Z0-9_-]{11})",
                "live/([a-zA-Z0-9_-]{11})"
            )
            for (p in patterns) {
                val matcher = Pattern.compile(p).matcher(trimmed)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            return null
        }
    }
}
