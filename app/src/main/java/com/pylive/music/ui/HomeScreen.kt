package com.pylive.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pylive.music.R
import com.pylive.music.domain.Track
import com.pylive.music.ui.theme.PylivePalette

/**
 * Accueil : identité de la marque, chiffre clé, raccourcis utiles puis les
 * sections « à l'écoute », dernières écoutes, récemment ajoutés, favoris et
 * playlists. Une seule liste descendante, donc lisible sur tous les écrans.
 */
@Composable
fun HomeScreen(
    state: UiSnapshot,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onPlayShuffle: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylists: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp
        )
    ) {
        item { HeroCard(state, onOpenLibrary, onOpenPlayer, onPlayShuffle) }

        if (state.isEmptyLibrary && !state.loading) {
            item {
                EmptyState(
                    icon = R.drawable.ic_library,
                    title = "Aucun morceau détecté",
                    body = "Ajoutez des fichiers audio sur le téléphone, autorisez l’accès puis " +
                        "actualisez. Les fichiers privés des applications de streaming ne sont " +
                        "pas accessibles.",
                    actionLabel = if (state.permissionGranted) "Actualiser" else "Autoriser l’accès"
                )
            }
            item { PrivacyCard() }
            return@LazyColumn
        }

        if (state.player.hasTrack) {
            item { SectionHeader("À l’écoute", actionLabel = "Ouvrir", actionIcon = R.drawable.ic_play, onAction = onOpenPlayer) }
            item {
                NowPlayingCard(
                    state = state,
                    onOpen = onOpenPlayer,
                    onOpenQueue = onOpenPlayer
                )
            }
        }

        if (state.recentTracks.isNotEmpty()) {
            item { SectionHeader("Dernières écoutes", actionLabel = "Tout voir", onAction = onOpenHistory) }
            itemsIndexed(state.recentTracks.take(5), key = { _, t -> "recent:${t.uri}" }) { index, track ->
                TrackRow(
                    track = track,
                    isFavorite = track.uri in state.favoriteUris,
                    compact = state.compact,
                    isPlaying = track.uri == state.player.uri,
                    onClick = { onPlay(state.recentTracks, index) },
                    onToggleFavorite = { onFavorite(track) },
                    onEnqueue = { onEnqueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onShowDetails = { onDetails(track) }
                )
            }
        }

        val added = state.allTracks.sortedByDescending { it.addedAtSeconds }.take(5)
        if (added.isNotEmpty()) {
            item { SectionHeader("Récemment ajoutés", actionLabel = "Bibliothèque", onAction = onOpenLibrary) }
            itemsIndexed(added, key = { _, t -> "added:${t.uri}" }) { index, track ->
                TrackRow(
                    track = track,
                    isFavorite = track.uri in state.favoriteUris,
                    compact = state.compact,
                    isPlaying = track.uri == state.player.uri,
                    onClick = { onPlay(added, index) },
                    onToggleFavorite = { onFavorite(track) },
                    onEnqueue = { onEnqueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onShowDetails = { onDetails(track) }
                )
            }
        }

        if (state.favoriteTracks.isNotEmpty()) {
            item { SectionHeader("Vos favoris", actionLabel = "Tout voir", onAction = onOpenFavorites) }
            itemsIndexed(state.favoriteTracks.take(5), key = { _, t -> "fav:${t.uri}" }) { index, track ->
                TrackRow(
                    track = track,
                    isFavorite = true,
                    compact = state.compact,
                    isPlaying = track.uri == state.player.uri,
                    onClick = { onPlay(state.favoriteTracks, index) },
                    onToggleFavorite = { onFavorite(track) },
                    onEnqueue = { onEnqueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onShowDetails = { onDetails(track) }
                )
            }
        }

        item {
            SectionHeader("Vos playlists", actionLabel = "Gérer", onAction = onOpenPlaylists)
        }
        if (state.playlists.isEmpty()) {
            item {
                PyliveCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Votre première sélection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Créez une playlist, puis utilisez le menu d’un morceau pour l’y ajouter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        PyliveButton("Nouvelle playlist", onCreatePlaylist, icon = R.drawable.ic_add)
                    }
                }
            }
        } else {
            itemsIndexed(state.playlists.take(4), key = { _, p -> "pl:${p.id}" }) { _, playlist ->
                val count = state.entries.count { it.playlistId == playlist.id }
                PlaylistRow(playlist.name, count, onOpen = { onOpenPlaylist(playlist.id) })
            }
            item {
                Spacer(Modifier.height(10.dp))
                PyliveOutlinedButton(
                    "Toutes les playlists",
                    onOpenPlaylists,
                    Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_playlist
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            PrivacyCard()
        }
    }
}

@Composable
private fun HeroCard(
    state: UiSnapshot,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayShuffle: (List<Track>) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = PylivePalette.Violet,
        contentColor = PylivePalette.White
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 30.dp, onLight = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "PYLIVE MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.White.copy(alpha = 0.94f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "La musique\nde votre téléphone.",
                style = MaterialTheme.typography.headlineLarge,
                color = PylivePalette.White
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = when {
                    state.allTracks.isEmpty() -> "Aucun titre détecté pour l’instant"
                    state.allTracks.size == 1 -> "1 titre • 100 % local"
                    else -> "${state.allTracks.size} titres • 100 % local"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = PylivePalette.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onOpenLibrary,
                    shape = RoundedCornerShape(14.dp),
                    color = PylivePalette.White,
                    contentColor = PylivePalette.VioletDeep
                ) {
                    Row(
                        Modifier
                            .height(46.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PyliveIcon(R.drawable.ic_library, null, tint = PylivePalette.VioletDeep, size = 19.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Bibliothèque",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    onClick = { if (state.allTracks.isEmpty()) onOpenLibrary() else onPlayShuffle(state.allTracks) },
                    shape = RoundedCornerShape(14.dp),
                    color = PylivePalette.Yellow,
                    contentColor = PylivePalette.Ink
                ) {
                    Row(
                        Modifier
                            .height(46.dp)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PyliveIcon(R.drawable.ic_shuffle, null, tint = PylivePalette.Ink, size = 19.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Aléatoire",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (state.player.hasTrack) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    onClick = onOpenPlayer,
                    shape = RoundedCornerShape(14.dp),
                    color = PylivePalette.White.copy(alpha = 0.16f),
                    contentColor = PylivePalette.White
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NowPlayingBars(color = PylivePalette.Yellow)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.player.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = PylivePalette.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                state.player.artist.ifBlank { "Pylive Music" },
                                style = MaterialTheme.typography.bodySmall,
                                color = PylivePalette.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Ouvrir",
                            style = MaterialTheme.typography.labelLarge,
                            color = PylivePalette.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    state: UiSnapshot,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit
) {
    PyliveCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TrackCover(state.player.artworkUri, state.player.title, Modifier.size(62.dp), corner = 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.player.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.player.artist.ifBlank { "Pylive Music" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp)
                        .background(PylivePalette.OutlineSoft, RoundedCornerShape(50))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(state.player.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(PylivePalette.Yellow, RoundedCornerShape(50))
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            PyliveIconButton(
                icon = R.drawable.ic_queue,
                contentDescription = "File d’attente",
                onClick = onOpenQueue,
                tint = PylivePalette.Violet
            )
        }
    }
}

@Composable
fun PlaylistRow(name: String, count: Int, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PylivePalette.VioletSoft,
            modifier = Modifier.size(44.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PyliveIcon(R.drawable.ic_playlist, null, tint = PylivePalette.Violet, size = 22.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (count == 1) "1 morceau" else "$count morceaux",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PyliveIcon(R.drawable.ic_chevron_right, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
    }
}

/** Rappel de confidentialité : rassurant, factuel, sans emoji. */
@Composable
fun PrivacyCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PylivePalette.SurfaceTint
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            PyliveIcon(R.drawable.ic_shield, null, tint = PylivePalette.Violet, size = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "100 % sur cet appareil",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Aucun compte, aucune publicité, aucune télémétrie, aucune permission " +
                        "Internet. Vos fichiers audio ne quittent jamais le téléphone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PylivePalette.InkSoft,
                    modifier = Modifier.widthIn(max = 520.dp)
                )
            }
        }
    }
}
