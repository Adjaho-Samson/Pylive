package com.pylive.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pylive.music.PyliveApp
import com.pylive.music.data.Favorite
import com.pylive.music.data.Playlist
import com.pylive.music.data.PlaylistTrack
import com.pylive.music.data.Recent
import com.pylive.music.domain.LibraryRules
import com.pylive.music.domain.MusicSort
import com.pylive.music.domain.Track
import com.pylive.music.library.LocalLibrary
import com.pylive.music.playback.PlaybackConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Messages ponctuels affichés à l'utilisatrice (succès, information, erreur). */
sealed interface Notice {
    data class Success(val text: String) : Notice
    data class Info(val text: String) : Notice
    data class Failure(val text: String) : Notice
}

/**
 * État de l'application et actions utilisateur.
 *
 * Les données de la bibliothèque vivent dans [LocalLibrary], la lecture dans
 * [PlaybackConnection] ; ce ViewModel les combine et expose une vision unique
 * et réactive à l'interface, sans logique d'affichage.
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PyliveApp
    private val dao = app.database.dao()

    val library = LocalLibrary(app, viewModelScope)
    val playback = PlaybackConnection(app, viewModelScope)

    val favorites: StateFlow<List<Favorite>> = dao.favorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists: StateFlow<List<Playlist>> = dao.playlists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val entries: StateFlow<List<PlaylistTrack>> = dao.entries()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history: StateFlow<List<Recent>> = dao.history()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sort: StateFlow<MusicSort> = app.preferences.sort
        .stateIn(viewModelScope, SharingStarted.Eagerly, MusicSort.TITLE)
    val compact: StateFlow<Boolean> = app.preferences.compact
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val folder: StateFlow<String?> = app.preferences.folder
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Recherche partagée entre l'onglet Bibliothèque et l'écran d'accueil. */
    val query = MutableStateFlow("")
    val notice = MutableStateFlow<Notice?>(null)

    val favoriteUris: StateFlow<Set<String>> = favorites
        .map { list -> list.map(Favorite::uri).toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Résultat filtré et trié, recalculé hors du fil principal. */
    val visibleTracks: StateFlow<List<Track>> = combine(
        library.tracks, query, sort
    ) { tracks, text, order ->
        LibraryRules.select(tracks, text, order)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val recentTracks: StateFlow<List<Track>> = combine(library.tracks, history) { tracks, recent ->
        LibraryRules.resolve(recent.map(Recent::uri), tracks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = combine(library.tracks, favoriteUris, sort) { tracks, favorite, order ->
        LibraryRules.sort(tracks.filter { it.uri in favorite }, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Le dossier facultatif choisi est rescané à chaque démarrage.
        viewModelScope.launch {
            app.preferences.folder.collect { uri -> library.setFolder(uri) }
        }
    }

    // --- Autorisations et dossier -----------------------------------------
    fun requestRefresh() = library.refresh()

    fun hasAudioPermission(): Boolean = library.hasAudioPermission()

    fun chooseFolder(uri: String?) {
        viewModelScope.launch {
            app.preferences.setFolder(uri)
            if (uri == null) library.setFolder(null)
        }
    }

    // --- Favoris -----------------------------------------------------------
    fun toggleFavorite(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (favoriteUris.value.contains(uri)) dao.unfavorite(uri)
            else dao.favorite(Favorite(uri, System.currentTimeMillis()))
        }
    }

    // --- Playlists ---------------------------------------------------------
    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.create(Playlist(name = trimmed, createdAt = System.currentTimeMillis()))
            notice.value = Notice.Success(trimmed)
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { dao.rename(id, trimmed) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(id) }
    }

    fun addToPlaylist(playlistId: Long, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.append(playlistId, uri)
            val name = playlists.value.firstOrNull { it.id == playlistId }?.name.orEmpty()
            notice.value = Notice.Success(name)
        }
    }

    fun removeFromPlaylist(playlistId: Long, uri: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.remove(playlistId, uri) }
    }

    fun moveInPlaylist(playlistId: Long, from: Int, to: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.reorder(playlistId, from, to) }
    }

    fun playlistTracks(playlist: Playlist): List<Track> {
        val refs = entries.value
            .filter { it.playlistId == playlist.id }
            .sortedBy { it.position }
            .map { it.uri }
        return LibraryRules.resolve(refs, library.tracks.value)
    }

    fun playlistReferences(playlist: Playlist): List<String> = entries.value
        .filter { it.playlistId == playlist.id }
        .sortedBy { it.position }
        .map { it.uri }

    // --- Historique --------------------------------------------------------
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) { dao.clearHistory() }
    }

    // --- Réglages ----------------------------------------------------------
    fun setSort(value: MusicSort) {
        viewModelScope.launch(Dispatchers.IO) { app.preferences.setSort(value) }
    }

    fun setCompact(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { app.preferences.setCompact(value) }
    }

    // --- Lecture -----------------------------------------------------------
    fun play(tracks: List<Track>, index: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        if (!playback.isConnected) {
            notice.value = Notice.Failure("connecting")
            return
        }
        val selected = tracks.getOrNull(index) ?: return
        viewModelScope.launch {
            if (!library.isAccessible(selected.uri)) {
                notice.value = Notice.Failure("missing")
                library.refresh()
                return@launch
            }
            playback.play(tracks, index, shuffle)
        }
    }

    fun enqueue(track: Track) {
        if (!playback.isConnected) {
            notice.value = Notice.Failure("connecting")
            return
        }
        viewModelScope.launch {
            if (!library.isAccessible(track.uri)) {
                notice.value = Notice.Failure("inaccessible")
                return@launch
            }
            playback.enqueue(track)
            notice.value = Notice.Success(track.title)
        }
    }

    fun dismissNotice() {
        notice.value = null
    }

    override fun onCleared() {
        library.close()
        playback.close()
        super.onCleared()
    }
}
