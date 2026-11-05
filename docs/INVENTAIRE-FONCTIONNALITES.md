# Inventaire du projet source et correspondance Pylive Music

Ce document recense ce qui est **réellement implémenté** dans
`Adjaho-Samson/Tchatz-Music-Android` (dossier `Tchatz-Music/`), puis indique comment
Pylive Music reprend, adapte ou remplace chaque élément. Le projet source n'a été ni
modifié ni copié : il a été lu, compris, puis reconstruit avec sa propre identité.

## 1. Architecture du projet source

| Élément | Tchatz Music | Pylive Music |
|---|---|---|
| Langage | Kotlin | Kotlin |
| Interface | Jetpack Compose + Material 3 | Compose + Material 3, thème propre |
| Lecture | Media3 ExoPlayer 1.6.1 + `MediaSessionService` | identique (réutilisé, API vérifiée) |
| Bibliothèque | MediaStore + `ContentObserver` + dossier SAF | identique, enrichi (recherche accentuée, plafond de parcours) |
| Persistance | Room 2.7.1 (favoris, playlists, historique) + DataStore | identique, DAO transactionnel clarifié |
| Images | Coil (pochettes) | Coil |
| Injection | aucune, singletons dans `Application` | identique |
| Packages | `com.tchatz.music` | `com.pylive.music` |
| Thème | sombre (noir/gris/rouge `#F1394B`) | **clair** (blanc/violet `#B80DB8`/jaune `#F5D328`) |
| Police | police système | **Roboto embarquée** (Regular/Medium/Bold) |
| Icônes | Material (`material-icons-core`) + 9 icônes copiées | **jeu maison de 38 icônes vectorielles** |

## 2. Fonctionnalités du projet source → statut dans Pylive Music

### Bibliothèque

| Fonction source | Fichier source | Pylive Music |
|---|---|---|
| Scan MediaStore complet (tous les fichiers audio, pas seulement `IS_MUSIC`) | `library/LocalLibrary.kt` | **Repris**, mêmes colonnes, `artworkUri` seulement si `album_id > 0` |
| Repli métadonnées (`<unknown>`, blanc → nom de fichier) | idem | **Repris** |
| `ContentObserver` avec temporisation 600 ms | idem | **Repris** |
| Rafraîchissement au retour au premier plan | `ui/TchatzRoot.kt` | **Repris** (observation `ON_RESUME`) |
| Rafraîchissement manuel | idem | **Repris** (en-tête + réglages) |
| Dossier SAF facultatif, parcours récursif, `MediaMetadataRetriever` | `LocalLibrary.kt` | **Repris**, avec plafond de 5 000 entrées et extensions élargies |
| Autorisation persistante du dossier | `TchatzRoot.kt` | **Repris** |
| Détection d'URI inaccessibles (`openAssetFileDescriptor`) | `LocalLibrary.kt` | **Repris** |
| Recherche insensible à la casse | `domain/Track.kt` | **Étendu** : insensible aussi aux accents |
| Tri titre / artiste / album / durée / date | idem | **Repris** + tri secondaire stable |
| Déduplication par URI | idem | **Repris** (`LibraryRules.distinct`) |
| Vue liste avec pochettes / liste compacte | `ui/Components.kt` | **Repris** |
| Message d'erreur de scan | `LocalLibrary.kt` | **Repris**, transformé en code + bandeau lisible |

### Lecture

