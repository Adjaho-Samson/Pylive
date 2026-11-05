package com.pylive.music

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pylive.music.data.Favorite
import com.pylive.music.data.MusicDatabase
import com.pylive.music.data.Playlist
import com.pylive.music.data.Recent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Vérifie sur appareil que favoris, playlists et historique persistent réellement. */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: MusicDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, MusicDatabase::class.java, DB_NAME).build()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun favoritesSurviveReopen() = runBlocking {
        db.dao().favorite(Favorite("content://test/1", 1L))
        db.close()
        db = Room.databaseBuilder(context, MusicDatabase::class.java, DB_NAME).build()
        assertEquals(1, db.dao().favorites().first().size)
        db.dao().unfavorite("content://test/1")
        assertTrue(db.dao().favorites().first().isEmpty())
    }

    @Test
    fun playlistAppendRenameReorderAndCascade() = runBlocking {
        val dao = db.dao()
        val id = dao.create(Playlist(name = "Voyage", createdAt = 1L))
        dao.append(id, "a")
        dao.append(id, "b")
        dao.append(id, "a") // doublon ignoré
        assertEquals(2, dao.members(id).size)
        dao.reorder(id, 0, 1)
        assertEquals("b", dao.members(id).first().uri)
        dao.rename(id, "Route")
        assertEquals("Route", dao.playlists().first().single().name)
        dao.remove(id, "a")
        assertEquals(1, dao.members(id).size)
        dao.delete(id)
        assertTrue(dao.entries().first().isEmpty())
    }

    @Test
    fun historyIsLimitedAndClearable() = runBlocking {
        val dao = db.dao()
        dao.played(Recent("a", 1L))
        dao.played(Recent("b", 2L))
        assertEquals("b", dao.history().first().first().uri)
        dao.clearHistory()
        assertTrue(dao.history().first().isEmpty())
    }

    private companion object {
        const val DB_NAME = "pylive-test.db"
    }
}
