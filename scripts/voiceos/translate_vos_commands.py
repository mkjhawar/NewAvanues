#!/usr/bin/env python3
"""
VOS Command Translation Helper

Translates VOS command files from English to German, Spanish, and French.

Usage:
    python3 translate_vos_commands.py --language de|es|fr [--file FILENAME]
    python3 translate_vos_commands.py --all  # Translate all languages
"""

import json
import os
import sys
from pathlib import Path
from typing import Dict, List, Any
from datetime import datetime

# Base paths
VOS_EN_DIR = Path("modules/managers/CommandManager/src/main/assets/commands/en-VOS")
VOS_DE_DIR = Path("modules/managers/CommandManager/src/main/assets/commands/de-VOS")
VOS_ES_DIR = Path("modules/managers/CommandManager/src/main/assets/commands/es-VOS")
VOS_FR_DIR = Path("modules/managers/CommandManager/src/main/assets/commands/fr-VOS")

# Translation dictionaries for common voice command terms
TRANSLATIONS = {
    "de": {  # German
        # Actions
        "turn on": "einschalten",
        "turn off": "ausschalten",
        "enable": "aktivieren",
        "disable": "deaktivieren",
        "open": "öffnen",
        "close": "schließen",
        "show": "zeigen",
        "hide": "verstecken",
        "start": "starten",
        "stop": "stoppen",
        "increase": "erhöhen",
        "decrease": "verringern",
        "navigate": "navigieren",
        "go": "gehen",
        "back": "zurück",
        "forward": "vorwärts",
        "up": "hoch",
        "down": "runter",
        "left": "links",
        "right": "rechts",
        "click": "klicken",
        "select": "auswählen",
        "scroll": "scrollen",
        "swipe": "wischen",
        "drag": "ziehen",
        "press": "drücken",
        "hold": "halten",

        # Objects
        "bluetooth": "Bluetooth",
        "wifi": "WLAN",
        "airplane mode": "Flugmodus",
        "mobile data": "Mobile Daten",
        "cursor": "Cursor",
        "menu": "Menü",
        "home": "Startseite",
        "settings": "Einstellungen",
        "volume": "Lautstärke",
        "brightness": "Helligkeit",
        "screen": "Bildschirm",
        "keyboard": "Tastatur",
        "notification": "Benachrichtigung",
        "overlay": "Overlay",
        "dialog": "Dialog",
        "help": "Hilfe",

        # Common phrases
        "please": "bitte",
        "now": "jetzt",
        "all": "alles",
        "recent": "kürzlich",
        "next": "nächste",
        "previous": "vorherige",
        "first": "erste",
        "last": "letzte",

        # Locale metadata
        "locale_code": "de-DE",
        "locale_name": "Deutsch (Deutschland)"
    },

    "es": {  # Spanish
        # Actions
        "turn on": "activar",
        "turn off": "desactivar",
        "enable": "habilitar",
        "disable": "deshabilitar",
        "open": "abrir",
        "close": "cerrar",
        "show": "mostrar",
        "hide": "ocultar",
        "start": "iniciar",
        "stop": "detener",
        "increase": "aumentar",
        "decrease": "disminuir",
        "navigate": "navegar",
        "go": "ir",
        "back": "atrás",
        "forward": "adelante",
        "up": "arriba",
        "down": "abajo",
        "left": "izquierda",
        "right": "derecha",
        "click": "hacer clic",
        "select": "seleccionar",
        "scroll": "desplazar",
        "swipe": "deslizar",
        "drag": "arrastrar",
        "press": "presionar",
        "hold": "mantener",

        # Objects
        "bluetooth": "Bluetooth",
        "wifi": "WiFi",
        "airplane mode": "modo avión",
        "mobile data": "datos móviles",
        "cursor": "cursor",
        "menu": "menú",
        "home": "inicio",
        "settings": "configuración",
        "volume": "volumen",
        "brightness": "brillo",
        "screen": "pantalla",
        "keyboard": "teclado",
        "notification": "notificación",
        "overlay": "superposición",
        "dialog": "diálogo",
        "help": "ayuda",

        # Common phrases
        "please": "por favor",
        "now": "ahora",
        "all": "todo",
        "recent": "reciente",
        "next": "siguiente",
        "previous": "anterior",
        "first": "primero",
        "last": "último",

        # Locale metadata
        "locale_code": "es-ES",
        "locale_name": "Español (España)"
    },

    "fr": {  # French
        # Actions
        "turn on": "activer",
        "turn off": "désactiver",
        "enable": "activer",
        "disable": "désactiver",
        "open": "ouvrir",
        "close": "fermer",
        "show": "afficher",
        "hide": "masquer",
        "start": "démarrer",
        "stop": "arrêter",
        "increase": "augmenter",
        "decrease": "diminuer",
        "navigate": "naviguer",
        "go": "aller",
        "back": "retour",
        "forward": "avancer",
        "up": "haut",
        "down": "bas",
        "left": "gauche",
        "right": "droite",
        "click": "cliquer",
        "select": "sélectionner",
        "scroll": "défiler",
        "swipe": "glisser",
        "drag": "faire glisser",
        "press": "appuyer",
        "hold": "maintenir",

        # Objects
        "bluetooth": "Bluetooth",
        "wifi": "WiFi",
        "airplane mode": "mode avion",
        "mobile data": "données mobiles",
        "cursor": "curseur",
        "menu": "menu",
        "home": "accueil",
        "settings": "paramètres",
        "volume": "volume",
        "brightness": "luminosité",
        "screen": "écran",
        "keyboard": "clavier",
        "notification": "notification",
        "overlay": "superposition",
        "dialog": "dialogue",
        "help": "aide",

        # Common phrases
        "please": "s'il vous plaît",
        "now": "maintenant",
        "all": "tout",
        "recent": "récent",
        "next": "suivant",
        "previous": "précédent",
        "first": "premier",
        "last": "dernier",

        # Locale metadata
        "locale_code": "fr-FR",
        "locale_name": "Français (France)"
    }
}