| Fonction source | Fichier source | Pylive Music |
|---|---|---|
| ExoPlayer + attributs audio + `handleAudioBecomingNoisy` + `WAKE_MODE_LOCAL` | `playback/MusicService.kt` | **Repris à l'identique** |
| `MediaSessionService` + `MediaSession` + `sessionActivity` | idem | **Repris** + identifiant de session et `onTaskRemoved` explicite |
| Notification multimédia Media3 | (fournie par Media3) | **Repris** + canal et nom propres, petite icône dédiée |
| Lecture / pause / reprise | `PlaybackConnection.kt` | **Repris** |
| Suivant / précédent | idem | **Repris**, précédent revenant au début après 3 s |
| Déplacement dans la chanson (barre de progression) | `ui/PlayerScreen.kt` | **Repris** (glissement local puis `seekTo`) |
| Aléatoire | idem | **Repris** |
| Répétition arrêtée / file / titre | idem | **Repris** (`RepeatCycle` testé) |
| Lecture d'une liste entière | idem | **Repris** |
| Ajout à la file | idem | **Repris** |
| File : saut, retrait, réorganisation | idem | **Repris** |
| Historique des 100 dernières références | `MusicService.kt` | **Repris** |
| Vérification d'accès avant lecture | `MusicViewModel.kt` | **Repris** |
| Message d'erreur de codec avec code | `PlaybackConnection.kt` | **Repris** (`errorCodeName`) |
| Mini-lecteur persistant | `TchatzRoot.kt` | **Repris**, redessiné (violet, texte blanc) |

**Ajouts Pylive Music (sans retirer l'existant) :** avancer/reculer de 10 s,
indicateur « en cours de lecture » dans les listes, fiche de détails du morceau
(album, fichier, taille, date, source, URI), bouton « Lire les N résultats » quand une
recherche est active.

### Organisation

| Fonction source | Pylive Music |
|---|---|
| Favoris (Room, bascule) | **Repris** |
| Playlists : créer / renommer / supprimer | **Repris** + confirmation de suppression explicite |
| Ajout / retrait de morceaux | **Repris** (doublons ignorés silencieusement) |
| Réorganisation par flèches | **Repris** (transaction atomique) |
| Références indisponibles visibles et retirables | **Repris**, état d'erreur clairement signalé |
| Historique effaçable avec confirmation | **Repris** |
| Accueil : à l'écoute, dernières écoutes, récents, favoris, playlists | **Repris**, hiérarchie repensée |

### Autorisations et vie privée

| Élément source | Pylive Music |
|---|---|
| `READ_MEDIA_AUDIO` (13+) / `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) | **Identique** |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | **Identique** |
| Aucune permission Internet | **Identique** (vérifié par `scripts/check_project.py`) |
| Sauvegarde cloud désactivée | **Identique** + `fullBackupContent="false"` |
| Bandeau d'explication avant demande | **Repris**, texte et contrastes revus |
| Accès direct aux réglages Android en cas de refus | **Repris** |

## 3. Ce que Pylive Music ne reprend pas

- **La palette et le thème sombre** du projet source : remplacés par l'identité
  blanche / violette / jaune imposée.
- **Les icônes Material copiées** dans `androidx/compose/material/icons/rounded/` :
  remplacées par un jeu maison, sans dépendance à `material-icons-extended`.
- **Le nom, le package, le logo et les ressources** du projet source.
- Le formatage compact du code source (lignes très denses) : Pylive Music utilise un
  code lisible, commenté en français et vérifié par des contrôles statiques.

## 4. Fonctionnalités du projet source volontairement non ajoutées

Le projet source ne contient **pas** d'égaliseur, de paroles, de grille d'albums, de
glisser-déposer, d'import de fichiers, de favoris synchronisés en ligne, de reprise
automatique au démarrage, ni de migration depuis l'ancienne PWA. Ces absences sont
conservées : elles n'ont pas été demandées et ne doivent pas être simulées.

## 5. Vérifications automatisées du dépôt

`python3 scripts/check_project.py` vérifie notamment : identité (`com.pylive.music`,
API 26, nom d'application), absence des permissions réseau et de stockage global,
palette exacte, validité de tous les XML, existence de chaque `R.drawable` / `R.font` /
`R.string` référencé, cohérence des imports internes, composants déclarés dans le
manifeste, schéma Room et étapes du workflow CI.

`app/src/test/` couvre les règles pures (recherche accentuée, tris, références
disparues, file, permissions par version, formatage des durées et tailles).
`app/src/androidTest/` couvre la persistance Room réelle, la mécanique ExoPlayer et la
création d'une `MediaSession`, ainsi que le scan de bibliothèque sans plantage.
