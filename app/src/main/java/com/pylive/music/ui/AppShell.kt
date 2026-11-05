package com.pylive.music.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.pylive.music.R
import com.pylive.music.domain.PermissionPolicy
import com.pylive.music.domain.formatDuration
import com.pylive.music.playback.PlaybackState
import com.pylive.music.ui.theme.PylivePalette

/** Onglets de navigation principale. */
enum class PyliveTab(val label: String, val icon: Int) {
    HOME("Accueil", R.drawable.ic_home),
    LIBRARY("Bibliothèque", R.drawable.ic_library),
    FAVORITES("Favoris", R.drawable.ic_heart),
    PLAYLISTS("Playlists", R.drawable.ic_playlist)
}

/**
 * En-tête commun : marque, titre de l'écran et actions utiles.
 * La barre reste blanche pour garantir un contraste maximal du texte violet.
 */
@Composable
fun PyliveTopBar(
    title: String?,
    subtitle: String?,
    onBack: (() -> Unit)?,
    onSettings: () -> Unit,
    onRefresh: (() -> Unit)?,
    refreshing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                PyliveIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = "Retour",
                    onClick = onBack,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
            } else {
                BrandMark(size = 36.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                if (title == null) {
                    Text(
                        text = "Pylive Music",
                        style = MaterialTheme.typography.titleLarge,
                        color = PylivePalette.Ink,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = PylivePalette.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onRefresh != null) {
                if (refreshing) {
                    PyliveIconButton(
                        icon = R.drawable.ic_refresh,
                        contentDescription = "Actualisation en cours",
                        onClick = {},
                        enabled = false,
                        tint = PylivePalette.Violet
                    )
                } else {
                    PyliveIconButton(
                        icon = R.drawable.ic_refresh,
                        contentDescription = "Actualiser la bibliothèque",
                        onClick = onRefresh,
                        tint = PylivePalette.Violet
                    )
                }
            }
            PyliveIconButton(
                icon = R.drawable.ic_info,
                contentDescription = "Réglages",
                onClick = onSettings,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        SoftDivider()
        if (refreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = PylivePalette.Violet,
                trackColor = PylivePalette.VioletSoft
            )
        }
    }
}

/**
 * Bandeau d'autorisation.
 * Il explique l'usage des fichiers audio, propose l'autorisation, un dossier de
 * secours et, en cas de refus, l'accès direct aux réglages Android.
 */
@Composable
fun PermissionBanner(
    granted: Boolean,
    denied: Boolean,
    folderName: String?,
    onRequest: () -> Unit,
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (granted && folderName == null) return
    Column(modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        if (!granted) {
            InfoBanner(
                title = if (denied) "Accès audio refusé" else "Votre musique, automatiquement",
                body = if (denied) {
                    "Sans cette autorisation, la bibliothèque reste vide. Vous pouvez l’accorder " +
                        "dans les réglages Android, ou choisir un dossier précis."
                } else {
                    "Autorisez l’accès aux fichiers audio pour afficher la musique enregistrée sur ce " +
                        "téléphone. Aucun envoi, aucun compte, aucune connexion Internet."
                },
                tone = if (denied) BannerTone.Warning else BannerTone.Info,
                actionLabel = "Autoriser l’accès",
                onAction = onRequest,
                secondaryLabel = if (denied) "Réglages Android" else "Choisir un dossier",
                onSecondary = if (denied) onOpenSettings else onPickFolder
            )
            if (denied) {
                Spacer(Modifier.height(8.dp))
                PyliveTextButton("Choisir un dossier à la place", onPickFolder)
            }
        } else if (folderName != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PyliveIcon(R.drawable.ic_folder, null, tint = PylivePalette.Violet, size = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Dossier analysé : $folderName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    PyliveTextButton("Oublier", onPickFolder)
                }
            }
        }
    }
}

/** Mini-lecteur persistant : pochette, titre, artiste, lecture/pause et suivant. */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onOpenPlayer: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = PylivePalette.Violet,
        contentColor = PylivePalette.White,
        shadowElevation = 3.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlayer)
                .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCover(state.artworkUri, state.title, Modifier.size(44.dp), corner = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "Aucun morceau" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PylivePalette.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.artist.ifBlank { "Pylive Music" },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = PylivePalette.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PyliveIconButton(
                icon = if (state.playing) R.drawable.ic_pause else R.drawable.ic_play,
                contentDescription = if (state.playing) "Mettre en pause" else "Reprendre la lecture",
                onClick = onToggle,
                tint = PylivePalette.White,
                enabled = state.hasTrack
            )
            PyliveIconButton(
                icon = R.drawable.ic_next,
                contentDescription = "Morceau suivant",
                onClick = onNext,
                tint = PylivePalette.White,
                enabled = state.hasTrack
            )
        }
    }
}

/** Barre de navigation inférieure : quatre onglets, libellés toujours visibles. */
@Composable
fun PyliveNavigationBar(
    selected: PyliveTab?,
    onSelect: (PyliveTab) -> Unit
) {
    Column {
        SoftDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .background(PylivePalette.White)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PyliveTab.entries.forEach { tab ->
                val active = tab == selected
                val tint by animateColorAsState(
                    targetValue = if (active) PylivePalette.Violet else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabTint"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) PylivePalette.VioletSoft else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        PyliveIcon(tab.icon, contentDescription = null, tint = tint, size = 22.dp)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Demande d'autorisation audio + observation du retour au premier plan. */
@Composable
fun rememberAudioPermission(
    onPermissionChanged: () -> Unit
): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val permission = remember { PermissionPolicy.audioPermission(android.os.Build.VERSION.SDK_INT) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var deniedDialogSeen by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        if (!result) deniedDialogSeen = true
        onPermissionChanged()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
                if (now != granted) {
                    granted = now
                    onPermissionChanged()
                } else if (now) {
                    onPermissionChanged()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val request = { launcher.launch(permission) }
    return granted to request
}

/** Sélecteur de dossier facultatif, avec conservation de l'autorisation de lecture. */
@Composable
fun rememberFolderPicker(onPicked: (String) -> Unit, onError: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            }.getOrDefault(false)
            if (persisted) onPicked(uri.toString()) else onError()
        }
    }
    return { launcher.launch(null) }
}

/** Ouvre la page d'autorisations de l'application dans les réglages Android. */
fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Référence partagée pour les écrans qui affichent une durée de file. */
fun queueDurationLabel(entries: List<com.pylive.music.playback.QueueEntry>): String =
    formatDuration(entries.sumOf { it.durationMs })
