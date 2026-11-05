package com.pylive.music

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pylive.music.ui.PyliveRoot

/**
 * Point d'entrée unique de Pylive Music.
 *
 * L'application n'embarque aucune WebView : l'interface est entièrement Compose.
 * Les barres système restent transparentes et le contenu passe sous elles ; les
 * marges sont gérées dans les composables.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Aucune reprise automatique : l'écran de démarrage disparaît dès que
        // l'interface est prête, sans bloquer inutilement.
        splash.setKeepOnScreenCondition { false }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent { PyliveRoot() }
    }

    /** Une notification peut rouvrir l'application : rien à réinitialiser. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
