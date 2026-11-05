package com.pylive.music.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/*
 * Base locale Pylive Music : uniquement des références (URI) vers les fichiers
 * audio du téléphone. Aucun son n'est copié en base, aucun accès réseau.
 * Ne jamais activer fallbackToDestructiveMigration : toute évolution passe par
 * une migration explicite et un schéma exporté.
 */

@Entity(tableName = "favorite")
data class Favorite(@PrimaryKey val uri: String, val addedAt: Long = 0L)

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = 0L
)

@Entity(
    tableName = "playlist_track",
    primaryKeys = ["playlistId", "uri"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("playlistId")]
)
data class PlaylistTrack(val playlistId: Long, val uri: String, val position: Int)

@Entity(tableName = "recent")
data class Recent(@PrimaryKey val uri: String, val playedAt: Long)

@Dao
interface MusicDao {

    // --- Favoris -----------------------------------------------------------
    @Query("SELECT * FROM favorite ORDER BY addedAt DESC")
    fun favorites(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun favorite(value: Favorite)

    @Query("DELETE FROM favorite WHERE uri = :uri")
    suspend fun unfavorite(uri: String)

    // --- Playlists ---------------------------------------------------------
    @Query("SELECT * FROM playlist ORDER BY name COLLATE NOCASE")
    fun playlists(): Flow<List<Playlist>>

    @Insert
    suspend fun create(value: Playlist): Long

    @Query("UPDATE playlist SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_track ORDER BY position")
    fun entries(): Flow<List<PlaylistTrack>>

    @Query("SELECT * FROM playlist_track WHERE playlistId = :id ORDER BY position")
    suspend fun members(id: Long): List<PlaylistTrack>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(value: PlaylistTrack)

    @Query("DELETE FROM playlist_track WHERE playlistId = :id AND uri = :uri")
    suspend fun remove(id: Long, uri: String)

    @Query("UPDATE playlist_track SET position = :position WHERE playlistId = :id AND uri = :uri")
    suspend fun setPosition(id: Long, uri: String, position: Int)

    /** Ajoute en fin de playlist ; un doublon est ignoré sans erreur. */
    @Transaction
    suspend fun append(id: Long, uri: String) {
        val existing = members(id)
        if (existing.any { it.uri == uri }) return
        val next = (existing.maxOfOrNull { it.position } ?: -1) + 1
        add(PlaylistTrack(id, uri, next))
    }

    /** Réordonne puis réécrit toutes les positions dans une seule transaction. */
    @Transaction
    suspend fun reorder(id: Long, from: Int, to: Int) {
        val list = members(id).toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        list.forEachIndexed { index, entry -> setPosition(id, entry.uri, index) }
        // Nettoie d'éventuels trous d'indexation laissés par une ancienne version.
        members(id).filterIndexed { index, entry -> entry.position != index }
            .forEach { setPosition(id, it.uri, list.indexOfFirst { m -> m.uri == it.uri }) }
    }

    @Query("SELECT COUNT(*) FROM playlist_track WHERE playlistId = :id")
    suspend fun count(id: Long): Int

    // --- Historique --------------------------------------------------------
    @Query("SELECT * FROM recent ORDER BY playedAt DESC LIMIT 100")
    fun history(): Flow<List<Recent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun played(value: Recent)

    @Query("DELETE FROM recent")
    suspend fun clearHistory()
}

@Database(
    entities = [Favorite::class, Playlist::class, PlaylistTrack::class, Recent::class],
    version = 1,
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao
}
