#!/usr/bin/env python3
"""
Contrôles structurels de Pylive Music, exécutables sans SDK Android.

Ils ne remplacent ni Gradle ni un test sur appareil : ils vérifient seulement
que le dépôt reste cohérent (identité, permissions, ressources, liens Kotlin).
Exécution : python3 scripts/check_project.py
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app" / "src" / "main"
PKG = "com.pylive.music"
errors: list[str] = []
checks = 0


def check(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        errors.append(message)


# 1. Fichiers indispensables -------------------------------------------------
required = [
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "settings.gradle.kts",
    "build.gradle.kts",
    "app/build.gradle.kts",
    ".github/workflows/android.yml",
    "app/src/main/AndroidManifest.xml",
    "README.md",
]
for name in required:
    check((ROOT / name).is_file(), f"fichier manquant : {name}")

check(zipfile.is_zipfile(ROOT / "gradle/wrapper/gradle-wrapper.jar"), "gradle-wrapper.jar invalide")
check(bool((ROOT / "gradlew").stat().st_mode & 0o111), "gradlew n'est pas exécutable")

# 2. Identité technique ------------------------------------------------------
gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
check('namespace = "com.pylive.music"' in gradle, "namespace incorrect")
check('applicationId = "com.pylive.music"' in gradle, "applicationId incorrect")
check("minSdk = 26" in gradle, "minSdk doit rester à 26 (Android 8.0)")
check("compileSdk = 35" in gradle, "compileSdk doit rester à 35")
check("com.tchatz" not in gradle, "référence résiduelle au projet d'origine dans Gradle")

manifest_raw = (APP / "AndroidManifest.xml").read_text(encoding="utf-8")
# Les commentaires sont retirés : ils citent les permissions volontairement absentes.
manifest = re.sub(r"<!--.*?-->", "", manifest_raw, flags=re.DOTALL)
check('android:label="@string/app_name"' in manifest, "libellé d'application non externalisé")
check("android:name=\".PyliveApp\"" in manifest, "classe Application déclarée absente")
check(".playback.MusicService" in manifest, "service de lecture absent du manifeste")
check('android:foregroundServiceType="mediaPlayback"' in manifest, "type de service de lecture absent")
check('android:exported="true"' in manifest, "activité exportée manquante")
check('android:allowBackup="false"' in manifest, "sauvegarde distante encore active")

# 3. Confidentialité : aucune permission réseau ------------------------------
for forbidden in ("android.permission.INTERNET", "ACCESS_NETWORK_STATE", "MANAGE_EXTERNAL_STORAGE",
                  "POST_NOTIFICATIONS", "RECORD_AUDIO", "CAMERA", "READ_PHONE_STATE"):
    check(forbidden not in manifest, f"permission interdite déclarée : {forbidden}")

check("READ_MEDIA_AUDIO" in manifest, "READ_MEDIA_AUDIO absent")
check('android:maxSdkVersion="32"' in manifest, "READ_EXTERNAL_STORAGE doit être limité à l'API 32")
check("FOREGROUND_SERVICE_MEDIA_PLAYBACK" in manifest, "permission de service de lecture absente")

strings = (APP / "res/values/strings.xml").read_text(encoding="utf-8")
check("<string name=\"app_name\">Pylive Music</string>" in strings, "nom d'application incorrect")

# 4. Palette imposée ---------------------------------------------------------
colors = (APP / "res/values/colors.xml").read_text(encoding="utf-8")
for value in ("#FFB80DB8", "#FFFFFFFF", "#FFF5D328"):
    check(value in colors, f"couleur imposée absente : {value}")

# 5. Ressources XML valides --------------------------------------------------
for xml_file in APP.rglob("*.xml"):
    try:
        ET.parse(xml_file)
    except ET.ParseError as exc:
        errors.append(f"XML invalide {xml_file.relative_to(ROOT)} : {exc}")
    checks += 1

# 6. Toutes les icônes référencées existent ----------------------------------
drawables = {p.stem for p in (APP / "res/drawable").glob("*.xml")}
mipmaps = {p.stem for p in (APP / "res").glob("mipmap-*/ic_launcher*.xml")}
mipmaps |= {p.stem for p in (APP / "res").glob("mipmap-*/ic_launcher*.png")}
for mipmap in ("ic_launcher", "ic_launcher_round"):
    check(mipmap in mipmaps, f"icône adaptative manquante : {mipmap}")

for kt in APP.rglob("*.kt"):
    text = kt.read_text(encoding="utf-8")
    for icon in re.findall(r"R\.drawable\.(\w+)", text):
        check(icon in drawables, f"drawable inexistant « {icon} » référencé dans {kt.name}")
    for font in re.findall(r"R\.font\.(\w+)", text):
        check((APP / "res/font" / f"{font}.ttf").is_file(),
              f"police inexistante « {font} » référencée dans {kt.name}")
    for string in re.findall(r"R\.string\.(\w+)", text):
        check(f'name="{string}"' in strings, f"chaîne inexistante « {string} » dans {kt.name}")

# 7. Cohérence des paquets et des imports internes ---------------------------
sources = list(APP.rglob("*.kt"))
for kt in sources:
    text = kt.read_text(encoding="utf-8")
    check("package com.pylive.music" in text, f"paquet inattendu dans {kt.relative_to(APP)}")
    check("com.tchatz" not in text, f"référence au projet d'origine dans {kt.name}")

declared = set()
for kt in sources:
    text = kt.read_text(encoding="utf-8")
    for name in re.findall(
        r"^(?:abstract |open |sealed |data |enum |public |private |internal )*"
        r"(?:class|object|interface) (\w+)", text, flags=re.MULTILINE
    ):
        declared.add(name)
    # Les fonctions et propriétés de haut niveau s'importent aussi.
    for name in re.findall(r"^(?:public |internal |private )?(?:fun|val|var) (\w+)",
                           text, flags=re.MULTILINE):
        declared.add(name)
    for name in re.findall(r"^(?:public |internal |private )?(?:fun|val|var) <[^>]+> (\w+)",
                           text, flags=re.MULTILINE):
        declared.add(name)
for kt in sources:
    text = kt.read_text(encoding="utf-8")
    for imported in re.findall(r"^import com\.pylive\.music\.[\w.]*\.(\w+)$", text, flags=re.MULTILINE):
        check(imported in declared, f"symbole interne introuvable « {imported} » importé dans {kt.name}")

# 8. Manifeste : composants déclarés présents dans le code -------------------
check((APP / "java/com/pylive/music/PyliveApp.kt").is_file(), "PyliveApp.kt absent")
check((APP / "java/com/pylive/music/MainActivity.kt").is_file(), "MainActivity.kt absent")
check((APP / "java/com/pylive/music/playback/MusicService.kt").is_file(), "MusicService.kt absent")

# 9. Schéma Room exporté -----------------------------------------------------
schemas = list((ROOT / "app/schemas").glob("*/1.json"))
if not schemas:
    # Le schéma est produit par KSP lors de la première compilation ; son absence
    # n'empêche pas de compiler, on l'annonce donc sans faire échouer le contrôle.
    print("note : schéma Room v1 pas encore généré (produit par la première compilation).")
if schemas:
    schema = schemas[0].read_text(encoding="utf-8")
    for table in ("favorite", "playlist", "playlist_track", "recent"):
        check(f'"{table}"' in schema, f"table Room absente du schéma : {table}")

# 10. Workflow CI ------------------------------------------------------------
workflow_path = ROOT / ".github/workflows/android.yml"
check(workflow_path.is_file(), "workflow CI absent")
if workflow_path.is_file():
    workflow = workflow_path.read_text(encoding="utf-8")
    for step in (":app:testDebugUnitTest", ":app:lintDebug", ":app:assembleDebug",
                 "app/build/outputs/apk/debug/app-debug.apk", "workflow_dispatch"):
        check(step in workflow, f"étape CI manquante : {step}")

# 11. Interdits d'interface : texte blanc sur jaune ou jaune sur blanc --------
theme = (APP / "java/com/pylive/music/ui/theme/Theme.kt").read_text(encoding="utf-8")
check("onSecondaryContainer = PylivePalette.Ink" in theme,
      "le conteneur jaune doit imposer un texte foncé")

# 12. Pièges Kotlin : extensions Compose appelées avec leur nom complet ------
# `androidx.compose.foundation.clickable(…)` ne compile pas : une fonction
# d'extension doit être importée puis appelée sur son récepteur.
extension_pattern = re.compile(
    r"(?<![\w.])androidx\.compose\.(?:foundation|ui)\."
    r"(?:clickable|background|border|padding|fillMaxWidth|fillMaxSize|height|width|size|"
    r"weight|offset|clip|alpha|rotate|aspectRatio|widthIn|testTag)\s*\("
)
for kt in sources:
    text = kt.read_text(encoding="utf-8")
    check(not extension_pattern.search(text),
          f"extension Compose appelée avec son nom complet dans {kt.name} (import requis)")

# 13. Les fichiers UI qui utilisent `by remember` importent getValue/setValue --
for kt in sources:
    text = kt.read_text(encoding="utf-8")
    if re.search(r"\bby remember", text):
        check("import androidx.compose.runtime.getValue" in text,
              f"getValue manquant pour `by remember` dans {kt.name}")
        check("import androidx.compose.runtime.setValue" in text,
              f"setValue manquant pour `var … by remember` dans {kt.name}")

# 14. BuildConfig doit être activé si le code l'utilise -----------------------
for kt in sources:
    if "BuildConfig." in kt.read_text(encoding="utf-8"):
        check("buildConfig = true" in gradle, "buildConfig non activé alors que BuildConfig est utilisé")
        break

# 15. Fonctionnalités annoncées réellement présentes -------------------------
expected_files = [
    "java/com/pylive/music/library/LocalLibrary.kt",
    "java/com/pylive/music/playback/PlaybackConnection.kt",
    "java/com/pylive/music/data/MusicDatabase.kt",
    "java/com/pylive/music/data/Preferences.kt",
    "java/com/pylive/music/ui/PlayerScreen.kt",
    "java/com/pylive/music/ui/PyliveRoot.kt",
]
for name in expected_files:
    check((APP / name).is_file(), f"fonctionnalité attendue absente : {name}")

# ---------------------------------------------------------------------------
if errors:
    print(f"{len(errors)} problème(s) détecté(s) sur {checks} contrôles :")
    for message in errors:
        print(f"  - {message}")
    sys.exit(1)

print(f"OK — {checks} contrôles réussis : identité, confidentialité, palette, ressources, "
      f"icônes, imports internes, schéma Room et workflow CI.")
