package com.pylive.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pylive.music.MainActivity
import com.pylive.music.PyliveApp
import com.pylive.music.R
import com.pylive.music.data.Recent
import kotlinx.coroutines.launch

/**
 * Service de lecture réel (ExoPlayer + MediaSession).
 *
 * Il fournit la notification multimédia, les commandes de l'écran verrouillé,
 * des casques filaires, des écouteurs Bluetooth, des commandes vocales système
 * et de la télécommande. La gestion du focus audio et la mise en pause à la
 * déconnexion d'un casque sont déléguées à ExoPlayer, qui ne simule jamais la
 * lecture : chaque position affichée provient du moteur audio.
 */
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            // Pause automatique si le casque est débranché : jamais de musique
            // surprise dans le haut-parleur.
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }

        val app = application as PyliveApp
        var lastRecordedUri: String? = null

        fun recordHistory() {
            val uri = player.currentMediaItem?.mediaId ?: return
            if (player.isPlaying && uri != lastRecordedUri) {
                lastRecordedUri = uri
                app.appScope.launch {
                    app.database.dao().played(Recent(uri, System.currentTimeMillis()))
                }
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) recordHistory() else lastRecordedUri = null
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                lastRecordedUri = null
                recordHistory()
            }
        })

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.notification_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) }
        )

        session = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * Comportement par défaut conservé : si la lecture est en cours, retirer
     * l'application des récents ne coupe pas la musique ; sinon le service
     * s'arrête proprement et les ressources sont libérées.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        const val SESSION_ID = "pylive-music"
        const val NOTIFICATION_CHANNEL_ID = "pylive_playback"
        const val NOTIFICATION_ID = 3108
    }
}
