package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RoomChatMessage
import com.example.data.model.RoomParticipant
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream

@Composable
fun RoomChatAndParticipantsPanel(
    roomId: String,
    currentUserId: String,
    isHost: Boolean,
    participants: List<RoomParticipant>,
    chatMessages: List<RoomChatMessage>,
    hostMuteAll: Boolean,
    allowUnmute: Boolean,
    onSendMessage: (String) -> Unit,
    onHostMuteParticipant: (participantId: String, isMuted: Boolean) -> Unit,
    onHostMuteAllChange: (muteAll: Boolean, allowUnmute: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto scroll chat to bottom on new messages
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && selectedTabIndex == 0) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MehfilMainBackground)
    ) {
        // Tab Selector (Chat vs Participants)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MehfilSecondary,
            contentColor = MehfilCreamGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MehfilCreamGold,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 0) MehfilCreamGold else MehfilYellowCream,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mehfil Chat (${chatMessages.size})",
                            color = if (selectedTabIndex == 0) MehfilCreamGold else MehfilYellowCream,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 1) MehfilCreamGold else MehfilYellowCream,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Members (${participants.size})",
                            color = if (selectedTabIndex == 1) MehfilCreamGold else MehfilYellowCream,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }

        // Tab Content
        if (selectedTabIndex == 0) {
            // Chat Tab
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No messages yet. Say hello to the Mehfil!",
                                    color = MehfilYellowCream,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(chatMessages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            isMe = msg.senderUid == currentUserId
                        )
                    }
                }

                // Chat Input Bar
                Surface(
                    color = MehfilSecondary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = MehfilMutedGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Input in Deep Maroon #4A0C20 with Muted Gold border and Yellow-Cream text
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Chat with watch party...", color = MehfilYellowCream.copy(alpha = 0.7f), fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = MehfilYellowCream),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MehfilCreamGold,
                                unfocusedBorderColor = MehfilMutedGold,
                                focusedTextColor = MehfilYellowCream,
                                unfocusedTextColor = MehfilYellowCream,
                                focusedContainerColor = MehfilInputField,
                                unfocusedContainerColor = MehfilInputField
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatInput.isNotBlank()) {
                                    onSendMessage(chatInput)
                                    chatInput = ""
                                    focusManager.clearFocus()
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send button in Maroon #701F32 with Yellow-Cream icon and Muted Gold border
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendMessage(chatInput)
                                    chatInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = chatInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MehfilSecondary,
                                    CircleShape
                                )
                                .border(1.dp, MehfilMutedGold, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (chatInput.isNotBlank()) MehfilYellowCream else MehfilYellowCream.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Participants Tab
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Host Privilege Controls Banner (if user is Host)
                if (isHost) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MehfilSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MehfilCreamGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Host Microphone Privileges",
                                        color = MehfilCreamGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        onHostMuteAllChange(!hostMuteAll, allowUnmute)
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
                                    Icon(
                                        imageVector = Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = MehfilYellowCream,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (hostMuteAll) "Unmute All" else "Mute All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MehfilYellowCream
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (allowUnmute) "Participants can self-unmute" else "Unmuting is restricted by host",
                                    color = MehfilYellowCream,
                                    fontSize = 11.sp
                                )

                                OutlinedButton(
                                    onClick = {
                                        onHostMuteAllChange(hostMuteAll, !allowUnmute)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = if (allowUnmute) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MehfilYellowCream,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (allowUnmute) "Lock Unmute" else "Allow Unmute",
                                        color = MehfilYellowCream,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants, key = { it.id }) { participant ->
                        ParticipantRowItem(
                            participant = participant,
                            isHostUser = isHost,
                            onMuteToggle = {
                                onHostMuteParticipant(participant.id, !participant.isHostMuted)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: RoomChatMessage,
    isMe: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, MehfilMutedGold, CircleShape),
                shape = CircleShape,
                color = MehfilSecondary
            ) {
                if (message.senderAvatar.isNotBlank()) {
                    AsyncImage(
                        model = message.senderAvatar,
                        contentDescription = message.senderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.senderName.take(1).uppercase(),
                            color = MehfilYellowCream,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) MehfilInputField else MehfilSecondary,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    1.dp,
                    MehfilMutedGold,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        color = MehfilCreamGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    color = MehfilYellowCream,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ParticipantRowItem(
    participant: RoomParticipant,
    isHostUser: Boolean,
    onMuteToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MehfilSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(
                        if (participant.isSpeaking) {
                            Modifier.border(2.dp, MehfilCreamGold, CircleShape)
                        } else {
                            Modifier.border(1.dp, MehfilMutedGold, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MehfilInputField,
                    modifier = Modifier.size(38.dp)
                ) {
                    if (participant.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = participant.photoUrl,
                            contentDescription = participant.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = participant.displayName.take(1).uppercase(),
                                color = MehfilYellowCream,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.displayName,
                        color = MehfilYellowCream,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (participant.isHost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MehfilInputField, RoundedCornerShape(4.dp))
                                .border(1.dp, MehfilMutedGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HOST",
                                color = MehfilCreamGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                MehfilCreamGold,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            participant.isSpeaking -> "Speaking now"
                            participant.isHostMuted -> "Muted by Host"
                            participant.isMicMuted -> "Muted"
                            else -> "Mic Active"
                        },
                        color = MehfilYellowCream,
                        fontSize = 11.sp
                    )
                }
            }

            // Mic Icon
            Icon(
                imageVector = if (participant.isMicMuted || participant.isHostMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                tint = MehfilYellowCream,
                modifier = Modifier.size(20.dp)
            )

            // Host Mute/Unmute Action Button (cannot mute host)
            if (isHostUser && !participant.isHost) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onMuteToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MehfilSecondary,
                        contentColor = MehfilYellowCream
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .border(1.dp, MehfilMutedGold, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (participant.isHostMuted) "Unmute" else "Mute",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MehfilYellowCream
                    )
                }
            }
        }
    }
}
