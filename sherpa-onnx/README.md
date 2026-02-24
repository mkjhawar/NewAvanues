# Sherpa-ONNX for AVX Engine

The AVX (AvaVox) command engine uses Sherpa-ONNX for on-device ONNX transducer inference.

## Download

Download from GitHub Releases:
https://github.com/k2-fsa/sherpa-onnx/releases

Target version: **v1.12.25** or later (requires hot words support via modified_beam_search)

## Android Setup

1. Download `sherpa-onnx-{version}.aar` from the release
2. Place in this directory as: `sherpa-onnx.aar`

The AAR contains:
- Native libraries (`libsherpa-onnx-jni.so`) for arm64-v8a, armeabi-v7a, x86, x86_64
- Kotlin/Java API: `OnlineRecognizer`, `OnlineStream`, `OfflineRecognizer`
- ONNX Runtime bundled

## Desktop JVM Setup

Desktop requires two artifacts extracted from the AAR:

### 1. API Classes (compile-time)

Extract the classes JAR from the AAR:
```bash
unzip sherpa-onnx.aar classes.jar
mv classes.jar sherpa-onnx-classes.jar
```

Place `sherpa-onnx-classes.jar` in this directory. The Desktop build uses this for compilation.

### 2. Native Libraries (runtime)

Download platform-specific native libraries from the release:

| Platform | Library | Download |
|---|---|---|
| macOS (Apple Silicon) | `libsherpa-onnx-jni.dylib` | `sherpa-onnx-v{ver}-osx-arm64.tar.bz2` |
| macOS (Intel) | `libsherpa-onnx-jni.dylib` | `sherpa-onnx-v{ver}-osx-x86_64.tar.bz2` |
| Linux (x64) | `libsherpa-onnx-jni.so` | `sherpa-onnx-v{ver}-linux-x86_64.tar.bz2` |
| Windows (x64) | `sherpa-onnx-jni.dll` | `sherpa-onnx-v{ver}-win-x64.tar.bz2` |

Place the native library in one of these search paths:
- `java.library.path` (JVM flag: `-Djava.library.path=/path/to/lib`)
- `{working_dir}/lib/`
- `{working_dir}/natives/`
- `~/.avanues/lib/`

## Requirements for Hot Words

Hot words boosting ONLY works with:
- Transducer models (encoder + decoder + joiner ONNX files)
- `decodingMethod = "modified_beam_search"`
- NOT paraformer, NOT CTC models

## Files in This Directory

| File | Purpose | Required By |
|---|---|---|
| `sherpa-onnx.aar` | Android AAR (compileOnly) | Android build |
| `sherpa-onnx-classes.jar` | Kotlin API classes | Desktop build |
| `README.md` | This file | — |
