package com.pylive.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.pylive.music.domain.RepeatMode
import com.pylive.music.domain.RepeatCycle
import com.pylive.music.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** État immuable du lecteur, consommé par l'interface. */
data class PlaybackState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val uri: String? = null,
    val artworkUri: String? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val queue: List<QueueEntry> = emptyList(),
    val index: Int = -1,
    val connected: Boolean = false
) {
    val hasTrack: Boolean get() = uri != null
    val canPlayPause: Boolean get() = hasTrack && connected
    val progress: Float
        get() = when {
            durationMs <= 0L -> 0f
            else -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

data class QueueEntry(
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long
)

/**
 * Connexion au service de lecture Media3.
 *
 * Le contrôleur expose des commandes réelles (lecture, pause, suivant,
 * précédent, déplacement, déplacement dans la file) et publie un état rafraîchi
 * toutes les 400 ms pour animer la barre de progression. Toute erreur de codec
 * ou de fichier remonte ici avec son code, pour être affichée telle quelle.
 */
class PlaybackConnection(
    private val context: Context,
    private val scope: CoroutineScope
) {

    val state = MutableStateFlow(PlaybackState())
    val error = MutableStateFlow<PlaybackError?>(null)

    private var controller: MediaController? = null
    private val token = SessionToken(context, ComponentName(context, MusicService::class.java))
    private val future = MediaController.Builder(context, token).buildAsync()
    private var ticker: Job? = null
    private var queueDirty = true

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED)
            ) {
                queueDirty = true
            }
            publish()
        }

        override fun onPlayerError(error: PlaybackException) {
            this@PlaybackConnection.error.value = PlaybackError(
                code = error.errorCodeName,
                message = error.message
            )
            publish()
        }
    }

    init {
        future.addListener({
            runCatching { future.get() }
                .onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    queueDirty = true
                    publish()
                }
                .onFailure {
                    error.value = PlaybackError(code = "SERVICE", message = null)
                }
        }, ContextCompat.getMainExecutor(context))

        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(400)
            }
        }
    }

    private fun publish() {
        val player = controller ?: return
        val item = player.currentMediaItem
        val queue = if (queueDirty || state.value.queue.size != player.mediaItemCount) {
            queueDirty = false
            (0 until player.mediaItemCount).map { index ->
                val media = player.getMediaItemAt(index)
                QueueEntry(
                    uri = media.mediaId,
                    title = media.mediaMetadata.title?.toString() ?: "Piste",
                    artist = media.mediaMetadata.artist?.toString() ?: "",
                    durationMs = media.mediaMetadata.extras?.getLong(KEY_DURATION) ?: 0L
                )
            }
        } else {
            state.value.queue
        }

        state.value = PlaybackState(
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            uri = item?.mediaId,
            artworkUri = item?.mediaMetadata?.artworkUri?.toString(),
            playing = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            queue = queue,
            index = player.currentMediaItemIndex,
            connected = true
        )
    }

    val isConnected: Boolean get() = controller != null

    /** Lance une liste entière à partir de l'index demandé. */
    fun play(tracks: List<Track>, index: Int = 0, shuffle: Boolean = false) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        error.value = null
        val items = tracks.map(::toMediaItem)
        val start = index.coerceIn(tracks.indices)
        player.setMediaItems(items, start, 0L)
        player.shuffleModeEnabled = shuffle
        player.prepare()
        player.play()
        queueDirty = true
        publish()
    }

    /** Ajoute un morceau en fin de file sans interrompre la lecture. */
    fun enqueue(track: Track) {
        val player = controller ?: return
        player.addMediaItem(toMediaItem(track))
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        queueDirty = true
        publish()
    }

    fun toggle() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) player.prepare()
            player.play()
        }
        publish()
    }

    fun next() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        else if (player.repeatMode == Player.REPEAT_MODE_ALL) player.seekTo(0, 0L)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        if (!player.isPlaying) player.play()
        queueDirty = true
        publish()
    }

    fun previous() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        // Comportement standard des lecteurs : revenir au début si l'on est déjà loin.
        if (player.currentPosition > RESTART_THRESHOLD_MS) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0, 0L)
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        if (!player.isPlaying) player.play()
        queueDirty = true
        publish()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        val target = (player.currentPosition + deltaMs)
            .coerceIn(0L, if (player.duration > 0) player.duration else 0L)
        player.seekTo(target)
        publish()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        publish()
    }

    fun cycleRepeat() {
        val player = controller ?: return
        val current = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        player.repeatMode = when (RepeatCycle.next(current)) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        publish()
    }

    fun jumpTo(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
        queueDirty = true
        publish()
    }

    fun removeAt(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            queueDirty = true
            publish()
        }
    }

    fun move(from: Int, to: Int) {
        val player = controller ?: return
        if (from in 0 until player.mediaItemCount && to in 0 until player.mediaItemCount && from != to) {
            player.moveMediaItem(from, to)
            queueDirty = true
            publish()
        }
    }

    fun clearError() {
        error.value = null
    }

    fun close() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controller = null
        runCatching { MediaController.releaseFuture(future) }
    }

    private fun toMediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId(track.uri)
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.artworkUri?.let(Uri::parse))
                .setIsPlayable(true)
                .setExtras(android.os.Bundle().apply { putLong(KEY_DURATION, track.durationMs) })
                .build()
        )
        .build()

    private companion object {
        const val KEY_DURATION = "pylive:duration"
        const val RESTART_THRESHOLD_MS = 3000L
    }
}

data class PlaybackError(val code: String, val message: String?)
