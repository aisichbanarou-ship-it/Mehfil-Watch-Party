package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.YouTubeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mehfil Play", appName)
  }

  @Test
  fun `verify youtube video id extraction`() {
    val url1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val id1 = YouTubeRepository.extractVideoId(url1)
    assertEquals("dQw4w9WgXcQ", id1)

    val url2 = "https://youtu.be/M576WGiDBdQ"
    val id2 = YouTubeRepository.extractVideoId(url2)
    assertEquals("M576WGiDBdQ", id2)

    val url3 = "jfKfPfyJRdk"
    val id3 = YouTubeRepository.extractVideoId(url3)
    assertEquals("jfKfPfyJRdk", id3)
  }
}
