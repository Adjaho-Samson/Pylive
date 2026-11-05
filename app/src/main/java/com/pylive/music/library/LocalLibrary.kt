package com.pylive.music.library

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.pylive.music.domain.LibraryRules
import com.pylive.music.domain.PermissionPolicy
import com.pylive.music.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Détection automatique des fichiers audio du téléphone.
 *
 * Deux sources complémentaires :
 *  1. MediaStore (source principale, données déjà indexées par Android) ;
 *  2. un dossier choisi facultativement par l'utilisatrice via le sélecteur
 *     Android (SAF), utile quand un fournisseur n'indexe pas certains fichiers.
 *
 * Aucune écriture, aucune suppression et aucun envoi : l'application ne fait que
 * lire des métadonnées et ouvrir des flux audio en lecture seule.
 */
class LocalLibrary(
    private val context: Context,
    private val scope: CoroutineScope
) {

    val tracks = MutableStateFlow<List<Track>>(emptyList())
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val folderName = MutableStateFlow<String?>(null)

    private var scanJob: Job? = null
    private var folderUri: String? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Regroupe les rafales de notifications émises par MediaStore.
            refresh(delayMs = 600)
        }
    }

    init {
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
            )
        }
    }

    /** Point d'entrée des réglages : dossier facultatif à analyser. */
    fun setFolder(uri: String?) {
        if (folderUri == uri) return
        folderUri = uri
        folderName.value = uri?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it))?.name }.getOrNull() }
        refresh()
    }

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, PermissionPolicy.audioPermission(android.os.Build.VERSION.SDK_INT)
    ) == PackageManager.PERMISSION_GRANTED

    fun refresh(delayMs: Long = 0L) {
        scanJob?.cancel()
        scanJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            loading.value = true
            try {
                error.value = null
                val scanned = withContext(Dispatchers.IO) { scan() }
                tracks.value = scanned
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error.value = "scan_failed"
            } finally {
                loading.value = false
            }
        }
    }

    private fun scan(): List<Track> {
        val result = mutableListOf<Track>()
        if (hasAudioPermission()) result += scanMediaStore()
        folderUri?.let { tree -> result += scanFolder(tree) }
        return LibraryRules.distinct(result)
    }

    private fun scanMediaStore(): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE
        )
        val rows = mutableListOf<Track>()
        context.contentResolver.query(collection, columns, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            fun text(column: Int): String? = cursor.getString(column)
                ?.takeUnless { it.isBlank() || it.equals("<unknown>", ignoreCase = true) }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val fileName = text(nameColumn) ?: "Audio"
                val albumId = cursor.getLong(albumIdColumn)
                rows += Track(
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    title = text(titleColumn) ?: fileName.substringBeforeLast('.'),
                    artist = text(artistColumn) ?: "Artiste inconnu",
                    album = text(albumColumn) ?: "Album inconnu",
                    durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                    addedAtSeconds = cursor.getLong(addedColumn),
                    artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId
                        ).toString()
                    } else null,
                    fileName = fileName,
                    sizeBytes = cursor.getLong(sizeColumn),
                    source = Track.Source.MEDIA_STORE
                )
            }
        }
        return rows
    }

    private fun scanFolder(treeUri: String): List<Track> {
        val found = mutableListOf<Track>()
        try {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return found
            val pending = ArrayDeque<DocumentFile>()
            pending.add(root)
            var visited = 0
            while (pending.isNotEmpty() && visited < MAX_FOLDER_ENTRIES) {
                val directory = pending.removeFirst()
                val children = runCatching { directory.listFiles() }.getOrDefault(emptyArray())
                for (file in children) {
                    visited++
                    if (visited >= MAX_FOLDER_ENTRIES) break
                    if (file.isDirectory) {
                        pending.add(file)
                        continue
                    }
                    val name = file.name ?: continue
                    val looksLikeAudio = file.type?.startsWith("audio/") == true ||
                        name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS
                    if (!looksLikeAudio) continue
                    found += readFolderFile(file, name)
                }
            }
            folderName.value = runCatching { root.name }.getOrNull() ?: folderName.value
        } catch (revoked: SecurityException) {
            error.value = "folder_revoked"
        }
        return found
    }

    private fun readFolderFile(file: DocumentFile, name: String): Track {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file.uri)
            fun metadata(key: Int): String? = retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }
            Track(
                uri = file.uri.toString(),
                title = metadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name.substringBeforeLast('.'),
                artist = metadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artiste inconnu",
                album = metadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Album inconnu",
                durationMs = metadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                addedAtSeconds = file.lastModified() / 1000L,
                artworkUri = null,
                fileName = name,
                sizeBytes = file.length(),
                source = Track.Source.FOLDER
            )
        } catch (unsupported: Exception) {
            // Un fichier illisible n'est pas une erreur bloquante : on garde son nom.
            Track(
                uri = file.uri.toString(),
                title = name.substringBeforeLast('.'),
                fileName = name,
                sizeBytes = file.length(),
                addedAtSeconds = file.lastModified() / 1000L,
                source = Track.Source.FOLDER
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Vérifie que le fichier est réellement ouvrable avant de lancer la lecture. */
    suspend fun isAccessible(uri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    fun close() {
        scanJob?.cancel()
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "m4b", "wma", "aiff", "aif", "mp4a")
        const val MAX_FOLDER_ENTRIES = 5000
    }
}
