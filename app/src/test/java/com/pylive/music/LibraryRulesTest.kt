package com.pylive.music

import com.pylive.music.domain.LibraryRules
import com.pylive.music.domain.MusicSort
import com.pylive.music.domain.PermissionPolicy
import com.pylive.music.domain.QueueRules
import com.pylive.music.domain.RepeatCycle
import com.pylive.music.domain.RepeatMode
import com.pylive.music.domain.Track
import com.pylive.music.domain.formatDuration
import com.pylive.music.domain.formatSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests JVM des règles pures : recherche, tri, références, file, formatage. */
class LibraryRulesTest {

    private val zoo = Track("a", "Zèbre", "Alice", "Live", 300_000, 1)
    private val alpha = Track("b", "Alpha", "Bob", "Studio", 100_000, 3)
    private val cote = Track("c", "Été", "Élodie", "Été", 200_000, 2)

    @Test
    fun `recherche insensible a la casse`() {
        assertEquals(listOf(zoo), LibraryRules.select(listOf(zoo, alpha), " ALICE ", MusicSort.TITLE))
    }

    @Test
    fun `recherche insensible aux accents`() {
        assertEquals("c", LibraryRules.select(listOf(zoo, alpha, cote), "ete", MusicSort.TITLE).first().uri)
        assertEquals("c", LibraryRules.select(listOf(zoo, alpha, cote), "ÉLODIE", MusicSort.TITLE).first().uri)
    }

    @Test
    fun `recherche par album et par nom de fichier`() {
        val track = alpha.copy(fileName = "01 - alpha.mp3")
        assertEquals("b", LibraryRules.select(listOf(zoo, track), "studio", MusicSort.TITLE).single().uri)
        assertEquals("b", LibraryRules.select(listOf(zoo, track), "alpha.mp3", MusicSort.TITLE).single().uri)
    }

    @Test
    fun `tri par titre par artiste par album`() {
        val all = listOf(zoo, alpha, cote)
        assertEquals("b", LibraryRules.select(all, "", MusicSort.TITLE).first().uri)
        assertEquals("a", LibraryRules.select(all, "", MusicSort.ARTIST).first().uri)
        assertEquals("c", LibraryRules.select(all, "", MusicSort.ALBUM).first().uri)
    }

    @Test
    fun `tri par duree et par date d ajout`() {
        val all = listOf(zoo, alpha, cote)
        assertEquals("b", LibraryRules.select(all, "", MusicSort.DURATION).first().uri)
        assertEquals("b", LibraryRules.select(all, "", MusicSort.ADDED).first().uri)
    }

    @Test
    fun `bibliotheque vide et recherche sans resultat`() {
        assertTrue(LibraryRules.select(emptyList(), "", MusicSort.TITLE).isEmpty())
        assertTrue(LibraryRules.select(listOf(zoo, alpha), "absent", MusicSort.TITLE).isEmpty())
    }

    @Test
    fun `references disparues ignorees et ordre conserve`() {
        val all = listOf(zoo, alpha)
        assertEquals(listOf(alpha), LibraryRules.resolve(listOf("inconnu", "b"), all))
        assertEquals(listOf("b", "a"), LibraryRules.resolve(listOf("b", "a"), all).map { it.uri })
        assertTrue(LibraryRules.resolve(emptyList(), all).isEmpty())
    }

    @Test
    fun `deduplication par uri`() {
        val duplicate = zoo.copy(title = "Autre titre")
        assertEquals(2, LibraryRules.distinct(listOf(zoo, duplicate, alpha)).size)
    }

    @Test
    fun `permission audio selon la version d android`() {
        assertEquals("android.permission.READ_MEDIA_AUDIO", PermissionPolicy.audioPermission(33))
        assertEquals("android.permission.READ_MEDIA_AUDIO", PermissionPolicy.audioPermission(35))
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", PermissionPolicy.audioPermission(32))
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", PermissionPolicy.audioPermission(26))
    }

    @Test
    fun `deplacement dans la file`() {
        assertEquals(listOf("b", "a"), QueueRules.move(listOf("a", "b"), 0, 1))
        assertEquals(listOf("a", "b"), QueueRules.move(listOf("a", "b"), 1, 5))
        assertEquals(listOf("a", "b"), QueueRules.move(listOf("a", "b"), 1, 1))
        assertEquals(listOf("a", "b"), QueueRules.move(listOf("a", "b"), 0, -1))
    }

    @Test
    fun `cycle de repetition`() {
        assertEquals(RepeatMode.ALL, RepeatCycle.next(RepeatMode.OFF))
        assertEquals(RepeatMode.ONE, RepeatCycle.next(RepeatMode.ALL))
        assertEquals(RepeatMode.OFF, RepeatCycle.next(RepeatMode.ONE))
    }

    @Test
    fun `formatage des durees`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:07", formatDuration(7_000))
        assertEquals("3:07", formatDuration(187_000))
        assertEquals("1:02:45", formatDuration(3_765_000))
        assertEquals("0:00", formatDuration(-5_000))
    }

    @Test
    fun `formatage des tailles`() {
        assertEquals("—", formatSize(0))
        assertEquals("512 o", formatSize(512))
        assertEquals("1 Ko", formatSize(1024))
        assertTrue(formatSize(5_242_880).endsWith("Mo"))
    }

    @Test
    fun `duree affichee d un morceau`() {
        assertEquals("5:00", zoo.durationLabel)
        assertFalse(zoo.durationLabel.contains("-"))
    }
}
