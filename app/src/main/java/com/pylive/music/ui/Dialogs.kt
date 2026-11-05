package com.pylive.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pylive.music.R
import com.pylive.music.data.Playlist
import com.pylive.music.domain.Track
import com.pylive.music.domain.formatDate
import com.pylive.music.domain.formatSize
import com.pylive.music.ui.theme.PylivePalette

/** Boîte de dialogue de saisie d'un nom de playlist. */
@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String = "",
    confirmLabel: String = "Enregistrer",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = PylivePalette.Ink
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Nom de la playlist") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PylivePalette.Violet,
                    unfocusedBorderColor = PylivePalette.Outline,
                    cursorColor = PylivePalette.Violet,
                    focusedTextColor = PylivePalette.Ink,
                    unfocusedTextColor = PylivePalette.Ink
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()); onDismiss() },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel, color = if (name.isNotBlank()) PylivePalette.Violet else PylivePalette.InkSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = PylivePalette.InkSoft)
            }
        },
        containerColor = PylivePalette.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/** Confirmation destructive explicite (suppression de playlist, historique). */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, color = PylivePalette.Ink)
        },
        text = {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = PylivePalette.InkSoft
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = PylivePalette.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = PylivePalette.InkSoft) }
        },
        containerColor = PylivePalette.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/** Choix de la playlist de destination d'un morceau. */
@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onSelect: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Ajouter à une playlist", style = MaterialTheme.typography.titleLarge, color = PylivePalette.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = PylivePalette.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (playlists.isEmpty()) {
                    Text(
                        "Créez d’abord une playlist : elle rangera les morceaux sans copier les fichiers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PylivePalette.InkSoft
                    )
                    Spacer(Modifier.height(14.dp))
                    PyliveButton("Créer une playlist", onCreateNew, icon = R.drawable.ic_add)
                } else {
                    playlists.forEach { playlist ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(playlist) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PylivePalette.VioletSoft,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                        PyliveIcon(
                                            R.drawable.ic_playlist, null,
                                            tint = PylivePalette.Violet, size = 18.dp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PylivePalette.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        SoftDivider()
                    }
                    Spacer(Modifier.height(12.dp))
                    PyliveTextButton("Créer une nouvelle playlist", onCreateNew, icon = R.drawable.ic_add)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = PylivePalette.InkSoft) }
        },
        containerColor = PylivePalette.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/** Détails techniques d'un morceau, en lecture seule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsSheet(
    track: Track,
    onDismiss: () -> Unit,
    onEnqueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PylivePalette.White
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackCover(track.artworkUri, track.title, Modifier.size(64.dp), corner = 16.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = PylivePalette.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PylivePalette.InkSoft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            DetailRow("Album", track.album)
            DetailRow("Durée", track.durationLabel)
            DetailRow("Fichier", track.fileName.ifBlank { "—" })
            DetailRow("Taille", formatSize(track.sizeBytes))
            DetailRow("Ajouté le", formatDate(track.addedAtSeconds))
            DetailRow(
                "Source",
                if (track.source == Track.Source.MEDIA_STORE) "Bibliothèque Android" else "Dossier choisi"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Référence",
                style = MaterialTheme.typography.labelMedium,
                color = PylivePalette.InkSoft
            )
            Text(
                track.uri,
                style = MaterialTheme.typography.bodySmall,
                color = PylivePalette.InkSoft
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PyliveButton("File d’attente", onEnqueue, icon = R.drawable.ic_queue)
                PyliveOutlinedButton(
                    if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                    onToggleFavorite,
                    icon = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = PylivePalette.InkSoft,
            modifier = Modifier.width(104.dp)
        )
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = PylivePalette.Ink,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Message d'information : toujours un titre explicite et un bouton clair. */
@Composable
fun NoticeDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = PylivePalette.Ink) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium, color = PylivePalette.InkSoft) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Compris", color = PylivePalette.Violet, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PylivePalette.White,
        shape = RoundedCornerShape(24.dp)
    )
}
