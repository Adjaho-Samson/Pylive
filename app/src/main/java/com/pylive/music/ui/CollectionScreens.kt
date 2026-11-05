package com.pylive.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.pylive.music.data.Playlist
import com.pylive.music.domain.Track
import com.pylive.music.domain.formatDuration
import com.pylive.music.ui.theme.PylivePalette

/** Favoris : liste triable et lecture en un geste. */
@Composable
fun FavoritesScreen(
    state: UiSnapshot,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onFavorite: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val list = state.favoriteTracks
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (list.size == 1) "1 titre" else "${list.size} titres",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                Pill(
                    text = formatDuration(list.sumOf { it.durationMs }),
                    style = PillStyle.Yellow
                )
            }
            if (list.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PyliveButton("Tout écouter", { onPlayAll(list) }, icon = R.drawable.ic_play)
                    PyliveOutlinedButton("Aléatoire", { onShuffleAll(list) }, icon = R.drawable.ic_shuffle)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_heart,
                    title = "Aucun favori",
                    body = "Appuyez sur le cœur d’un morceau pour le retrouver ici. " +
                        "Les favoris restent enregistrés sur cet appareil.",
                    actionLabel = "Parcourir la bibliothèque",
                    onAction = onOpenLibrary
                )
            }
        }
        itemsIndexed(list, key = { _, track -> "fav:${track.uri}" }) { index, track ->
            TrackRow(
                track = track,
                isFavorite = true,
                compact = state.compact,
                isPlaying = track.uri == state.player.uri,
                onClick = { onPlay(list, index) },
                onToggleFavorite = { onFavorite(track) },
                onEnqueue = { onEnqueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onDetails(track) }
            )
            SoftDivider(Modifier.padding(start = if (state.compact) 4.dp else 72.dp))
        }
    }
}

/** Historique des 100 dernières références distinctes. */
@Composable
fun HistoryScreen(
    state: UiSnapshot,
    onPlay: (List<Track>, Int) -> Unit,
    onFavorite: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onClearHistory: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val list = state.recentTracks
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (list.isEmpty()) "Aucune écoute" else "${list.size} morceaux écoutés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                if (state.history.isNotEmpty()) {
                    PyliveOutlinedButton("Effacer", onClearHistory, icon = R.drawable.ic_delete)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_history,
                    title = "Aucune écoute",
                    body = "Les morceaux écoutés apparaîtront ici, du plus récent au plus ancien. " +
                        "Cet historique ne quitte jamais le téléphone.",
                    actionLabel = "Ouvrir la bibliothèque",
                    onAction = onOpenLibrary
                )
            }
        }
        itemsIndexed(list, key = { _, track -> "hist:${track.uri}" }) { index, track ->
            TrackRow(
                track = track,
                isFavorite = track.uri in state.favoriteUris,
                compact = state.compact,
                isPlaying = track.uri == state.player.uri,
                onClick = { onPlay(list, index) },
                onToggleFavorite = { onFavorite(track) },
                onEnqueue = { onEnqueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onDetails(track) }
            )
            SoftDivider(Modifier.padding(start = if (state.compact) 4.dp else 72.dp))
        }
    }
}

