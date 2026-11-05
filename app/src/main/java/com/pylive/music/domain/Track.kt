package com.pylive.music.domain

/**
 * Un morceau de la bibliothèque locale.
 *
 * L'identité d'un morceau est son URI : on ne devine jamais un fichier à partir
 * d'un nom potentiellement ambigu. Un fichier déplacé ou réindexé apparaît donc
 * comme « Fichier indisponible » dans les playlists, et reste retirable.
 */
data class Track(
    val uri: String,
    val title: String,
    val artist: String = "Artiste inconnu",
    val album: String = "Album inconnu",
    val durationMs: Long = 0L,
    val addedAtSeconds: Long = 0L,
    val artworkUri: String? = null,
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val source: Source = Source.MEDIA_STORE
) {
    enum class Source { MEDIA_STORE, FOLDER }

    /** Durée lisible : « 3:07 », « 1:02:45 ». Jamais de valeur négative affichée. */
    val durationLabel: String get() = formatDuration(durationMs)
}

enum class MusicSort { TITLE, ARTIST, ALBUM, DURATION, ADDED }

enum class RepeatMode { OFF, ALL, ONE }

/**
 * Règles pures de la bibliothèque, testables sans Android : recherche, tri,
 * résolution des références persistées, déplacement dans une file.
 */
object LibraryRules {

    /** Recherche insensible à la casse et aux accents sur titre, artiste, album et nom de fichier. */
    fun select(tracks: List<Track>, query: String, sort: MusicSort): List<Track> {
        val needle = normalize(query.trim())
        val found = if (needle.isEmpty()) tracks else tracks.filter { track ->
            normalize(track.title).contains(needle) ||
                normalize(track.artist).contains(needle) ||
                normalize(track.album).contains(needle) ||
                normalize(track.fileName).contains(needle)
        }
        return sort(found, sort)
    }

    fun sort(tracks: List<Track>, sort: MusicSort): List<Track> = when (sort) {
        MusicSort.TITLE -> tracks.sortedWith(
            compareBy<Track> { normalize(it.title) }.thenBy { normalize(it.artist) }
        )
        MusicSort.ARTIST -> tracks.sortedWith(
            compareBy<Track> { normalize(it.artist) }.thenBy { normalize(it.album) }
                .thenBy { normalize(it.title) }
        )
        MusicSort.ALBUM -> tracks.sortedWith(
            compareBy<Track> { normalize(it.album) }.thenBy { normalize(it.title) }
        )
        MusicSort.DURATION -> tracks.sortedBy { it.durationMs }
        MusicSort.ADDED -> tracks.sortedWith(
            compareByDescending<Track> { it.addedAtSeconds }.thenBy { normalize(it.title) }
        )
    }

    /** Retrouve les morceaux correspondant à des URI persistées, dans l'ordre demandé. */
    fun resolve(uris: List<String>, tracks: List<Track>): List<Track> {
        if (uris.isEmpty() || tracks.isEmpty()) return emptyList()
        val index = tracks.associateBy { it.uri }
        return uris.mapNotNull { index[it] }
    }

    /** Déduplique par URI en conservant le premier occurrence (MediaStore prioritaire). */
    fun distinct(tracks: List<Track>): List<Track> = tracks.distinctBy { it.uri }

    /** Normalisation utilitaire : minuscules, accents retirés, espaces réduits. */
    fun normalize(value: String): String {
        val lowered = value.lowercase()
        val builder = StringBuilder(lowered.length)
        for (char in lowered) {
            val plain = when (char) {
                'à', 'á', 'â', 'ã', 'ä', 'å' -> 'a'
                'ç' -> 'c'
                'è', 'é', 'ê', 'ë' -> 'e'
                'ì', 'í', 'î', 'ï' -> 'i'
                'ñ' -> 'n'
                'ò', 'ó', 'ô', 'õ', 'ö', 'ø' -> 'o'
                'ù', 'ú', 'û', 'ü' -> 'u'
                'ý', 'ÿ' -> 'y'
                'œ' -> 'o'
                'æ' -> 'a'
                'ß' -> 's'
                else -> char
            }
            builder.append(plain)
        }
        return builder.toString().trim().replace(Regex("\\s+"), " ")
    }
}

object QueueRules {
    /** Déplace un élément, en tolérant des indices hors bornes (aucune exception). */
    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices || from == to) return items
        val copy = items.toMutableList()
        copy.add(to, copy.removeAt(from))
        return copy
    }
}

object PermissionPolicy {
    /** Android 13+ utilise READ_MEDIA_AUDIO, les versions antérieures READ_EXTERNAL_STORAGE. */
    fun audioPermission(sdkInt: Int): String =
        if (sdkInt >= 33) "android.permission.READ_MEDIA_AUDIO"
        else "android.permission.READ_EXTERNAL_STORAGE"
}

object RepeatCycle {
    /** Cycle arrêtée → file → titre → arrêtée. */
    fun next(mode: RepeatMode): RepeatMode = when (mode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
    }
}

/** Formatage « 3:07 » ou « 1:02:45 » : jamais de durée négative ni de tiret. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (if (millis < 0) 0 else millis) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/** Taille lisible pour l'écran de détails. */
fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024L -> "$bytes o"
    bytes < 1024L * 1024L -> "%.0f Ko".format(bytes / 1024.0)
    else -> "%.1f Mo".format(bytes / (1024.0 * 1024.0))
}

/** Date courte lisible, sans dépendance à la locale de l'appareil. */
fun formatDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "—"
    val date = java.util.Date(epochSeconds * 1000)
    return java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.FRENCH).format(date)
}
