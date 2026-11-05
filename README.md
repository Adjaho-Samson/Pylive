# Pylive Music — application Android native

Lecteur de fichiers audio locaux pour Android **8.0 (API 26) et supérieur**, écrit en
**Kotlin** avec **Jetpack Compose**, **MediaStore** et **Android Media3 / ExoPlayer**.

Aucun compte, aucun serveur, aucune publicité, aucune télémétrie, aucune permission
Internet : la musique reste sur le téléphone.

**Identité visuelle :** violet `#B80DB8`, blanc `#FFFFFF`, jaune `#F5D328`.
**Identifiant Android :** `com.pylive.music` — installation indépendante de tout autre
lecteur, cohabitation possible avec Tchatz Music sur le même téléphone.

---

## 1. Obtenir l'APK sans Android Studio

1. Ouvrir l'onglet **Actions** du dépôt GitHub.
2. Choisir le workflow **Pylive Music APK** (il se lance à chaque `push` ; le bouton
   **Run workflow** permet aussi de le relancer à la demande).
3. Ouvrir la dernière exécution réussie du job **Tests, analyse et APK**.
4. Télécharger l'artefact **Pylive-Music-debug-APK**, puis extraire `app-debug.apk`.
5. Sur le téléphone, ouvrir le fichier et autoriser temporairement
   **Installer des applications inconnues** pour l'application source.

L'APK est aussi publié automatiquement dans **Releases** sous le nom
`pylive-music-debug.apk` à chaque exécution sur `main`.

> L'artefact téléchargé est une archive ZIP : il faut l'extraire avant de l'installer.
> Ne pas essayer d'installer le ZIP.

### Installer sur le téléphone

Transférer `app-debug.apk` par câble USB, Drive, ou le télécharger directement.
L'installateur Android demande la confirmation d'installation. Play Protect peut
analyser un APK de debug non publié sur le Play Store : n'installer que vos propres
builds ou une source de confiance.

Au premier lancement :

1. L'écran d'accueil explique la détection locale. Appuyer sur **Autoriser l'accès**.
2. Accepter l'accès aux fichiers audio. La bibliothèque se remplit depuis MediaStore,
   sans import manuel titre par titre.
3. Sélectionner un morceau, ou appuyer sur **Tout écouter** / **Aléatoire**.
4. En cas de refus, le bandeau reste visible : bouton **Réglages Android** pour rétablir
   l'autorisation, ou **Choisir un dossier** pour un accès limité à un dossier précis.

---

## 2. Compiler soi-même

### Prérequis

- JDK **17**
- Android SDK **35** (`ANDROID_HOME` défini) et Build-Tools 35.0.0
- Connexion Internet **uniquement pour compiler** (téléchargement des dépendances).
  L'application installée n'a besoin d'aucun réseau.

### Commandes

```sh
chmod +x gradlew
./gradlew :app:testDebugUnitTest     # tests JVM (règles de bibliothèque, formatage)
./gradlew :app:lintDebug             # analyse Android Lint
./gradlew :app:assembleDebug         # APK de debug
python3 scripts/check_project.py     # contrôles structurels du dépôt

# avec un téléphone ou un émulateur connecté :
./gradlew :app:connectedDebugAndroidTest
```

Sortie : **`app/build/outputs/apk/debug/app-debug.apk`** (signé avec la clé de debug).
Le module s'appelle `:app`.

### Version release signée (facultatif)

