package com.pylive.music

import com.pylive.music.playback.PlaybackState
import com.pylive.music.playback.QueueEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM de l'état de lecture exposé à l'interface. */
class PlaybackStateTest {

    @Test
    fun `etat initial sans morceau`() {
        val state = PlaybackState()
        assertFalse(state.hasTrack)
        assertFalse(state.canPlayPause)
        assertEquals(0f, state.progress, 0.0001f)
    }

    @Test
    fun `progression bornee entre 0 et 1`() {
        assertEquals(0.5f, PlaybackState(durationMs = 200_000, positionMs = 100_000).progress, 0.0001f)
        assertEquals(1f, PlaybackState(durationMs = 200_000, positionMs = 400_000).progress, 0.0001f)
        assertEquals(0f, PlaybackState(durationMs = 0, positionMs = 5_000).progress, 0.0001f)
    }

    @Test
    fun `lecture possible seulement si connecte et morceau present`() {
        val entry = QueueEntry("content://media/1", "Titre", "Artiste", 120_000)
        assertFalse(PlaybackState(uri = entry.uri, connected = false).canPlayPause)
        assertTrue(PlaybackState(uri = entry.uri, connected = true).canPlayPause)
    }
}
