package com.pylive.music

import android.app.Application
import androidx.room.Room
import com.pylive.music.data.MusicDatabase
import com.pylive.music.data.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Conteneur de dépendances volontairement minimal (aucun framework d'injection) :
 * les singletons appartiennent à l'Application, les ressources d'écran au ViewModel.
 */
class PyliveApp : Application() {

    /** Portée applicative pour les écritures de l'historique faites par le service audio. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: MusicDatabase by lazy {
        Room.databaseBuilder(this, MusicDatabase::class.java, "pylive.db")
            // Aucune migration destructive : les données locales de l'utilisatrice
            // ne doivent jamais disparaître silencieusement lors d'une mise à jour.
            .build()
    }

    val preferences: Preferences by lazy { Preferences(this) }
}
