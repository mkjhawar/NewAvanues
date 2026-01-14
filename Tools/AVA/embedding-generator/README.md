# AVA Intent Embedding Generator

Pre-computes intent embeddings at build time for instant loading on device.
Eliminates 10+ minute first-launch delay by bundling embeddings in APK.

## Supported File Formats

| Extension | Format | Schema | Description |
|-----------|--------|--------|-------------|
| `.ava` | Universal Format | `avu-1.0` | VCM text format (AVA native) |
| `.ava` | AVA JSON | `ava-1.0` | Abbreviated JSON format |
| `.vos` | Universal Format | `avu-1.0` | VCM text format (VoiceOS) |
| `.vos` | VoiceOS JSON | `vos-1.0` | VoiceOS JSON with commands array |

## Setup

```bash
cd tools/embedding-generator
pip install -r requirements.txt
```

## Usage

### From .ava/.vos files (recommended)

```bash
python generate_embeddings.py \
    --model ../../android/ava/src/main/assets/models/AVA-384-Base-INT8.AON \
    --vocab ../../android/ava/src/main/assets/models/vocab.txt \
    --ava-dir ../../android/ava/src/main/assets/ava-examples \
    --ava-core-dir ../../.ava/core \
    --output-sql ../../common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/PrecomputedEmbeddings.sq \
    --output-aot ../../android/ava/src/main/assets/embeddings/bundled_embeddings.aot \
    --model-version AVA-384-Base-INT8 \
    --locale en-US
```

### From JSON file (legacy)

```bash
python generate_embeddings.py \
    --model ../../android/ava/src/main/assets/models/AVA-384-Base-INT8.AON \
    --vocab ../../android/ava/src/main/assets/models/vocab.txt \
    --intents intents.json \
    --output-sql ../../common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/PrecomputedEmbeddings.sq \
    --output-aot ../../android/ava/src/main/assets/embeddings/bundled_embeddings.aot
```

## File Formats

### Universal Format (avu-1.0) - Recommended
Used by both `.ava` and `.vos` files:
```
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: device-settings.ava
  category: system
---
NLU:wifi_on:turn on wifi
NLU:wifi_on:enable wifi
NLU:show_time:what time is it
LLM:create_workflow:when I get home turn on lights
HYB:control_thermostat:set temperature to 72
```

### Intent Type Prefixes
| Prefix | Description | Processing |
|--------|-------------|------------|
| NLU | Direct action intent | NLU-only, no LLM needed |
| LLM | Requires LLM reasoning | Routed to LLM |
| HYB | Hybrid processing | NLU classifies, LLM extracts params |
| VCM | Voice command (legacy) | Same as NLU |
| INFO | Information request | Same as NLU |

### AVA JSON format (ava-1.0)
Abbreviated field names:
```json
{
  "s": "ava-1.0",
  "l": "en-US",
  "i": [
    {
      "id": "show_time",
      "c": "show time",
      "s": ["what time is it", "tell me the time"]
    }
  ]
}
```

### VoiceOS JSON format (vos-1.0)
Full field names with commands array:
```json
{
  "schema": "vos-1.0",
  "locale": "en-US",
  "commands": [
    {
      "action": "TURN_ON_WIFI",
      "cmd": "turn on wifi",
      "syn": ["wifi on", "enable wifi", "activate wifi"]
    }
  ]
}
```

## Outputs

| File | Purpose |
|------|---------|
| `PrecomputedEmbeddings.sq` | SQLDelight migration with INSERT statements |
| `bundled_embeddings.aot` | Binary backup for DB corruption recovery |

## Integration

1. Run this script during build or before release
2. The SQL file is executed as a SQLDelight migration on first launch
3. Embeddings are pre-populated in database - no runtime computation needed
4. The .aot file is a backup if database becomes corrupted

## Architecture

```
Build Time:
  .ava files → generate_embeddings.py → SQL + .aot

App Launch:
  SQLDelight migration → Database populated with embeddings
  (No runtime embedding computation needed)

Runtime:
  User query → Tokenize → Compute query embedding → Compare with DB embeddings
```

## Intent File Structure

```
.ava/
└── core/
    └── en-US/
        ├── communication.ava     # Calls, texts, emails (10 intents)
        ├── maps-navigation.ava   # Directions, POI (10 intents)
        ├── calendar.ava          # Events, scheduling (8 intents)
        ├── timers.ava            # Timers, alarms (6 intents)
        ├── lists-notes.ava       # Todo, notes, shopping (8 intents)
        ├── device-settings.ava   # Android settings (20 intents)
        ├── media-control.ava     # Music, video playback (10 intents)
        ├── smart-home.ava        # Lights, thermostat (12 intents)
        ├── camera-photos.ava     # Camera, gallery (8 intents)
        ├── health-fitness.ava    # Workouts, tracking (8 intents)
        ├── finance.ava           # Banking, stocks (6 intents)
        ├── travel.ava            # Rides, flights (8 intents)
        ├── entertainment.ava     # Jokes, trivia, games (12 intents)
        ├── search-info.ava       # Web search, knowledge (10 intents)
        ├── translation-conversion.ava  # Units, currency (6 intents)
        ├── accessibility.ava     # A11y features (6 intents)
        ├── app-control.ava       # App management (10 intents)
        ├── routines.ava          # Morning/night routines (8 intents)
        ├── workflows.ava         # Automation (LLM) (6 intents)
        ├── magicui.ava           # UI generation (LLM) (12 intents)
        ├── magiccode.ava         # Code generation (LLM) (15 intents)
        └── avanues-plugin.ava    # Plugin dev (LLM) (12 intents)
```

## Prompt Templates (.avp)

LLM-required intents use prompt templates in `.ava/prompts/{locale}/`:

| File | Intent | Description |
|------|--------|-------------|
| `create_workflow.avp` | create_workflow | Automation workflow creation |
| `create_ui_component.avp` | create_ui_component | MagicUI component generation |
| `create_ui_screen.avp` | create_ui_screen | Full screen layout generation |
| `generate_code.avp` | generate_code | Kotlin/Swift code generation |
| `create_plugin.avp` | create_plugin | Avanues plugin development |
| `answer_question.avp` | answer_question | General knowledge responses |
| `compose_message.avp` | compose_message | Email/text composition |
| `summarize_content.avp` | summarize_content | Article/document summaries |

## Author

Manoj Jhawar