def translate_phrase(phrase: str, lang: str) -> str:
    """
    Translate a phrase using the translation dictionary.

    Args:
        phrase: English phrase to translate
        lang: Target language code (de, es, fr)

    Returns:
        Translated phrase (or original if no translation found)
    """
    if lang not in TRANSLATIONS:
        return phrase

    translations = TRANSLATIONS[lang]
    phrase_lower = phrase.lower()

    # Try exact match first
    if phrase_lower in translations:
        return translations[phrase_lower]

    # Try word-by-word translation for compound phrases
    words = phrase_lower.split()
    translated_words = []

    for word in words:
        if word in translations:
            translated_words.append(translations[word])
        else:
            translated_words.append(word)

    return " ".join(translated_words)


def translate_command(command: Dict[str, Any], lang: str) -> Dict[str, Any]:
    """
    Translate a single VOS command to target language.

    Args:
        command: VOS command dict with action, cmd, syn
        lang: Target language code (de, es, fr)

    Returns:
        Translated command dict
    """
    translated_cmd = command.copy()

    # Translate primary command
    translated_cmd["cmd"] = translate_phrase(command["cmd"], lang)

    # Translate all synonyms
    translated_synonyms = []
    for synonym in command["syn"]:
        translated_synonyms.append(translate_phrase(synonym, lang))

    translated_cmd["syn"] = translated_synonyms

    return translated_cmd


def translate_vos_file(en_file_path: Path, target_file_path: Path, lang: str):
    """
    Translate entire VOS file to target language.

    Args:
        en_file_path: Path to English VOS file
        target_file_path: Path to output translated VOS file
        lang: Target language code (de, es, fr)
    """
    # Read English VOS file
    with open(en_file_path, 'r', encoding='utf-8') as f:
        en_data = json.load(f)

    # Update metadata
    en_data["locale"] = TRANSLATIONS[lang]["locale_code"]

    # Translate all commands
    translated_commands = []
    for cmd in en_data["commands"]:
        translated_cmd = translate_command(cmd, lang)
        translated_commands.append(translated_cmd)

    en_data["commands"] = translated_commands

    # Save translated file
    with open(target_file_path, 'w', encoding='utf-8') as f:
        json.dump(en_data, f, indent=2, ensure_ascii=False)

    print(f"✓ Translated {en_file_path.name} -> {target_file_path}")


def translate_all_files(lang: str):
    """
    Translate all VOS files for a language.

    Args:
        lang: Target language code (de, es, fr)
    """
    # Determine target directory
    target_dir = {
        "de": VOS_DE_DIR,
        "es": VOS_ES_DIR,
        "fr": VOS_FR_DIR
    }[lang]

    lang_name = TRANSLATIONS[lang]["locale_name"]
    print(f"\nTranslating to {lang_name} ({lang.upper()})...")
    print("-" * 60)

    # Get all English VOS files
    en_files = sorted(VOS_EN_DIR.glob("*.vos"))

    for en_file in en_files:
        target_file = target_dir / en_file.name
        translate_vos_file(en_file, target_file, lang)

    print(f"\n✓ Completed translation for {lang_name}")
    print(f"  Files translated: {len(en_files)}")


def main():
    """Main translation function."""
    print("=" * 60)
    print("VOS Command Translation Helper")
    print("=" * 60)
    print()

    # Translate all languages
    for lang in ["de", "es", "fr"]:
        translate_all_files(lang)

    print("\n" + "=" * 60)
    print("Translation Complete!")
    print("=" * 60)
    print(f"\nTranslated 3 languages (German, Spanish, French)")
    print(f"Total VOS files per language: 19")
    print("\n⚠️  NOTE: Machine translations may need manual review")
    print("   Please verify technical terms and natural phrasing")


if __name__ == "__main__":
    # Change to VoiceOS directory
    os.chdir("/Volumes/M-Drive/Coding/VoiceOS")
    main()
