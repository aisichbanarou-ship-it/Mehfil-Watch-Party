package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioDeviceType
import com.example.audio.VoiceChatManager
import com.example.data.model.UserProfile
import com.example.data.model.WatchRoom
import com.example.data.repository.RoomRepository
import com.example.data.repository.YouTubeRepository
import com.example.ui.components.HostCameraPip
import com.example.ui.components.RoomChatAndParticipantsPanel
import com.example.ui.components.SynchronizedVideoPlayer
import com.example.ui.components.YouTubeSearchDialog
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RoomScreen(
    initialRoom: WatchRoom,
    currentUser: UserProfile,
    roomRepository: RoomRepository,
    youTubeRepository: YouTubeRepository,
    onLeaveRoom: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time Room State from Firestore
    val roomState by roomRepository.observeRoom(initialRoom.id).collectAsState(initial = initialRoom)
    val room = roomState ?: initialRoom
    val isHost = room.hostUid == currentUser.id

    val participants by roomRepository.observeParticipants(room.id).collectAsState(initial = emptyList())
    val chatMessages by roomRepository.observeChatMessages(room.id).collectAsState(initial = emptyList())
    val myParticipant = participants.find { it.id == currentUser.id }

    // Audio & Voice Chat Manager
    val voiceChatManager = remember { VoiceChatManager(context, scope) }
    val isVoiceActive by voiceChatManager.isVoiceActive.collectAsState()
    val isMicMuted by voiceChatManager.isMicMuted.collectAsState()
    val isSpeaking by voiceChatManager.isSpeaking.collectAsState()
    val videoVolumeMultiplier by voiceChatManager.videoVolume.collectAsState()
    val audioDeviceType by voiceChatManager.audioDevice.collectAsState()

    var showYouTubeSearch by remember { mutableStateOf(false) }
    var showFullScreenBrowser by remember { mutableStateOf(false) }

    // Runtime permissions for Camera & Audio
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (isHost) {
            listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            listOf(Manifest.permission.RECORD_AUDIO)
        }
    )

    // Handle host-mute enforcement from Firestore
    LaunchedEffect(myParticipant?.isHostMuted, room.hostMuteAll, room.allowUnmute) {
        val forceMuted = (myParticipant?.isHostMuted == true) || (room.hostMuteAll && !isHost && !room.allowUnmute)
        voiceChatManager.onHostMuteEnforced(forceMuted)
        if (forceMuted) {
            roomRepository.updateParticipantMic(room.id, currentUser.id, isMuted = true, isSpeaking = false)
        }
    }

    // Sync speaking & mic status to Firestore for participants to see
    LaunchedEffect(isMicMuted, isSpeaking) {
        roomRepository.updateParticipantMic(
            roomId = room.id,
            participantId = currentUser.id,
            isMuted = isMicMuted,
            isSpeaking = isSpeaking
        )
    }

    // Handle Android Back button
    BackHandler {
        if (showFullScreenBrowser) {
            showFullScreenBrowser = false
        } else {
            scope.launch {
                roomRepository.leaveRoom(room.id, currentUser.id)
                voiceChatManager.cleanup()
                onLeaveRoom()
            }
        }
    }

    DisposableEffect(room.id) {
        onDispose {
            voiceChatManager.cleanup()
        }
    }

    // Dedicated Full-Screen In-App Browser Screen (keeps watch-party alive in background)
    if (showFullScreenBrowser) {
        FullScreenBrowserScreen(
            initialUrl = room.currentUrl,
            roomCode = room.roomCode,
            roomName = room.roomName,
            isVoiceActive = isVoiceActive,
            isHost = isHost,
            onBackToRoom = { showFullScreenBrowser = false },
            onPlayVideoInRoom = { url, videoId, title ->
                showFullScreenBrowser = false
                scope.launch {
                    roomRepository.updatePlayback(
                        roomId = room.id,
                        isPlaying = true,
                        positionMs = 0L,
                        videoId = videoId,
                        currentUrl = url,
                        title = title
                    )
                }
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MehfilMainBackground)
    ) {
        // Top Room Bar
        Surface(
            color = MehfilSecondary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MehfilMutedGold, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                roomRepository.leaveRoom(room.id, currentUser.id)
                                voiceChatManager.cleanup()
                                onLeaveRoom()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave Room",
                            tint = MehfilYellowCream
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = room.roomName,
                            color = MehfilCreamGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        // Room Code Pill with Copy Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MehfilInputField)
                                .border(1.dp, MehfilMutedGold, RoundedCornerShape(6.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mehfil Room Code", room.roomCode))
                                    Toast.makeText(context, "Room Code ${room.roomCode} copied!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CODE: ${room.roomCode}",
                                color = MehfilCreamGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = MehfilYellowCream,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Top Actions: Search video, Full-Screen Browser, and Invite
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Host Video Search Button
                    if (isHost) {
                        IconButton(
                            onClick = { showYouTubeSearch = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Video",
                                tint = MehfilYellowCream
                            )
                        }
                    }

                    // Open Full-Screen Browser Button
                    IconButton(
                        onClick = { showFullScreenBrowser = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Open Full-Screen Browser",
                            tint = MehfilYellowCream
                        )
                    }

                    // Share Invite Button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my Mehfil Play watch party! Room Code: ${room.roomCode}\nWatch videos and chat together in real time!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Invite to Watch Party"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Invite Friends",
                            tint = MehfilYellowCream
                        )
                    }
                }
            }
        }

        // Host Mute Alert Banner
        AnimatedVisibility(visible = myParticipant?.isHostMuted == true || (room.hostMuteAll && !isHost)) {
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
                        text = if (myParticipant?.isHostMuted == true)
                            "Your microphone was muted by the host."
                        else
                            "Host has muted all participants in this room.",
                        color = MehfilYellowCream,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Main Synchronized Video Stage Container (No embedded 240dp browser card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // Main Stage: Synchronized Video Player
            SynchronizedVideoPlayer(
                videoId = room.videoId,
                videoUrl = room.currentUrl,
                videoTitle = room.currentTitle,
                isPlaying = room.isPlaying,
                playbackPositionMs = room.playbackPositionMs,
                lastPlaybackUpdatedMs = room.lastPlaybackUpdated?.toDate()?.time ?: 0L,
                isHost = isHost,
                videoVolumeMultiplier = videoVolumeMultiplier,
                onHostPlaybackChange = { playing, posMs ->
                    scope.launch {
                        roomRepository.updatePlayback(
                            roomId = room.id,
                            isPlaying = playing,
                            positionMs = posMs,
                            videoId = room.videoId,
                            currentUrl = room.currentUrl,
                            title = room.currentTitle
                        )
                    }
                },
                onSwitchToBrowser = {
                    showFullScreenBrowser = true
                },
                onOpenSearch = {
                    showYouTubeSearch = true
                },
                modifier = Modifier.fillMaxSize()
            )

            // Host Camera Live PIP Window (Overlaid in top-right of player stage)
            HostCameraPip(
                isHost = isHost,
                isCameraActive = room.hostCameraActive,
                hostName = room.hostName,
                onToggleCamera = { active ->
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                    scope.launch {
                        roomRepository.updateHostCamera(room.id, active)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        // Voice Chat Floating Action Controls Bar
        Surface(
            color = MehfilSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MehfilMutedGold)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Join / Leave Voice Chat Button
                Button(
                    onClick = {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                            return@Button
                        }
                        if (isVoiceActive) {
                            voiceChatManager.stopVoiceChat()
                        } else {
                            voiceChatManager.startVoiceChat()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MehfilSecondary,
                        contentColor = MehfilYellowCream
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = MehfilYellowCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVoiceActive) "Voice Active" else "Join Voice",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MehfilYellowCream
                    )
                }

                // Mic Mute / Unmute Button (with speaking pulse indicator)
                if (isVoiceActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (myParticipant?.isHostMuted == true || (room.hostMuteAll && !isHost && !room.allowUnmute)) {
                                    Toast.makeText(context, "Microphone unmuting is locked by the host", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                voiceChatManager.setMicMuted(!isMicMuted)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MehfilInputField,
                                    CircleShape
                                )
                                .border(
                                    1.5.dp,
                                    MehfilMutedGold,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Mic",
                                tint = MehfilYellowCream,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        if (isSpeaking) {
                            Text(
                                text = "Speaking",
                                color = MehfilCreamGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Audio Device Indicator (Speaker, Bluetooth, Wired Headset)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (audioDeviceType) {
                            AudioDeviceType.BLUETOOTH -> Icons.Default.Bluetooth
                            AudioDeviceType.WIRED_HEADSET -> Icons.Default.Headphones
                            else -> Icons.Default.Speaker
                        },
                        contentDescription = null,
                        tint = MehfilYellowCream,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (audioDeviceType) {
                            AudioDeviceType.BLUETOOTH -> "Bluetooth"
                            AudioDeviceType.WIRED_HEADSET -> "Headset"
                            else -> "Speaker"
                        },
                        color = MehfilYellowCream,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Real-Time Chat & Members Bottom Panel
        RoomChatAndParticipantsPanel(
            roomId = room.id,
            currentUserId = currentUser.id,
            isHost = isHost,
            participants = participants,
            chatMessages = chatMessages,
            hostMuteAll = room.hostMuteAll,
            allowUnmute = room.allowUnmute,
            onSendMessage = { text ->
                scope.launch {
                    roomRepository.sendChatMessage(
                        roomId = room.id,
                        senderUid = currentUser.id,
                        senderName = currentUser.displayName,
                        senderAvatar = currentUser.photoUrl,
                        text = text
                    )
                }
            },
            onHostMuteParticipant = { participantId, isMuted ->
                scope.launch {
                    roomRepository.setHostMuteParticipant(room.id, participantId, isMuted)
                }
            },
            onHostMuteAllChange = { muteAll, allowUnmute ->
                scope.launch {
                    roomRepository.setHostMuteAll(room.id, muteAll, allowUnmute)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    // YouTube Search Dialog
    if (showYouTubeSearch) {
        YouTubeSearchDialog(
            youTubeRepository = youTubeRepository,
            onVideoSelected = { video ->
                scope.launch {
                    roomRepository.updatePlayback(
                        roomId = room.id,
                        isPlaying = true,
                        positionMs = 0L,
                        videoId = video.id,
                        currentUrl = "https://www.youtube.com/watch?v=${video.id}",
                        title = video.title
                    )
                }
            },
            onDismiss = { showYouTubeSearch = false }
        )
    }
}
