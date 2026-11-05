package com.pylive.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pylive.music.R
import com.pylive.music.domain.MusicSort
import com.pylive.music.domain.Track
import com.pylive.music.ui.theme.PylivePalette

/**
 * Bibliothèque : recherche instantanée, tri explicite, mode compact et lecture
 * de toute la sélection (dans l'ordre affiché ou en aléatoire).
 */
@Composable
fun LibraryScreen(
    state: UiSnapshot,
    onQueryChange: (String) -> Unit,
    onSortChange: (MusicSort) -> Unit,
    onToggleCompact: () -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onFavorite: (Track) -> Unit,
    onEnqueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onRefresh: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 28.dp)
    ) {
        item {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                onSearch = { keyboard?.hide() }
            )
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortChip(state.sort, onSortChange)
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onToggleCompact,
                    shape = RoundedCornerShape(50),
                    color = if (state.compact) PylivePalette.VioletSoft else MaterialTheme.colorScheme.surface,
                    contentColor = if (state.compact) PylivePalette.VioletDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PylivePalette.Outline)
                ) {
                    Row(
                        Modifier
                            .height(40.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PyliveIcon(
                            R.drawable.ic_tune,
                            null,
                            tint = if (state.compact) PylivePalette.VioletDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.compact) "Pochettes" else "Compact",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.padding(top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (state.visibleTracks.size) {
                        0 -> "Aucun morceau"
                        1 -> "1 morceau"
                        else -> "${state.visibleTracks.size} morceaux"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                if (state.visibleTracks.isNotEmpty()) {
                    PyliveTextButton(
                        "Aléatoire",
                        { onShuffleAll(state.visibleTracks) },
                        icon = R.drawable.ic_shuffle
                    )
                }
            }
        }
        if (state.visibleTracks.isNotEmpty()) {
            item {
                PyliveButton(
                    text = if (state.hasQuery) {
                        "Lire les ${state.visibleTracks.size} résultats"
                    } else {
                        "Tout écouter"
                    },
                    onClick = { onPlayAll(state.visibleTracks) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = R.drawable.ic_play
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (state.visibleTracks.isEmpty()) {
            item {
                if (state.allTracks.isEmpty()) {
                    EmptyState(
                        icon = R.drawable.ic_library,
                        title = "Aucun morceau détecté",
                        body = "Ajoutez des fichiers audio sur le téléphone, autorisez l’accès puis " +
                            "actualisez la bibliothèque.",
                        actionLabel = "Actualiser",
                        onAction = onRefresh
                    )
                } else {
                    EmptyState(
                        icon = R.drawable.ic_search,
                        title = "Aucun résultat",
                        body = "Aucun morceau ne correspond à « ${state.query} ». Essayez un autre " +
                            "titre, artiste, album ou nom de fichier.",
                        actionLabel = "Effacer la recherche",
                        onAction = { onQueryChange("") }
                    )
                }
            }
        }

        itemsIndexed(state.visibleTracks, key = { _, track -> track.uri }) { index, track ->
            TrackRow(
                track = track,
                isFavorite = track.uri in state.favoriteUris,
                compact = state.compact,
                isPlaying = track.uri == state.player.uri,
                onClick = { onPlay(state.visibleTracks, index) },
                onToggleFavorite = { onFavorite(track) },
                onEnqueue = { onEnqueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onDetails(track) }
            )
            SoftDivider(Modifier.padding(start = if (state.compact) 4.dp else 72.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Titre, artiste, album, fichier…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            PyliveIcon(R.drawable.ic_search, null, tint = PylivePalette.Violet, size = 22.dp)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                PyliveIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = "Effacer la recherche",
                    onClick = { onQueryChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PylivePalette.Violet,
            unfocusedBorderColor = PylivePalette.Outline,
            focusedContainerColor = PylivePalette.White,
            unfocusedContainerColor = PylivePalette.White,
            cursorColor = PylivePalette.Violet,
            focusedTextColor = PylivePalette.Ink,
            unfocusedTextColor = PylivePalette.Ink
        )
    )
}

@Composable
fun SortChip(current: MusicSort, onSelect: (MusicSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            contentColor = PylivePalette.Ink,
            border = androidx.compose.foundation.BorderStroke(1.dp, PylivePalette.Outline)
        ) {
            Row(
                Modifier
                    .height(40.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PyliveIcon(R.drawable.ic_sort, null, tint = PylivePalette.Violet, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tri : ${sortLabel(current)}",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MusicSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            sortLabel(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (option == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == current) PylivePalette.VioletDeep else PylivePalette.Ink
                        )
                    },
                    trailingIcon = if (option == current) {
                        { PyliveIcon(R.drawable.ic_check, null, tint = PylivePalette.Violet, size = 20.dp) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

fun sortLabel(sort: MusicSort): String = when (sort) {
    MusicSort.TITLE -> "Titre"
    MusicSort.ARTIST -> "Artiste"
    MusicSort.ALBUM -> "Album"
    MusicSort.DURATION -> "Durée"
    MusicSort.ADDED -> "Ajout récent"
}
