package com.pylive.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pylive.music.R
import com.pylive.music.domain.RepeatMode
import com.pylive.music.domain.formatDuration
import com.pylive.music.ui.theme.PylivePalette

/**
 * Lecteur complet : pochette, informations lisibles, barre de progression
 * interactive, commandes principales et secondaires. Aucune commande n'est
 * décorative : chaque bouton agit réellement sur le moteur audio.
 */
@Composable
fun PlayerScreen(
    state: UiSnapshot,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val player = state.player
    val isFavorite = player.uri != null && player.uri in state.favoriteUris

    Column(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!player.hasTrack) {
            Spacer(Modifier.height(24.dp))
            EmptyState(
                icon = R.drawable.ic_note,
                title = "Prêt pour votre musique",
                body = "Choisissez un morceau dans la bibliothèque pour commencer l’écoute. " +
                    "La lecture continue même si vous quittez cet écran.",
                actionLabel = "Ouvrir la bibliothèque",
                onAction = onOpenLibrary
            )
            return@Column
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "LECTURE LOCALE",
            style = MaterialTheme.typography.labelSmall,
            color = PylivePalette.Violet,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            color = PylivePalette.VioletSoft,
            shadowElevation = 2.dp
        ) {
            TrackCover(
                artworkUri = player.artworkUri,
                title = player.title,
                modifier = Modifier.fillMaxSize(),
                corner = 28.dp
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = player.title.ifBlank { "Sans titre" },
            style = MaterialTheme.typography.headlineSmall,
            color = PylivePalette.Ink,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = player.artist.ifBlank { "Artiste inconnu" },
            style = MaterialTheme.typography.bodyLarge,
            color = PylivePalette.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (player.album.isNotBlank() && player.album != "Album inconnu") {
            Spacer(Modifier.height(2.dp))
            Text(
                text = player.album,
                style = MaterialTheme.typography.bodySmall,
                color = PylivePalette.InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(18.dp))
        ProgressSection(
            positionMs = player.positionMs,
            durationMs = player.durationMs,
            enabled = player.hasTrack,
            onSeek = onSeek
        )

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PyliveIconButton(
                icon = R.drawable.ic_shuffle,
                contentDescription = if (player.shuffle) {
                    "Désactiver la lecture aléatoire"
                } else {
                    "Activer la lecture aléatoire"
                },
                onClick = onShuffle,
                tint = if (player.shuffle) PylivePalette.Violet else PylivePalette.InkSoft,
                iconSize = 24.dp
            )
            PyliveIconButton(
                icon = R.drawable.ic_replay_10,
                contentDescription = "Reculer de 10 secondes",
                onClick = { onSeekBy(-10_000L) },
                tint = PylivePalette.Ink,
                iconSize = 26.dp
            )
            PyliveIconButton(
                icon = R.drawable.ic_previous,
                contentDescription = "Morceau précédent",
                onClick = onPrevious,
                tint = PylivePalette.Ink,
                iconSize = 30.dp
            )
            Surface(
                onClick = onToggle,
                enabled = player.canPlayPause,
                modifier = Modifier
                    .size(76.dp)
                    .testTag("play_pause"),
                shape = CircleShape,
                color = if (player.canPlayPause) PylivePalette.Violet else PylivePalette.OutlineSoft,
                contentColor = if (player.canPlayPause) PylivePalette.White else PylivePalette.InkSoft
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PyliveIcon(
                        icon = if (player.playing) R.drawable.ic_pause else R.drawable.ic_play,
                        contentDescription = if (player.playing) "Mettre en pause" else "Lancer la lecture",
                        tint = if (player.canPlayPause) PylivePalette.White else PylivePalette.InkSoft,
                        size = 36.dp
                    )
                }
            }
            PyliveIconButton(
                icon = R.drawable.ic_next,
                contentDescription = "Morceau suivant",
                onClick = onNext,
                tint = PylivePalette.Ink,
                iconSize = 30.dp
            )
            PyliveIconButton(
                icon = R.drawable.ic_forward_10,
                contentDescription = "Avancer de 10 secondes",
                onClick = { onSeekBy(10_000L) },
                tint = PylivePalette.Ink,
                iconSize = 26.dp
            )
            PyliveIconButton(
                icon = when (player.repeat) {
                    RepeatMode.ONE -> R.drawable.ic_repeat_one
                    else -> R.drawable.ic_repeat
                },
                contentDescription = when (player.repeat) {
                    RepeatMode.OFF -> "Répétition désactivée, appuyer pour répéter la file"
                    RepeatMode.ALL -> "Répétition de la file active, appuyer pour répéter le morceau"
                    RepeatMode.ONE -> "Répétition du morceau active, appuyer pour désactiver"
                },
                onClick = onRepeat,
                tint = if (player.repeat == RepeatMode.OFF) PylivePalette.InkSoft else PylivePalette.Violet,
                iconSize = 24.dp
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PyliveOutlinedButton(
                text = if (isFavorite) "Dans les favoris" else "Ajouter aux favoris",
                onClick = onFavorite,
                icon = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )
            PyliveOutlinedButton(
                text = "File • ${player.queue.size}",
                onClick = onOpenQueue,
                icon = R.drawable.ic_queue
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Barre de progression : glissement local, puis envoi unique de la position. */
@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val max = durationMs.coerceAtLeast(1L).toFloat()
    val value = (dragging ?: positionMs.toFloat()).coerceIn(0f, max)

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..max,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("progress"),
            colors = SliderDefaults.colors(
                thumbColor = PylivePalette.Violet,
                activeTrackColor = PylivePalette.Violet,
                inactiveTrackColor = PylivePalette.OutlineSoft,
                disabledThumbColor = PylivePalette.Outline,
                disabledActiveTrackColor = PylivePalette.OutlineSoft,
                disabledInactiveTrackColor = PylivePalette.OutlineSoft
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DurationText(
                formatDuration((dragging ?: positionMs.toFloat()).toLong()),
                color = PylivePalette.Ink
            )
            DurationText(formatDuration(durationMs), color = PylivePalette.InkSoft)
        }
    }
}

/**
 * File d'attente : ordre réel du moteur de lecture, déplacement par flèches
 * accessibles, retrait immédiat et saut direct au morceau choisi.
 */
@Composable
fun QueueScreen(
    state: UiSnapshot,
    onJump: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val queue = state.player.queue
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp
        )
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (queue.size == 1) "1 morceau" else "${queue.size} morceaux",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.Ink,
                    modifier = Modifier.weight(1f)
                )
                Pill(
                    text = "Total ${formatDuration(queue.sumOf { it.durationMs })}",
                    style = PillStyle.Outline
                )
            }
        }
        if (queue.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_queue,
                    title = "La file est vide",
                    body = "Lancez un morceau ou ajoutez des titres depuis leur menu " +
                        "« Ajouter à la file d’attente ».",
                    actionLabel = "Ouvrir la bibliothèque",
                    onAction = onOpenLibrary
                )
            }
        }
        androidx.compose.foundation.lazy.itemsIndexed(queue, key = { index, item -> "$index:${item.uri}" }) { index, entry ->
            val current = index == state.player.index
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (current) {
                    NowPlayingBars(Modifier.padding(end = 8.dp))
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PylivePalette.InkSoft,
                        modifier = Modifier.width(24.dp)
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .androidxClickable { onJump(index) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        color = if (current) PylivePalette.VioletDeep else PylivePalette.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (entry.artist.isBlank()) {
                            if (current) "En cours de lecture" else formatDuration(entry.durationMs)
                        } else {
                            entry.artist
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PylivePalette.InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    enabled = index < queue.lastIndex,
                    iconSize = 20.dp
                )
                PyliveIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = "Retirer de la file",
                    onClick = { onRemove(index) },
                    iconSize = 20.dp
                )
            }
            SoftDivider()
        }
    }
}

/** Petit raccourci lisible pour `Modifier.clickable` dans ce fichier. */
private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
