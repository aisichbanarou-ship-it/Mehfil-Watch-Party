package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.UserProfile
import com.example.data.model.WatchRoom
import com.example.data.repository.RoomRepository
import com.example.data.repository.YouTubeRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.theme.MehfilDeepNavy
import com.example.ui.theme.MehfilPlayTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val roomRepository = RoomRepository(this)
        val youTubeRepository = YouTubeRepository()

        setContent {
            MehfilPlayTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MehfilDeepNavy
                ) {
                    MehfilPlayApp(
                        roomRepository = roomRepository,
                        youTubeRepository = youTubeRepository
                    )
                }
            }
        }
    }
}

@Composable
fun MehfilPlayApp(
    roomRepository: RoomRepository,
    youTubeRepository: YouTubeRepository
) {
    var firebaseUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var activeRoom by remember { mutableStateOf<WatchRoom?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = firebaseUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                firebaseUser = Firebase.auth.currentUser
            }
        )
    } else {
        val userProfile = remember(user.uid) {
            UserProfile(
                id = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Mehfil Guest",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
        }

        // Save profile to Firestore on login
        LaunchedEffect(user.uid) {
            scope.launch {
                try {
                    roomRepository.saveUserProfile(userProfile)
                } catch (_: Exception) {}
            }
        }

        val currentRoom = activeRoom
        if (currentRoom == null) {
            HomeScreen(
                currentUser = userProfile,
                roomRepository = roomRepository,
                youTubeRepository = youTubeRepository,
                onEnterRoom = { room ->
                    activeRoom = room
                },
                onSignOut = {
                    Firebase.auth.signOut()
                    activeRoom = null
                }
            )
        } else {
            RoomScreen(
                initialRoom = currentRoom,
                currentUser = userProfile,
                roomRepository = roomRepository,
                youTubeRepository = youTubeRepository,
                onLeaveRoom = {
                    activeRoom = null
                }
            )
        }
    }
}