/** Liste des playlists : création, ouverture, renommage et suppression. */
@Composable
fun PlaylistsScreen(
    state: UiSnapshot,
    onCreate: () -> Unit,
    onOpen: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onPlay: (Playlist) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.playlists.size == 1) "1 playlist" else "${state.playlists.size} playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                PyliveButton("Nouvelle", onCreate, icon = R.drawable.ic_add)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (state.playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_playlist,
                    title = "Votre première sélection",
                    body = "Créez une playlist, puis utilisez le menu d’un morceau pour l’y ajouter. " +
                        "Aucun fichier audio n’est copié : seules des références sont enregistrées.",
                    actionLabel = "Créer une playlist",
                    onAction = onCreate
                )
            }
        }
        itemsIndexed(state.playlists, key = { _, playlist -> "pl:${playlist.id}" }) { _, playlist ->
            val count = state.entries.count { it.playlistId == playlist.id }
            Surface(
                onClick = { onOpen(playlist) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = PylivePalette.White
            ) {
                Row(
                    Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PylivePalette.VioletSoft,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PyliveIcon(R.drawable.ic_playlist, null, tint = PylivePalette.Violet, size = 22.dp)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = PylivePalette.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (count == 1) "1 morceau" else "$count morceaux",
                            style = MaterialTheme.typography.bodySmall,
                            color = PylivePalette.InkSoft
                        )
                    }
                    PyliveIconButton(
                        icon = R.drawable.ic_play,
                        contentDescription = "Lire « ${playlist.name} »",
                        onClick = { onPlay(playlist) },
                        enabled = count > 0,
                        tint = PylivePalette.Violet
                    )
                    PyliveIconButton(
                        icon = R.drawable.ic_edit,
                        contentDescription = "Renommer « ${playlist.name} »",
                        onClick = { onRename(playlist) }
                    )
                    PyliveIconButton(
                        icon = R.drawable.ic_delete,
                        contentDescription = "Supprimer « ${playlist.name} »",
                        onClick = { onDelete(playlist) }
                    )
                }
            }
            SoftDivider()
        }
    }
}

/** Détail d'une playlist : lecture, ordre, retrait, et références indisponibles visibles. */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    state: UiSnapshot,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val references = state.entries
        .filter { it.playlistId == playlist.id }
        .sortedBy { it.position }
        .map { it.uri }
    val tracks = com.pylive.music.domain.LibraryRules.resolve(references, state.allTracks)

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (references.size == 1) "1 référence" else "${references.size} références",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                if (tracks.size != references.size) {
                    Pill(
                        text = "${tracks.size} accessibles",
                        style = PillStyle.Yellow
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PyliveButton(
                    "Lire la playlist",
                    { onPlayAll(tracks) },
                    icon = R.drawable.ic_play,
                    enabled = tracks.isNotEmpty()
                )
                PyliveOutlinedButton(
                    "Aléatoire",
                    { onShuffleAll(tracks) },
                    icon = R.drawable.ic_shuffle,
                    enabled = tracks.isNotEmpty()
                )
            }
            Spacer(Modifier.height(8.dp))
            PyliveTextButton("Ajouter depuis la bibliothèque", onOpenLibrary, icon = R.drawable.ic_library)
            Spacer(Modifier.height(6.dp))
        }
        if (references.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_playlist,
                    title = "Playlist vide",
                    body = "Ajoutez des titres depuis le menu d’un morceau, ou depuis la bibliothèque.",
                    actionLabel = "Ouvrir la bibliothèque",
                    onAction = onOpenLibrary
                )
            }
        }
        itemsIndexed(references, key = { _, uri -> "ref:$uri" }) { index, uri ->
            val track = state.allTracks.firstOrNull { it.uri == uri }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track != null) {
                    TrackCover(track.artworkUri, track.title, Modifier.size(44.dp), corner = 10.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onPlay(tracks, tracks.indexOf(track)) }
                    ) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = PylivePalette.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${track.artist} • ${track.durationLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PylivePalette.InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PylivePalette.DangerSoft,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PyliveIcon(R.drawable.ic_warning, null, tint = PylivePalette.Danger, size = 20.dp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Fichier indisponible",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PylivePalette.Danger
                        )
                        Text(
                            "Déplacé ou supprimé : retirez cette référence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PylivePalette.InkSoft,
                            maxLines = 2
                        )
                    }
                }
                PyliveIconButton(
                    icon = R.drawable.ic_chevron_up,
                    contentDescription = "Monter",
                    onClick = { onMove(index, index - 1) },
                    enabled = index > 0,
                    iconSize = 20.dp
                )
                PyliveIconButton(
                    icon = R.drawable.ic_chevron_down,
                    contentDescription = "Descendre",
                    onClick = { onMove(index, index + 1) },
                    enabled = index < references.lastIndex,
                    iconSize = 20.dp
                )
                PyliveIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = "Retirer de la playlist",
                    onClick = { onRemove(uri) },
                    iconSize = 20.dp
                )
            }
            SoftDivider()
        }
    }
}
