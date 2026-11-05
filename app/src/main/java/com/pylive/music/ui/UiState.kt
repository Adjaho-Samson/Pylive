package com.pylive.music.ui

import com.pylive.music.data.Playlist
import com.pylive.music.data.PlaylistTrack
import com.pylive.music.data.Recent
import com.pylive.music.domain.MusicSort
import com.pylive.music.domain.Track
import com.pylive.music.playback.PlaybackError
import com.pylive.music.playback.PlaybackState

/**
 * Instantané de tous les états nécessaires aux écrans. Un seul objet passé aux
 * composables : moins de paramètres, moins de recompositions inutiles.
 */
data class UiSnapshot(
    val allTracks: List<Track> = emptyList(),
    val visibleTracks: List<Track> = emptyList(),
    val favoriteUris: Set<String> = emptySet(),
    val favoriteTracks: List<Track> = emptyList(),
    val recentTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val entries: List<PlaylistTrack> = emptyList(),
    val history: List<Recent> = emptyList(),
    val player: PlaybackState = PlaybackState(),
    val loading: Boolean = false,
    val query: String = "",
    val compact: Boolean = false,
    val sort: MusicSort = MusicSort.TITLE,
    val permissionGranted: Boolean = false,
    val permissionDenied: Boolean = false,
    val folderName: String? = null,
    val libraryErrorCode: String? = null,
    val playbackError: PlaybackError? = null,
    val audioPermissionRequired: Boolean = true
) {
    val isEmptyLibrary: Boolean get() = allTracks.isEmpty()
    val hasQuery: Boolean get() = query.isNotBlank()
}

/** Écrans secondaires affichés par-dessus les onglets. */
sealed interface Route {
    data object Home : Route
    data object Library : Route
    data object Favorites : Route
    data object Playlists : Route
    data object Player : Route
    data object Queue : Route
    data object History : Route
    data object Settings : Route
    data class PlaylistDetail(val playlistId: Long) : Route
}
