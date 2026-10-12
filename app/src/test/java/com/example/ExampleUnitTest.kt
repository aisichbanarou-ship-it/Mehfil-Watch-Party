package com.example

import com.example.data.model.RoomParticipant
import com.example.data.model.WatchRoom
import com.example.data.repository.YouTubeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testRoomModelDefaults() {
    val room = WatchRoom(
      id = "test-123",
      roomCode = "582914",
      roomName = "Coke Studio Night",
      hostUid = "host-user-1",
      hostName = "Ali"
    )
    assertEquals("test-123", room.id)
    assertEquals("582914", room.roomCode)
    assertFalse(room.isPlaying)
    assertFalse(room.hostCameraActive)
    assertTrue(room.hostMicActive)
    assertTrue(room.allowUnmute)
  }

  @Test
  fun testParticipantModel() {
    val participant = RoomParticipant(
      id = "user-abc",
      roomId = "room-xyz",
      displayName = "Fatima",
      isHost = false,
      isMicMuted = true
    )
    assertEquals("user-abc", participant.id)
    assertFalse(participant.isHost)
    assertTrue(participant.isMicMuted)
    assertFalse(participant.isSpeaking)
  }

  @Test
  fun testYouTubeIdExtractionVariations() {
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeRepository.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ"))
  }
}