`assembleRelease` produit une release **non signée** tant qu'aucune configuration de
signature n'est ajoutée. Pour une distribution durable : créer un keystore hors dépôt,
le conserver en lieu sûr, définir les secrets GitHub (`ANDROID_KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), reconstituer le keystore dans un job
protégé, ajouter un `signingConfig`, puis appeler `:app:assembleRelease`.

**Ne jamais** committer la clé ni les mots de passe. Android exige **la même signature**
pour une mise à jour : la clé de debug d'un runner éphémère peut changer d'une exécution
à l'autre, donc deux builds debug successifs ne sont pas garantis installables l'un sur
l'autre (désinstaller puis installer fonctionne, mais efface favoris et playlists).

---

## 3. Fonctionnalités

### Bibliothèque

- Détection automatique via **MediaStore** : titres, artistes, albums, durées, dates
  d'ajout, tailles, noms de fichiers et pochettes exposées par Android.
- Valeurs de repli quand les métadonnées manquent (`Artiste inconnu`, nom de fichier).
- Observation des changements MediaStore (temporisation 600 ms), rafraîchissement à
  chaque retour au premier plan et bouton **Actualiser**.
- **Dossier facultatif** choisi avec le sélecteur Android (SAF), analysé
  récursivement, avec autorisation de lecture persistante.
- **Recherche** instantanée sur titre, artiste, album et nom de fichier, insensible à
  la casse **et aux accents**.
- **Tri** par titre, artiste, album, durée ou date d'ajout.
- Mode **liste avec pochettes** ou **liste compacte** (réglage persistant).
- État vide, état « aucun résultat » et messages d'erreur explicites.

### Lecture

- **ExoPlayer** réel via un `MediaSessionService` Media3 : aucune lecture simulée.
- Lecture, pause, reprise, morceau suivant / précédent, retour au début.
- **Barre de progression interactive** (glissement puis déplacement réel).
- Avancer / reculer de 10 secondes.
- **Aléatoire** et **répétition** (arrêtée → file → morceau).
- Lecture d'une liste entière, d'une playlist, d'une sélection filtrée, ou en aléatoire.
- **File d'attente** complète : saut direct, déplacement par flèches, retrait.
- Notification multimédia, écran verrouillé, casques filaires, Bluetooth et commandes
  système via Media3.
- Focus audio (appels, autres lecteurs) et pause à la déconnexion du casque.
- Vérification d'accès au fichier avant lecture ; erreur de codec affichée avec son code.

### Organisation

- **Favoris** (bascule immédiate, persistants).
- **Playlists** : création, renommage, suppression (avec confirmation), ajout, retrait,
  réorganisation par flèches.
- Références indisponibles affichées comme **« Fichier indisponible »** et retirables.
- **Historique** des 100 dernières références distinctes, effaçable avec confirmation.
- Accueil : morceau en cours, dernières écoutes, récemment ajoutés, favoris, playlists.

### Persistance locale

- **Room** : favoris, playlists et historique (aucun fichier audio copié).
- **DataStore** : tri, mode d'affichage, URI du dossier facultatif.
- Sauvegarde cloud et transfert d'appareil **désactivés**.

---

## 4. Identité visuelle et lisibilité

| Rôle | Couleur | Usage |
|------|---------|-------|
| Violet principal | `#B80DB8` | marque, boutons principaux, éléments actifs, mini-lecteur |
| Blanc | `#FFFFFF` | fonds, surfaces, zones de lecture |
| Jaune | `#F5D328` | accents, pastilles, indicateur de progression, bouton « Aléatoire » |

Contrastes mesurés (WCAG 2.1) :

| Association | Ratio | Niveau |
|-------------|-------|--------|
| violet `#B80DB8` / blanc | 5,5:1 | AA (texte normal) |
| texte `#1A1018` / blanc | 17,9:1 | AAA |
| texte secondaire `#5C4E5E` / blanc | 7,7:1 | AAA |
| jaune `#F5D328` / violet | 7,4:1 | AAA |
| texte `#1A1018` / jaune | 12,2:1 | AAA |

Règles appliquées dans toute l'interface :

- **Roboto embarquée** (Regular, Medium, Bold — licence OFL fournie), donc un rendu
  identique sur tous les fabricants.
- Hiérarchie typographique fixe : titre d'écran 20 sp, titre de morceau 17 sp,
  sous-titre 13,5 sp, légendes 12 sp. Aucun texte utile sous 13 sp.
- Texte secondaire jamais gris clair (`#5C4E5E`, 7,7:1).
- **Jamais** de blanc sur jaune, de jaune sur blanc, ni de violet sur violet.
- Cibles tactiles ≥ 48 dp, espacements verticaux de 8 à 26 dp.
- Tous les boutons ont un libellé visible ; chaque icône seule possède une description
  accessible (« Morceau suivant », « Retirer de la file », …).
- Aucun emoji, aucune illustration décorative : un jeu de **38 icônes vectorielles**
  dessiné pour le projet, trait uniforme de 2 px.

Les icônes et le logo sont générés par deux scripts versionnés :
`scripts/branding/generate_brand_assets.py` (icône adaptative, monochrome, logo, splash,
icône de notification, exports PNG) et `scripts/branding/generate_icons.py`
(jeu d'icônes de l'interface + planche de contrôle `docs/icones-systeme.png`).

---

## 5. Architecture

```text
app/src/main/java/com/pylive/music/
  PyliveApp.kt              Application : base Room et préférences (pas de DI externe)
  MainActivity.kt           point d'entrée Compose, splash, barres système
  domain/Track.kt           modèle Track, recherche, tri, file, formatage (pur, testé)
  library/LocalLibrary.kt   MediaStore, ContentObserver, dossier SAF, accès aux URI
  data/MusicDatabase.kt     Room : favoris, playlists, historique + DAO transactionnel
  data/Preferences.kt       DataStore : tri, mode compact, dossier facultatif
  playback/MusicService.kt  MediaSessionService ExoPlayer, notification, historique
  playback/PlaybackConnection.kt  MediaController, état temps réel, commandes
  ui/theme/Theme.kt         palette, typographie Roboto, formes
  ui/Components.kt          composants communs (lignes, boutons, états vides, bandeaux)
  ui/AppShell.kt            en-tête, bandeau d'autorisation, mini-lecteur, navigation
  ui/HomeScreen.kt          accueil
  ui/LibraryScreen.kt       bibliothèque, recherche, tri
  ui/PlayerScreen.kt        lecteur complet et file d'attente
  ui/CollectionScreens.kt   favoris, historique, playlists, détail de playlist
  ui/SettingsScreen.kt      réglages, confidentialité, dépannage
  ui/Dialogs.kt             dialogues et fiche de détails
  ui/PyliveRoot.kt          navigation, retour système, messages
app/src/test/               tests JVM des règles pures
app/src/androidTest/        tests Room, Media3 et analyse de bibliothèque
app/schemas/                schéma Room v1 exporté (généré à la compilation)
branding/                   logo SVG/PNG et icône Play Store
docs/                       documentation, aperçus graphiques
scripts/                    contrôles du dépôt et générateurs de ressources
```

Versions épinglées : JDK 17, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, KSP 2.1.20-1.0.32,
Compose BOM 2025.04.01, Material 3, Media3 1.6.1, Room 2.7.1, DataStore 1.1.4.

### Autorisations déclarées

| Permission | Rôle |
|------------|------|
| `READ_MEDIA_AUDIO` | lire les fichiers audio (Android 13+) |
| `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) | équivalent pour Android 8 → 12L |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | lecture en arrière-plan |
| `WAKE_LOCK` | lecture écran éteint |

Aucune permission Internet, aucune permission de notification généraliste (les
notifications multimédias en bénéficient par exemption Android 13), aucun
`MANAGE_EXTERNAL_STORAGE`.

### Base de données

Schéma **v1** versionné dans `app/schemas`. Ne jamais utiliser
`fallbackToDestructiveMigration`. Pour faire évoluer la base : augmenter `version`,
écrire une `Migration`, l'enregistrer avec `addMigrations(...)`, exporter le nouveau
schéma et le tester avec `MigrationTestHelper`.

---

## 6. Limites explicites

- La lecture réelle sur téléphone, les appels, le Bluetooth et les autorisations
  révoquées doivent être validés sur vos appareils (voir `docs/VERIFICATION.md`).
- Un arrêt forcé par Android interrompt la lecture. Pas de reprise automatique au
  démarrage du téléphone, pas de restauration de la file après destruction du service.
- Les fichiers privés des applications de streaming (Spotify, Deezer…) ne sont pas
  accessibles. Les formats MP3, M4A, AAC, FLAC, WAV, OGG/Opus dépendent du codec et du
  téléphone.
- Un fichier déplacé peut changer d'URI : il apparaît comme « Fichier indisponible ».
  Aucune correspondance automatique par empreinte audio.
- MediaStore et un dossier SAF peuvent exposer deux URI du même fichier : la
  déduplication se fait par URI.
- Pochettes issues de MediaStore uniquement ; pas d'extraction depuis un dossier SAF.
- Réorganisation par boutons accessibles (pas de glisser-déposer), pas d'égaliseur,
  pas de paroles — non demandés.
- La sauvegarde Android est désactivée : désinstaller efface favoris, playlists et
  réglages (jamais les fichiers audio).

---

## 7. Documentation complémentaire

- `docs/INVENTAIRE-FONCTIONNALITES.md` — inventaire du projet source et correspondance
  avec Pylive Music.
- `docs/VERIFICATION.md` — ce qui a été vérifié, comment, et ce qui reste à tester sur
  appareil.
- `docs/GUIDE-UTILISATION.md` — guide court pour la personne qui utilise l'application.

## Ressources tierces

- **Roboto** (Christian Robertson / Google) — SIL Open Font License 1.1, texte intégral
  dans `app/src/main/res/font/OFL-Roboto.txt`.
- Les icônes du lanceur, de l'interface et de la notification sont dessinées pour ce
  projet ; aucune ressource Material n'est copiée dans le dépôt.
- Bibliothèques AndroidX, Media3, Room, DataStore, Coil : Apache 2.0 (voir
  `NOTICE-APACHE-2.0.txt`).
