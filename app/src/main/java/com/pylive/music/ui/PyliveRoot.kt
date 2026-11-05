package com.pylive.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pylive.music.BuildConfig
import com.pylive.music.data.Playlist
import com.pylive.music.domain.Track
import com.pylive.music.ui.theme.PylivePalette
import com.pylive.music.ui.theme.PyliveTheme

/**
 * Racine de l'interface : navigation par états, gestion du retour système,
 * mini-lecteur persistant, boîtes de dialogue et messages.
 */
@Composable
fun PyliveRoot(vm: MusicViewModel = viewModel()) {
    PyliveTheme {
        val context = LocalContext.current
        val allTracks by vm.library.tracks.collectAsStateWithLifecycle()
        val loading by vm.library.loading.collectAsStateWithLifecycle()
        val libraryError by vm.library.error.collectAsStateWithLifecycle()
        val folderName by vm.library.folderName.collectAsStateWithLifecycle()
        val visibleTracks by vm.visibleTracks.collectAsStateWithLifecycle()
        val favoriteUris by vm.favoriteUris.collectAsStateWithLifecycle()
        val favoriteTracks by vm.favoriteTracks.collectAsStateWithLifecycle()
        val recentTracks by vm.recentTracks.collectAsStateWithLifecycle()
        val playlists by vm.playlists.collectAsStateWithLifecycle()
        val entries by vm.entries.collectAsStateWithLifecycle()
        val history by vm.history.collectAsStateWithLifecycle()
        val player by vm.playback.state.collectAsStateWithLifecycle()
        val playbackError by vm.playback.error.collectAsStateWithLifecycle()
        val compact by vm.compact.collectAsStateWithLifecycle()
        val sort by vm.sort.collectAsStateWithLifecycle()
        val query by vm.query.collectAsStateWithLifecycle()
        val notice by vm.notice.collectAsStateWithLifecycle()

        var permissionDenied by rememberSaveable { mutableStateOf(false) }
        val (permissionGranted, requestPermission) = rememberAudioPermission {
            vm.requestRefresh()
        }
        val pickFolder = rememberFolderPicker(
            onPicked = { uri -> vm.chooseFolder(uri) },
            onError = { vm.notice.value = Notice.Failure("folder") }
        )

        // Rafraîchit la bibliothèque à chaque retour au premier plan.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) vm.requestRefresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val state = UiSnapshot(
            allTracks = allTracks,
            visibleTracks = visibleTracks,
            favoriteUris = favoriteUris,
            favoriteTracks = favoriteTracks,
            recentTracks = recentTracks,
            playlists = playlists,
            entries = entries,
            history = history,
            player = player,
            loading = loading,
            query = query,
            compact = compact,
            sort = sort,
            permissionGranted = permissionGranted,
            permissionDenied = permissionDenied && !permissionGranted,
            folderName = folderName,
            libraryErrorCode = libraryError,
            playbackError = playbackError
        )

        var route by rememberSaveable { mutableStateOf("home") }
        var playlistDetailId by rememberSaveable { mutableStateOf(-1L) }
        var showCreatePlaylist by remember { mutableStateOf(false) }
        var renameTarget by remember { mutableStateOf<Playlist?>(null) }
        var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
        var clearHistoryConfirm by remember { mutableStateOf(false) }
        var addTarget by remember { mutableStateOf<Track?>(null) }
        var detailTarget by remember { mutableStateOf<Track?>(null) }
        var createForTrack by remember { mutableStateOf<Track?>(null) }

        val tab = when (route) {
            "home" -> PyliveTab.HOME
            "library" -> PyliveTab.LIBRARY
            "favorites" -> PyliveTab.FAVORITES
            "playlists", "playlist" -> PyliveTab.PLAYLISTS
            else -> null
        }

        fun goBack() {
            when (route) {
                "home", "library", "favorites", "playlists" -> Unit
                "playlist" -> route = "playlists"
                "queue" -> route = "player"
                else -> route = "home"
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PylivePalette.White
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    ) {
                        // En-tête contextuel
                        val isSecondary = route in setOf(
                            "player", "queue", "history", "settings", "playlist"
                        )
                        PyliveTopBar(
                            title = if (isSecondary) {
                                when (route) {
                                    "player" -> "Lecteur"
                                    "queue" -> "File d’attente"
                                    "history" -> "Historique"
                                    "settings" -> "Réglages"
                                    "playlist" -> playlists.firstOrNull { it.id == playlistDetailId }?.name
                                        ?: "Playlist"
                                    else -> null
                                }
                            } else null,
                            subtitle = when (route) {
                                "player" -> player.title.takeIf { it.isNotBlank() }
                                "queue" -> if (player.queue.size == 1) "1 morceau" else "${player.queue.size} morceaux"
                                "settings" -> "Confidentialité, bibliothèque et affichage"
                                else -> null
                            },
                            onBack = if (isSecondary) {
                                { goBack() }
                            } else null,
                            onSettings = { route = "settings" },
                            onRefresh = if (!isSecondary) {
                                { vm.requestRefresh() }
                            } else null,
                            refreshing = loading
                        )

                        // Bandeau d'autorisation / dossier
                        if (!permissionGranted || folderName != null) {
                            PermissionBanner(
                                granted = permissionGranted,
                                denied = permissionDenied,
                                folderName = folderName,
                                onRequest = {
                                    permissionDenied = false
                                    requestPermission()
                                },
                                onPickFolder = pickFolder,
                                onOpenSettings = { openAppSettings(context) }
                            )
                        }

                        libraryError?.let { code ->
                            Box(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                                InfoBanner(
                                    title = if (code == "folder_revoked") {
                                        "Accès au dossier révoqué"
                                    } else {
                                        "Accès audio impossible"
                                    },
                                    body = if (code == "folder_revoked") {
                                        "Choisissez à nouveau le dossier, ou utilisez la bibliothèque Android."
                                    } else {
                                        "Vérifiez l’autorisation audio puis actualisez la bibliothèque."
                                    },
                                    tone = BannerTone.Warning,
                                    actionLabel = "Actualiser",
                                    onAction = { vm.requestRefresh() }
                                )
                            }
                        }

                        Box(Modifier.weight(1f)) {
                            when (route) {
                                "home" -> HomeScreen(
                                    state = state,
                                    onOpenLibrary = { route = "library" },
                                    onOpenPlayer = { route = "player" },
                                    onOpenHistory = { route = "history" },
                                    onOpenFavorites = { route = "favorites" },
                                    onPlay = { list, index -> vm.play(list, index) },
                                    onPlayShuffle = { list -> vm.play(list, 0, shuffle = true) },
                                    onFavorite = { vm.toggleFavorite(it.uri) },
                                    onEnqueue = { vm.enqueue(it) },
                                    onAddToPlaylist = { addTarget = it },
                                    onDetails = { detailTarget = it },
                                    onOpenPlaylist = { id ->
                                        playlistDetailId = id
                                        route = "playlist"
                                    },
                                    onCreatePlaylist = { showCreatePlaylist = true },
                                    onOpenPlaylists = { route = "playlists" }
                                )

                                "library" -> LibraryScreen(
                                    state = state,
                                    onQueryChange = { vm.query.value = it },
                                    onSortChange = { vm.setSort(it) },
                                    onToggleCompact = { vm.setCompact(!compact) },
                                    onPlayAll = { list -> vm.play(list) },
                                    onShuffleAll = { list -> vm.play(list, 0, shuffle = true) },
                                    onPlay = { list, index -> vm.play(list, index) },
                                    onFavorite = { vm.toggleFavorite(it.uri) },
                                    onEnqueue = { vm.enqueue(it) },
                                    onAddToPlaylist = { addTarget = it },
                                    onDetails = { detailTarget = it },
                                    onRefresh = {
                                        if (!permissionGranted) requestPermission() else vm.requestRefresh()
                                    }
                                )

                                "favorites" -> FavoritesScreen(
                                    state = state,
                                    onPlayAll = { list -> vm.play(list) },
                                    onShuffleAll = { list -> vm.play(list, 0, shuffle = true) },
                                    onPlay = { list, index -> vm.play(list, index) },
                                    onFavorite = { vm.toggleFavorite(it.uri) },
                                    onEnqueue = { vm.enqueue(it) },
                                    onAddToPlaylist = { addTarget = it },
                                    onDetails = { detailTarget = it },
                                    onOpenLibrary = { route = "library" }
                                )

                                "history" -> HistoryScreen(
                                    state = state,
                                    onPlay = { list, index -> vm.play(list, index) },
                                    onFavorite = { vm.toggleFavorite(it.uri) },
                                    onEnqueue = { vm.enqueue(it) },
                                    onAddToPlaylist = { addTarget = it },
                                    onDetails = { detailTarget = it },
                                    onClearHistory = { clearHistoryConfirm = true },
                                    onOpenLibrary = { route = "library" }
                                )

                                "playlists" -> PlaylistsScreen(
                                    state = state,
                                    onCreate = { showCreatePlaylist = true },
                                    onOpen = { playlist ->
                                        playlistDetailId = playlist.id
                                        route = "playlist"
                                    },
                                    onRename = { renameTarget = it },
                                    onDelete = { deleteTarget = it },
                                    onPlay = { playlist ->
                                        val list = vm.playlistTracks(playlist)
                                        vm.play(list)
                                    }
                                )

                                "playlist" -> {
                                    val playlist = playlists.firstOrNull { it.id == playlistDetailId }
                                    if (playlist == null) {
                                        LaunchedEffect(playlistDetailId) { route = "playlists" }
                                    } else {
                                        PlaylistDetailScreen(
                                            playlist = playlist,
                                            state = state,
                                            onPlayAll = { list -> vm.play(list) },
                                            onShuffleAll = { list -> vm.play(list, 0, shuffle = true) },
                                            onPlay = { list, index -> vm.play(list, index) },
                                            onMove = { from, to -> vm.moveInPlaylist(playlist.id, from, to) },
                                            onRemove = { uri -> vm.removeFromPlaylist(playlist.id, uri) },
                                            onOpenLibrary = { route = "library" }
                                        )
                                    }
                                }

                                "player" -> PlayerScreen(
                                    state = state,
                                    onSeek = { vm.playback.seekTo(it) },
                                    onSeekBy = { vm.playback.seekBy(it) },
                                    onToggle = { vm.playback.toggle() },
                                    onNext = { vm.playback.next() },
                                    onPrevious = { vm.playback.previous() },
                                    onShuffle = { vm.playback.toggleShuffle() },
                                    onRepeat = { vm.playback.cycleRepeat() },
                                    onFavorite = { player.uri?.let { vm.toggleFavorite(it) } },
                                    onOpenQueue = { route = "queue" },
                                    onOpenLibrary = { route = "library" }
                                )

                                "queue" -> QueueScreen(
                                    state = state,
                                    onJump = { vm.playback.jumpTo(it) },
                                    onMove = { from, to -> vm.playback.move(from, to) },
                                    onRemove = { vm.playback.removeAt(it) },
                                    onOpenLibrary = { route = "library" }
                                )

                                "settings" -> SettingsScreen(
                                    state = state,
                                    onSortChange = { vm.setSort(it) },
                                    onToggleCompact = { vm.setCompact(it) },
                                    onRefresh = { vm.requestRefresh() },
                                    onRequestPermission = {
                                        permissionDenied = false
                                        requestPermission()
                                    },
                                    onOpenAndroidSettings = { openAppSettings(context) },
                                    onPickFolder = pickFolder,
                                    onForgetFolder = { vm.chooseFolder(null) },
                                    versionName = BuildConfig.VERSION_NAME
                                )
                            }
                        }
                    }
                }

                // Mini-lecteur persistant, masqué sur l'écran de lecture complet.
                AnimatedVisibility(
                    visible = player.hasTrack && route != "player",
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    MiniPlayer(
                        state = player,
                        onOpenPlayer = { route = "player" },
                        onToggle = { vm.playback.toggle() },
                        onNext = { vm.playback.next() }
                    )
                }

                if (route != "player" && route != "queue" && route != "settings") {
                    Column(
                        Modifier.padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        )
                    ) {
                        PyliveNavigationBar(
                            selected = tab,
                            onSelect = { selected ->
                                route = when (selected) {
                                    PyliveTab.HOME -> "home"
                                    PyliveTab.LIBRARY -> "library"
                                    PyliveTab.FAVORITES -> "favorites"
                                    PyliveTab.PLAYLISTS -> "playlists"
                                }
                            }
                        )
                    }
                } else {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            )
                    )
                }
            }
        }

        // Retour système : jamais de sortie accidentelle depuis un sous-écran.
        BackHandler(enabled = route !in setOf("home", "library", "favorites", "playlists")) {
            goBack()
        }

        // --- Boîtes de dialogue --------------------------------------------
        if (showCreatePlaylist) {
            PlaylistNameDialog(
                title = "Créer une playlist",
                confirmLabel = "Créer",
                onDismiss = { showCreatePlaylist = false },
                onConfirm = { name ->
                    vm.createPlaylist(name)
                    createForTrack?.let { track ->
                        val created = playlists.maxByOrNull { it.id }
                        created?.let { vm.addToPlaylist(it.id, track.uri) }
                        createForTrack = null
                    }
                }
            )
        }
        renameTarget?.let { playlist ->
            PlaylistNameDialog(
                title = "Renommer la playlist",
                initialName = playlist.name,
                onDismiss = { renameTarget = null },
                onConfirm = { name -> vm.renamePlaylist(playlist.id, name) }
            )
        }
        deleteTarget?.let { playlist ->
            ConfirmDialog(
                title = "Supprimer « ${playlist.name} » ?",
                body = "Les fichiers audio ne seront pas supprimés. La playlist sera définitivement effacée.",
                confirmLabel = "Supprimer",
                onConfirm = { vm.deletePlaylist(playlist.id) },
                onDismiss = { deleteTarget = null }
            )
        }
        if (clearHistoryConfirm) {
            ConfirmDialog(
                title = "Effacer l’historique ?",
                body = "Les fichiers audio ne seront pas supprimés. Seule la liste des dernières écoutes sera vidée.",
                confirmLabel = "Effacer",
                onConfirm = { vm.clearHistory() },
                onDismiss = { clearHistoryConfirm = false }
            )
        }
        addTarget?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                onSelect = { playlist ->
                    vm.addToPlaylist(playlist.id, track.uri)
                    addTarget = null
                },
                onCreateNew = {
                    createForTrack = track
                    addTarget = null
                    showCreatePlaylist = true
                },
                onDismiss = { addTarget = null }
            )
        }
        detailTarget?.let { track ->
            TrackDetailsSheet(
                track = track,
                isFavorite = track.uri in favoriteUris,
                onDismiss = { detailTarget = null },
                onEnqueue = {
                    vm.enqueue(track)
                    detailTarget = null
                },
                onToggleFavorite = { vm.toggleFavorite(track.uri) }
            )
        }

        playbackError?.let { failure ->
            NoticeDialog(
                title = "Lecture impossible",
                body = "Le fichier est inaccessible ou son codec n’est pas pris en charge " +
                    "(${failure.code}). Passez au morceau suivant ou retirez-le de la file d’attente.",
                onDismiss = { vm.playback.clearError() }
            )
        }
        notice?.let { message ->
            val (title, body) = when (message) {
                is Notice.Success -> "Ajouté" to
                    if (message.text.isBlank()) "Le morceau a été ajouté." else "« ${message.text} » a été ajouté."
                is Notice.Failure -> "Action impossible" to when (message.text) {
                    "connecting" -> "Le service audio se connecte. Réessayez dans un instant."
                    "missing" -> "Ce fichier a été déplacé, supprimé, ou son accès a été révoqué. " +
                        "La bibliothèque va être actualisée."
                    "inaccessible" -> "Ce fichier n’est plus accessible. Actualisez la bibliothèque."
                    "folder" -> "Impossible de conserver l’accès à ce dossier."
                    else -> message.text
                }
                is Notice.Info -> "Information" to message.text
            }
            NoticeDialog(title = title, body = body, onDismiss = { vm.dismissNotice() })
        }
    }
}
