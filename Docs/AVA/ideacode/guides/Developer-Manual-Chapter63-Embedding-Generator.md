# Chapter 63: Intent Embedding Generator

## Overview

The Intent Embedding Generator is a build-time tool that pre-computes semantic embeddings for all bundled intents. This eliminates first-launch delays by bundling embeddings directly in the APK.

## Quick Start

```bash
cd tools/embedding-generator
./generate.sh
```

This generates embeddings for all intents in:
- `android/ava/src/main/assets/ava-examples/`
- `.ava/core/`

## Output Files

| File | Purpose | Location |
|------|---------|----------|
| `PrecomputedEmbeddings.sq` | SQLDelight migration | `common/core/Data/src/main/sqldelight/.../` |
| `bundled_embeddings.aot` | Binary backup | `android/ava/src/main/assets/embeddings/` |

## Usage

### Basic Usage

```bash
./generate.sh
```

### Custom Paths

```bash
./generate.sh \
    --model /path/to/model.AON \
    --vocab /path/to/vocab.txt \
    --ava-dir /path/to/ava-files \
    --locale es-ES
```

### Available Options

| Option | Description | Default |
|--------|-------------|---------|
| `--model` | ONNX/AON model path | `assets/models/AVA-384-Base-INT8.AON` |
| `--vocab` | Vocabulary file | `assets/models/vocab.txt` |
| `--ava-dir` | .ava files directory | `assets/ava-examples` |
| `--ava-core` | Core .ava directory | `.ava/core` |
| `--output-sql` | Output SQL file | `PrecomputedEmbeddings.sq` |
| `--output-aot` | Output .aot file | `bundled_embeddings.aot` |
| `--locale` | Locale for embeddings | `en-US` |
| `--model-version` | Model version string | `AVA-384-Base-INT8` |

## Adding New Intents

### 1. Create or Edit .ava File

Add intents to an existing `.ava` file or create a new one:

```
---
schema: avu-1.0
version: 1.0.0
locale: en-US
---
VCM:my_new_intent:example phrase one
VCM:my_new_intent:another example phrase
VCM:my_new_intent:third variation
INFO:info_intent:what is the weather
INFO:info_intent:tell me the weather
---
```

### 2. Regenerate Embeddings

```bash
cd tools/embedding-generator
./generate.sh
```

### 3. Rebuild APK

```bash
./gradlew assembleDebug
```

## Intent Types

| Type | Description | Example |
|------|-------------|---------|
| `VCM` | Voice Command Manager (routes to VoiceOS) | `VCM:wifi_on:turn on wifi` |
| `INFO` | Information queries | `INFO:show_time:what time is it` |
| `SYS` | System intents | `SYS:show_history:show my history` |
| `LEARN` | User-taught intents | `LEARN:custom:my phrase` |

## Supported File Formats

The embedding generator reads both `.ava` and `.vos` files with automatic format detection.

| Extension | Format | Schema | Description |
|-----------|--------|--------|-------------|
| `.ava` | Universal Format | `avu-1.0` | VCM text format (AVA native) |
| `.ava` | AVA JSON | `ava-1.0` | Abbreviated JSON format |
| `.vos` | Universal Format | `avu-1.0` | VCM text format (VoiceOS) |
| `.vos` | VoiceOS JSON | `vos-1.0` | VoiceOS JSON with commands array |

### Universal Format (Recommended)

```
---
schema: avu-1.0
version: 1.0.0
locale: en-US
---
TYPE:intent_id:example text
TYPE:intent_id:another example
---
# Optional synonyms section
```

### AVA JSON Format (ava-1.0)

```json
{
  "s": "ava-1.0",
  "i": [
    {
      "id": "intent_id",
      "c": "canonical form",
      "s": ["synonym 1", "synonym 2"]
    }
  ]
}
```

### VoiceOS JSON Format (vos-1.0)

```json
{
  "schema": "vos-1.0",
  "locale": "en-US",
  "commands": [
    {
      "action": "TURN_ON_WIFI",
      "cmd": "turn on wifi",
      "syn": ["wifi on", "enable wifi"]
    }
  ]
}
```

**Note:** When parsing vos-1.0 files, action names like `TURN_ON_WIFI` are automatically converted to lowercase intent IDs (`turn_on_wifi`).

## Current Coverage

As of v1.4.0, the generator produces embeddings for 126 intents:

| Category | Count | Examples |
|----------|-------|----------|
| VoiceOS Commands | 94 | cursor control, dictation, keyboard, volume levels |
| System Control | 12 | wifi, bluetooth, brightness, flashlight |
| Navigation | 8 | open_app, go_home, open_browser |
| Media Control | 10 | play_music, pause, next_track, shuffle |
| Productivity | 2 | set_alarm, set_reminder |
| Information | 3 | check_weather, show_time, search_web |

## Multi-Locale Support

Generate embeddings for different locales:

```bash
# Spanish
./generate.sh --locale es-ES --ava-dir assets/ava-examples/es-ES

# German
./generate.sh --locale de-DE --ava-dir assets/ava-examples/de-DE
```

Each locale generates separate embeddings with its own SQL migration.

## Troubleshooting

### Missing Dependencies

```bash
cd tools/embedding-generator
pip install -r requirements.txt
```

### Model Not Found

Ensure the NLU model exists:
```bash
ls android/ava/src/main/assets/models/AVA-384-Base-INT8.AON
```

### No Intents Parsed

Check .ava file format:
- Must have `---` delimiters
- Intent lines must be `TYPE:id:example`
- TYPE must be uppercase (VCM, INFO, SYS)

### Verify Output

```bash
# Check SQL contains INSERT statements
head -30 common/core/Data/src/main/sqldelight/.../PrecomputedEmbeddings.sq

# Check .aot file size (should be >100KB for full coverage)
ls -la android/ava/src/main/assets/embeddings/bundled_embeddings.aot
```

## Build Integration

### Pre-Release Checklist

1. Run embedding generator
2. Verify output file sizes
3. Build debug APK
4. Test intent classification
5. Commit generated files

### CI/CD Integration

Add to build pipeline:

```yaml
- name: Generate Embeddings
  run: |
    cd tools/embedding-generator
    pip install -r requirements.txt
    ./generate.sh
```

## Architecture

```
Build Time:
  .ava/.vos files --> generate_embeddings.py --> SQL + .aot

App Launch:
  SQLDelight migration --> Database populated with embeddings
  (No runtime embedding computation needed)

Runtime:
  User query --> Tokenize --> Compute query embedding --> Compare with DB
```

### VoiceOS Integration

AVA can now read VoiceOS `.vos` files directly, enabling:
- **Single source of truth:** VoiceOS commands shared without conversion
- **Automatic embedding generation:** Run `./generate.sh` on VoiceOS directories
- **Cross-project compatibility:** Same format works in both AVA and VoiceOS

## Performance Impact

| Metric | Without Pre-computed | With Pre-computed |
|--------|---------------------|-------------------|
| First launch delay | 10+ minutes | <1 second |
| APK size increase | 0 | ~200 KB |
| Battery usage | High (GPU compute) | Minimal |

---

*Chapter Version: 1.0*
*Last Updated: December 2, 2025*
