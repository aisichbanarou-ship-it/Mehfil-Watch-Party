package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.model.WatchRoom
import com.example.data.model.YouTubeVideoItem
import com.example.data.repository.RoomRepository
import com.example.data.repository.YouTubeRepository
import com.example.ui.theme.MehfilCreamGold
import com.example.ui.theme.MehfilInputField
import com.example.ui.theme.MehfilMainBackground
import com.example.ui.theme.MehfilMutedGold
import com.example.ui.theme.MehfilSecondary
import com.example.ui.theme.MehfilYellowCream
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentUser: UserProfile,
    roomRepository: RoomRepository,
    youTubeRepository: YouTubeRepository,
    onEnterRoom: (WatchRoom) -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var curatedVideos by remember { mutableStateOf<List<YouTubeVideoItem>>(emptyList()) }
    var isLoadingCurated by remember { mutableStateOf(false) }
    var roomActionError by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingCurated = true
        curatedVideos = youTubeRepository.getCuratedVideos()
        isLoadingCurated = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MehfilMainBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar: Brand Title & User Profile Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MehfilSecondary, CircleShape)
                            .border(1.5.dp, MehfilMutedGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = MehfilYellowCream,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mehfil Play",
                            color = MehfilCreamGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Hi, ${currentUser.displayName.ifBlank { "Host" }}",
                            color = MehfilYellowCream,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, MehfilMutedGold, CircleShape),
                        shape = CircleShape,
                        color = MehfilSecondary
                    ) {
                        if (currentUser.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentUser.photoUrl,
                                contentDescription = currentUser.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.displayName.take(1).uppercase(),
                                    color = MehfilYellowCream,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MehfilYellowCream
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error notice if any
            roomActionError?.let { err ->
                Surface(
                    color = MehfilInputField,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MehfilMutedGold, RoundedCornerShape(10.dp))
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = err,
                            color = MehfilYellowCream,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { roomActionError = null }, modifier = Modifier.size(24.dp)) {
                            Text("✕", color = MehfilYellowCream, fontSize = 12.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Action: Create Room Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MehfilSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(20.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "HOST A PARTY",
                                        color = MehfilCreamGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create Watch Party Room",
                                        color = MehfilCreamGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Generate a private 6-digit code. Control shared playback, broadcast your camera, and invite friends.",
                                        color = MehfilYellowCream,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MehfilSecondary, CircleShape)
                                        .border(1.5.dp, MehfilMutedGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MehfilYellowCream,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Secondary Action: Join Room Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MehfilSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJoinDialog = true }
                            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "JOIN MEHFIL",
                                    color = MehfilCreamGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Join with Room Code",
                                    color = MehfilCreamGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Enter a 6-digit code shared by a friend to jump straight into their party with voice chat.",
                                    color = MehfilYellowCream,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MehfilSecondary, CircleShape)
                                    .border(1.5.dp, MehfilMutedGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = MehfilYellowCream,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Curated Quick-Launch Carousel
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Popular for Watch Parties",
                                color = MehfilCreamGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to Launch",
                                color = MehfilYellowCream,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingCurated) {
                            CircularProgressIndicator(
                                color = MehfilCreamGold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(curatedVideos) { video ->
                                    QuickPlayVideoCard(
                                        video = video,
                                        onClick = {
                                            isOperating = true
                                            scope.launch {
                                                val result = roomRepository.createRoom(
                                                    roomName = "${currentUser.displayName}'s Watch Party",
                                                    hostUid = currentUser.id,
                                                    hostName = currentUser.displayName,
                                                    hostAvatar = currentUser.photoUrl
                                                )
                                                result.onSuccess { room ->
                                                    // update initial video
                                                    roomRepository.updatePlayback(
                                                        roomId = room.id,
                                                        isPlaying = true,
                                                        positionMs = 0L,
                                                        videoId = video.id,
                                                        currentUrl = "https://www.youtube.com/watch?v=${video.id}",
                                                        title = video.title
                                                    )
                                                    isOperating = false
                                                    onEnterRoom(room.copy(videoId = video.id, currentTitle = video.title))
                                                }.onFailure { err ->
                                                    isOperating = false
                                                    roomActionError = err.localizedMessage ?: "Failed to start room"
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Loading overlay if creating room
        if (isOperating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MehfilMainBackground.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MehfilCreamGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Creating your Mehfil...", color = MehfilYellowCream, fontSize = 14.sp)
                }
            }
        }
    }

    // Create Room Dialog with Deep Maroon #4A0C20 input and Muted Gold borders
    if (showCreateDialog) {
        var roomNameInput by remember { mutableStateOf("${currentUser.displayName}'s Mehfil") }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MehfilSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Create Watch Party",
                        color = MehfilCreamGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set a title for your room. You can invite friends to watch and voice chat.",
                        color = MehfilYellowCream,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        label = { Text("Room Name", color = MehfilYellowCream) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = MehfilYellowCream),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MehfilCreamGold,
                            unfocusedBorderColor = MehfilMutedGold,
                            focusedLabelColor = MehfilCreamGold,
                            unfocusedLabelColor = MehfilYellowCream,
                            focusedTextColor = MehfilYellowCream,
                            unfocusedTextColor = MehfilYellowCream,
                            focusedContainerColor = MehfilInputField,
                            unfocusedContainerColor = MehfilInputField
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showCreateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MehfilYellowCream)
                        ) {
                            Text("Cancel", color = MehfilYellowCream)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                showCreateDialog = false
                                isOperating = true
                                scope.launch {
                                    val result = roomRepository.createRoom(
                                        roomName = roomNameInput,
                                        hostUid = currentUser.id,
                                        hostName = currentUser.displayName,
                                        hostAvatar = currentUser.photoUrl
                                    )
                                    isOperating = false
                                    result.onSuccess { room ->
                                        onEnterRoom(room)
                                    }.onFailure { err ->
                                        roomActionError = err.localizedMessage ?: "Failed to create room"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MehfilSecondary,
                                contentColor = MehfilYellowCream
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                        ) {
                            Text("Create & Enter", fontWeight = FontWeight.Bold, color = MehfilYellowCream)
                        }
                    }
                }
            }
        }
    }

    // Join Room Dialog
    if (showJoinDialog) {
        var roomCodeInput by remember { mutableStateOf("") }
        var joinError by remember { mutableStateOf<String?>(null) }
        var isJoining by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showJoinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MehfilSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Join Watch Party Room",
                        color = MehfilCreamGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter the 6-digit room code given by the host.",
                        color = MehfilYellowCream,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = {
                            if (it.length <= 6) roomCodeInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("6-Digit Code (e.g. 582914)", color = MehfilYellowCream) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = MehfilYellowCream),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MehfilCreamGold,
                            unfocusedBorderColor = MehfilMutedGold,
                            focusedLabelColor = MehfilCreamGold,
                            unfocusedLabelColor = MehfilYellowCream,
                            focusedTextColor = MehfilYellowCream,
                            unfocusedTextColor = MehfilYellowCream,
                            focusedContainerColor = MehfilInputField,
                            unfocusedContainerColor = MehfilInputField
                        )
                    )

                    if (joinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = joinError ?: "", color = MehfilYellowCream, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showJoinDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MehfilYellowCream)
                        ) {
                            Text("Cancel", color = MehfilYellowCream)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (roomCodeInput.length != 6) {
                                    joinError = "Please enter a valid 6-digit room code"
                                    return@Button
                                }
                                joinError = null
                                isJoining = true
                                scope.launch {
                                    val result = roomRepository.joinRoomByCode(roomCodeInput, currentUser)
                                    isJoining = false
                                    result.onSuccess { room ->
                                        showJoinDialog = false
                                        onEnterRoom(room)
                                    }.onFailure { err ->
                                        joinError = err.localizedMessage ?: "Could not join room"
                                    }
                                }
                            },
                            enabled = !isJoining,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MehfilSecondary,
                                contentColor = MehfilYellowCream
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, MehfilMutedGold, RoundedCornerShape(12.dp))
                        ) {
                            if (isJoining) {
                                CircularProgressIndicator(color = MehfilYellowCream, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Join Room", fontWeight = FontWeight.Bold, color = MehfilYellowCream)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPlayVideoCard(
    video: YouTubeVideoItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MehfilSecondary,
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
            .border(1.5.dp, MehfilMutedGold, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MehfilInputField)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MehfilMainBackground.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MehfilSecondary, CircleShape)
                            .border(1.dp, MehfilMutedGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MehfilYellowCream,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (video.durationText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(MehfilInputField.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, MehfilMutedGold, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            color = MehfilYellowCream,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = video.title,
                color = MehfilCreamGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.channelTitle,
                color = MehfilYellowCream,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
