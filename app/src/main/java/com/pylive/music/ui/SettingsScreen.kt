package com.pylive.music.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pylive.music.R
import com.pylive.music.domain.MusicSort
import com.pylive.music.ui.theme.PylivePalette

/**
 * Réglages : présentation, accès à la bibliothèque, confidentialité et aide.
 * Chaque ligne comporte un libellé explicite et, si nécessaire, une explication.
 */
@Composable
fun SettingsScreen(
    state: UiSnapshot,
    onSortChange: (MusicSort) -> Unit,
    onToggleCompact: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onPickFolder: () -> Unit,
    onForgetFolder: () -> Unit,
    versionName: String
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(PylivePalette.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 32.dp)
    ) {
        item { SettingsSectionTitle("Affichage") }
        item {
            SettingRow(
                title = "Liste compacte",
                detail = "Affiche plus de titres à l’écran, sans pochettes.",
                trailing = {
                    Switch(
                        checked = state.compact,
                        onCheckedChange = onToggleCompact,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PylivePalette.White,
                            checkedTrackColor = PylivePalette.Violet,
                            uncheckedThumbColor = PylivePalette.White,
                            uncheckedTrackColor = PylivePalette.Outline,
                            uncheckedBorderColor = PylivePalette.Outline
                        )
                    )
                }
            )
        }
        item {
            SettingRow(
                title = "Tri de la bibliothèque",
                detail = "Tri actuel : ${sortLabel(state.sort)}",
                trailing = { SortChip(state.sort, onSortChange) }
            )
        }

        item { SettingsSectionTitle("Bibliothèque locale") }
        item {
            SettingRow(
                title = "Morceaux détectés",
                detail = when (state.allTracks.size) {
                    0 -> "Aucun fichier audio détecté pour l’instant."
                    1 -> "1 fichier audio disponible sur cet appareil."
                    else -> "${state.allTracks.size} fichiers audio disponibles sur cet appareil."
                }
            )
        }
        item {
            ActionRow(
                icon = R.drawable.ic_refresh,
                title = "Actualiser maintenant",
                detail = "Relit MediaStore et le dossier facultatif.",
                onClick = onRefresh
            )
        }
        item {
            ActionRow(
                icon = R.drawable.ic_shield,
                title = if (state.permissionGranted) "Accès audio accordé" else "Demander l’accès audio",
                detail = if (state.permissionGranted) {
                    "Pylive Music peut lire les fichiers audio du téléphone."
                } else {
                    "Nécessaire pour afficher la musique enregistrée sur le téléphone."
                },
                onClick = onRequestPermission,
                highlighted = !state.permissionGranted
            )
        }
        item {
            ActionRow(
                icon = R.drawable.ic_info,
                title = "Autorisations Android",
                detail = "Ouvre la fiche de l’application dans les réglages du téléphone.",
                onClick = onOpenAndroidSettings
            )
        }
        item {
            ActionRow(
                icon = R.drawable.ic_folder,
                title = "Choisir un dossier facultatif",
                detail = state.folderName?.let { "Dossier analysé : $it" }
                    ?: "Utile si Android n’indexe pas certains fichiers.",
                onClick = onPickFolder
            )
        }
        if (state.folderName != null) {
            item {
                ActionRow(
                    icon = R.drawable.ic_close,
                    title = "Oublier le dossier facultatif",
                    detail = "Retire uniquement l’accès au dossier choisi.",
                    onClick = onForgetFolder
                )
            }
        }

        item { SettingsSectionTitle("Confidentialité") }
        item {
            PrivacyPanel()
        }

        item { SettingsSectionTitle("Dépannage") }
        item {
            Surface(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Un titre manque ? Vérifiez l’autorisation audio puis actualisez. Un fichier " +
                        "déplacé peut changer de référence : ajoutez la nouvelle entrée à vos playlists, " +
                        "les anciennes références restent retirables. Un même fichier exposé par MediaStore " +
                        "et par un dossier peut apparaître en double.\n\n" +
                        "Les fichiers privés ou protégés des applications de streaming ne sont pas " +
                        "accessibles. La prise en charge des formats MP3, M4A, AAC, FLAC, WAV, OGG et Opus " +
                        "dépend du codec et du téléphone.\n\n" +
                        "La lecture continue dans un service multimédia : appels, perte de focus audio et " +
                        "déconnexion d’un casque sont gérés par le moteur Android. Un arrêt forcé par le " +
                        "système interrompt la lecture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PylivePalette.Ink,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { SettingsSectionTitle("À propos") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark(size = 40.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Pylive Music",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PylivePalette.Ink
                    )
                    Text(
                        "Version $versionName • Android natif, Kotlin et Media3",
                        style = MaterialTheme.typography.bodySmall,
                        color = PylivePalette.InkSoft
                    )
                }
            }
        }
        item {
            Text(
                text = "Sans compte, sans serveur, sans publicité, sans télémétrie. " +
                    "Aucune permission Internet. Les favoris, playlists, historiques et réglages " +
                    "sont enregistrés uniquement sur cet appareil. Désinstaller l’application " +
                    "efface ces données, jamais vos fichiers audio.",
                style = MaterialTheme.typography.bodySmall,
                color = PylivePalette.InkSoft,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = PylivePalette.Violet,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = PylivePalette.Ink
            )
            Spacer(Modifier.height(2.dp))
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = PylivePalette.InkSoft
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun ActionRow(
    icon: Int,
    title: String,
    detail: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) PylivePalette.VioletSoft else PylivePalette.White
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (highlighted) PylivePalette.White else PylivePalette.VioletSoft,
                modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PyliveIcon(icon, null, tint = PylivePalette.Violet, size = 21.dp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (highlighted) PylivePalette.VioletDeep else PylivePalette.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = PylivePalette.InkSoft,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PyliveIcon(R.drawable.ic_chevron_right, null, tint = PylivePalette.InkSoft, size = 20.dp)
        }
    }
}

@Composable
private fun PrivacyPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        listOf(
            "Aucune création de compte",
            "Aucun serveur obligatoire",
            "Aucune publicité, aucune télémétrie",
            "Aucune collecte de données personnelles",
            "Aucune permission Internet"
        ).forEach { line ->
            Row(
                Modifier.padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PylivePalette.Yellow,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PyliveIcon(R.drawable.ic_check, null, tint = PylivePalette.Ink, size = 14.dp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PylivePalette.Ink
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Vos fichiers audio ne quittent jamais le téléphone. La lecture fonctionne " +
                "sans connexion Internet.",
            style = MaterialTheme.typography.bodySmall,
            color = PylivePalette.InkSoft
        )
    }
}
