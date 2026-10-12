package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PeerSignal
import com.example.data.model.RoomChatMessage
import com.example.data.model.RoomParticipant
import com.example.data.model.UserProfile
import com.example.data.model.WatchRoom
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RoomRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(user: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = mapOf(
                "id" to uid,
                "displayName" to user.displayName.ifBlank { "Guest User" },
                "email" to user.email,
                "photoUrl" to user.photoUrl,
                "createdAt" to (user.createdAt ?: FieldValue.serverTimestamp()),
                "lastSeen" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createRoom(
        roomName: String,
        hostUid: String,
        hostName: String,
        hostAvatar: String
    ): Result<WatchRoom> {
        val uid = requireUserId()
        val roomId = UUID.randomUUID().toString()
        val roomCode = (100000..999999).random().toString()
        val docRef = db.collection("rooms").document(roomId)

        val roomData = WatchRoom(
            id = roomId,
            roomCode = roomCode,
            roomName = roomName.ifBlank { "$hostName's Mehfil" },
            hostUid = uid,
            hostName = hostName,
            hostAvatar = hostAvatar,
            currentUrl = "",
            currentTitle = "Ready for Video",
            videoId = "",
            isPlaying = false,
            playbackPositionMs = 0L,
            hostCameraActive = false,
            hostMicActive = true,
            hostMuteAll = false,
            allowUnmute = true,
            sharedBrowserActive = false
        )

        return try {
            val payload = mapOf(
                "id" to roomId,
                "roomCode" to roomCode,
                "roomName" to roomData.roomName,
                "hostUid" to uid,
                "hostName" to hostName,
                "hostAvatar" to hostAvatar,
                "currentUrl" to roomData.currentUrl,
                "currentTitle" to roomData.currentTitle,
                "videoId" to roomData.videoId,
                "isPlaying" to roomData.isPlaying,
                "playbackPositionMs" to roomData.playbackPositionMs,
                "lastPlaybackUpdated" to FieldValue.serverTimestamp(),
                "hostCameraActive" to roomData.hostCameraActive,
                "hostMicActive" to roomData.hostMicActive,
                "hostMuteAll" to roomData.hostMuteAll,
                "allowUnmute" to roomData.allowUnmute,
                "sharedBrowserActive" to roomData.sharedBrowserActive,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Also join host as participant
            joinParticipant(roomId, uid, hostName, hostAvatar, isHost = true)

            Result.success(roomData)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(roomCode: String, user: UserProfile): Result<WatchRoom> {
        requireUserId()
        val roomsRef = db.collection("rooms")
        return try {
            val snapshot = roomsRef.whereEqualTo("roomCode", roomCode.trim()).get().await()
            if (snapshot.isEmpty) {
                Result.failure(IllegalArgumentException("Room with code '$roomCode' not found"))
            } else {
                val doc = snapshot.documents.first()
                val room = doc.toObject(WatchRoom::class.java)?.copy(id = doc.id)
                    ?: throw IllegalStateException("Could not parse room")
                joinParticipant(room.id, user.id, user.displayName, user.photoUrl, isHost = (room.hostUid == user.id))
                Result.success(room)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, roomsRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinParticipant(
        roomId: String,
        uid: String,
        displayName: String,
        photoUrl: String,
        isHost: Boolean
    ) {
        val docRef = db.collection("rooms").document(roomId).collection("participants").document(uid)
        try {
            val payload = mapOf(
                "id" to uid,
                "roomId" to roomId,
                "displayName" to displayName,
                "photoUrl" to photoUrl,
                "isHost" to isHost,
                "isMicMuted" to false,
                "isSpeaking" to false,
                "isHostMuted" to false,
                "joinedAt" to FieldValue.serverTimestamp(),
                "lastSeen" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeRoom(roomId: String): Flow<WatchRoom?> {
        val docRef = db.collection("rooms").document(roomId)
        return docRef.snapshots()
            .map { it.toObject(WatchRoom::class.java)?.copy(id = it.id) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
                emit(null)
            }
    }

    fun observeParticipants(roomId: String): Flow<List<RoomParticipant>> {
        val colRef = db.collection("rooms").document(roomId).collection("participants")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(RoomParticipant::class.java)?.copy(id = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, colRef.path)
                emit(emptyList())
            }
    }

    suspend fun updateParticipantMic(
        roomId: String,
        participantId: String,
        isMuted: Boolean,
        isSpeaking: Boolean
    ) {
        val docRef = db.collection("rooms").document(roomId).collection("participants").document(participantId)
        try {
            docRef.update(
                mapOf(
                    "isMicMuted" to isMuted,
                    "isSpeaking" to isSpeaking,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun setHostMuteParticipant(roomId: String, participantId: String, isHostMuted: Boolean) {
        val docRef = db.collection("rooms").document(roomId).collection("participants").document(participantId)
        try {
            docRef.update(
                mapOf(
                    "isHostMuted" to isHostMuted,
                    "isMicMuted" to (if (isHostMuted) true else false)
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun setHostMuteAll(roomId: String, muteAll: Boolean, allowUnmute: Boolean) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "hostMuteAll" to muteAll,
                    "allowUnmute" to allowUnmute,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun updatePlayback(
        roomId: String,
        isPlaying: Boolean,
        positionMs: Long,
        videoId: String = "",
        currentUrl: String = "",
        title: String = ""
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            val updates = mutableMapOf<String, Any>(
                "isPlaying" to isPlaying,
                "playbackPositionMs" to positionMs,
                "lastPlaybackUpdated" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (videoId.isNotBlank()) updates["videoId"] = videoId
            if (currentUrl.isNotBlank()) updates["currentUrl"] = currentUrl
            if (title.isNotBlank()) updates["currentTitle"] = title

            roomRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun updateHostCamera(roomId: String, active: Boolean) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "hostCameraActive" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun updateSharedBrowser(
        roomId: String,
        active: Boolean,
        url: String,
        title: String
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "sharedBrowserActive" to active,
                    "currentUrl" to url,
                    "currentTitle" to title,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun sendChatMessage(
        roomId: String,
        senderUid: String,
        senderName: String,
        senderAvatar: String,
        text: String
    ) {
        val colRef = db.collection("rooms").document(roomId).collection("messages")
        val msgId = UUID.randomUUID().toString()
        try {
            val payload = mapOf(
                "id" to msgId,
                "roomId" to roomId,
                "senderUid" to senderUid,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "text" to text.trim(),
                "timestamp" to FieldValue.serverTimestamp()
            )
            colRef.document(msgId).set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
        }
    }

    fun observeChatMessages(roomId: String): Flow<List<RoomChatMessage>> {
        val colRef = db.collection("rooms").document(roomId).collection("messages")
        return colRef.orderBy("timestamp", Query.Direction.ASCENDING).snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(RoomChatMessage::class.java)?.copy(id = it.id) }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, colRef.path)
                emit(emptyList())
            }
    }

    suspend fun sendSignal(signal: PeerSignal) {
        val colRef = db.collection("rooms").document(signal.roomId).collection("signals")
        val sigId = UUID.randomUUID().toString()
        try {
            val payload = mapOf(
                "id" to sigId,
                "roomId" to signal.roomId,
                "fromUid" to signal.fromUid,
                "toUid" to signal.toUid,
                "type" to signal.type,
                "payload" to signal.payload,
                "timestamp" to FieldValue.serverTimestamp()
            )
            colRef.document(sigId).set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
        }
    }

    fun observeSignals(roomId: String, myUid: String): Flow<List<PeerSignal>> {
        val colRef = db.collection("rooms").document(roomId).collection("signals")
        return colRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(30).snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(PeerSignal::class.java)?.copy(id = it.id) }
                    .filter { it.fromUid != myUid && (it.toUid == myUid || it.toUid == "all") }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, colRef.path)
                emit(emptyList())
            }
    }

    suspend fun leaveRoom(roomId: String, participantId: String) {
        val docRef = db.collection("rooms").document(roomId).collection("participants").document(participantId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }
}
