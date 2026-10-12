package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val lastSeen: Timestamp? = null
)

@IgnoreExtraProperties
data class WatchRoom(
    val id: String = "",
    val roomCode: String = "",
    val roomName: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val hostAvatar: String = "",
    val currentUrl: String = "",
    val currentTitle: String = "Ready for Video",
    val videoId: String = "",
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    @ServerTimestamp val lastPlaybackUpdated: Timestamp? = null,
    val hostCameraActive: Boolean = false,
    val hostMicActive: Boolean = true,
    val hostMuteAll: Boolean = false,
    val allowUnmute: Boolean = true,
    val sharedBrowserActive: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomParticipant(
    val id: String = "",
    val roomId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isHost: Boolean = false,
    val isMicMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val isHostMuted: Boolean = false,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val lastSeen: Timestamp? = null
)

@IgnoreExtraProperties
data class RoomChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class PeerSignal(
    val id: String = "",
    val roomId: String = "",
    val fromUid: String = "",
    val toUid: String = "all",
    val type: String = "ping", // "offer", "answer", "candidate", "audio_chunk", "ping"
    val payload: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationText: String = ""
)
