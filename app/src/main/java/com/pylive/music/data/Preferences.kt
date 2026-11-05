package com.pylive.music.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pylive.music.domain.MusicSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pyliveSettings by preferencesDataStore(name = "pylive_settings")

/**
 * Réglages persistants, strictement locaux : tri de la bibliothèque, affichage
 * compact, dossier facultatif choisi via le sélecteur Android (SAF).
 */
class Preferences(private val context: Context) {

    private val sortKey = stringPreferencesKey("sort")
    private val compactKey = booleanPreferencesKey("compact")
    private val folderKey = stringPreferencesKey("folder")

    val sort: Flow<MusicSort> = context.pyliveSettings.data.map { prefs ->
        runCatching { MusicSort.valueOf(prefs[sortKey] ?: MusicSort.TITLE.name) }
            .getOrDefault(MusicSort.TITLE)
    }

    val compact: Flow<Boolean> = context.pyliveSettings.data.map { prefs ->
        prefs[compactKey] ?: false
    }

    val folder: Flow<String?> = context.pyliveSettings.data.map { prefs ->
        prefs[folderKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun setSort(value: MusicSort) {
        context.pyliveSettings.edit { it[sortKey] = value.name }
    }

    suspend fun setCompact(value: Boolean) {
        context.pyliveSettings.edit { it[compactKey] = value }
    }

    suspend fun setFolder(uri: String?) {
        context.pyliveSettings.edit { prefs ->
            if (uri.isNullOrBlank()) prefs.remove(folderKey) else prefs[folderKey] = uri
        }
    }
}
