package com.pylive.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie la mécanique réelle du moteur Media3 : file, transitions, modes et
 * création de la session multimédia (notification et commandes système).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackEngineTest {

    @Test
    fun queueTransitionsModesAndSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var session: MediaSession? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val player = ExoPlayer.Builder(context).build()
            try {
                player.setMediaItems(
                    listOf(
                        MediaItem.fromUri(Uri.parse("content://test/1")),
                        MediaItem.fromUri(Uri.parse("content://test/2"))
                    )
                )
                assertEquals(0, player.currentMediaItemIndex)
                player.seekToNextMediaItem()
                assertEquals(1, player.currentMediaItemIndex)
                player.seekToPreviousMediaItem()
                assertEquals(0, player.currentMediaItemIndex)
                player.shuffleModeEnabled = true
                assertTrue(player.shuffleModeEnabled)
                player.repeatMode = Player.REPEAT_MODE_ONE
                assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
                player.moveMediaItem(0, 1)
                assertEquals(1, player.currentMediaItemIndex)
                player.removeMediaItem(0)
                assertEquals(1, player.mediaItemCount)

                session = MediaSession.Builder(context, player).build()
                assertNotNull(session?.token)
            } finally {
                session?.release()
                player.release()
            }
        }
    }
}
