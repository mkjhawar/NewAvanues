# Developer Manual - Chapter 62: AVA Toolchain Build System

**Version:** 1.0
**Date:** 2025-12-01
**Author:** AVA Development Team
**Status:** Implementation Complete

---

## Overview

This chapter documents the build system for creating distributable AVA AI Model Toolchain applications for macOS and Linux platforms.

---

## Architecture

| Component | Platform | Technology | Output |
|-----------|----------|------------|--------|
| macOS App | macOS 13+ | Swift/SwiftUI | .app, .dmg |
| Linux App | Ubuntu/Fedora | Python/PyQt6 | executable, .deb, .AppImage |
| Python Tools | Cross-platform | Python 3.10+ | CLI scripts |

---

## Directory Structure

```
scripts/tools/
├── macos-converter/
│   ├── Package.swift              # Swift Package Manager
│   ├── build-macos.sh             # Build script
│   └── AVAFormatConverter/
│       ├── AVAFormatConverterApp.swift
│       ├── Views/
│       │   └── ContentView.swift  # Main UI
│       ├── Models/
│       │   └── AvaFile.swift
│       └── Utilities/
│           ├── AvaConverter.swift
│           └── AVA3Encoder.swift
├── gui-app/
│   ├── main.py                    # Entry point
│   ├── AVAToolchain.spec          # PyInstaller spec
│   ├── build-linux.sh             # Build script
│   ├── requirements.txt
│   └── ava_toolchain_gui/
│       ├── main_window.py
│       ├── splash_screen.py
│       └── tabs/
└── python-tools/
    └── ava_toolchain/
        ├── alm_processor.py
        ├── ava3_encoder.py
        └── ...
```

---

## Building macOS Application

### Prerequisites

```bash
# Xcode Command Line Tools
xcode-select --install

# Swift 5.9+
swift --version
```

### Build Commands

```bash
cd scripts/tools/macos-converter

# Build .app only
./build-macos.sh

# Build .app and create .dmg installer
./build-macos.sh --dmg

# Clean build artifacts
./build-macos.sh --clean
```

### Output Files

| File | Location | Description |
|------|----------|-------------|
| AVAToolchain.app | `build/` | Application bundle |
| AVAToolchain-1.0.0.dmg | `release/` | Installer with Applications link |
| AVAToolchain-1.0.0.dmg.sha256 | `release/` | SHA256 checksum |

### Supported File Associations

| Extension | Type | Description |
|-----------|------|-------------|
| .ava | Editor | Intent files |
| .aon | Editor | AON NLU models |
| .amm | Viewer | MLC-LLM models |
| .amg | Viewer | GGUF models |
| .amr | Viewer | LiteRT models |

---

## Building Linux Application

### Prerequisites

```bash
# Ubuntu/Debian
sudo apt install python3.10 python3-pip python3-pyqt6

# Install Python requirements
pip install -r requirements.txt
pip install pyinstaller

# For .deb packaging
sudo gem install fpm
```

### Build Commands

```bash
cd scripts/tools/gui-app

# Build executable only
./build-linux.sh

# Build and create .deb package
./build-linux.sh --deb

# Build and create AppImage
./build-linux.sh --appimage

# Build all formats
./build-linux.sh --all

# Clean build artifacts
./build-linux.sh --clean
```

### Output Files

| File | Location | Description |
|------|----------|-------------|
| AVAToolchain | `dist/AVAToolchain/` | Linux executable |
| ava-toolchain_1.0.0_amd64.deb | `release/` | Debian package |
| AVAToolchain-1.0.0-x86_64.AppImage | `release/` | Portable AppImage |

---

## Extension Scheme v2.0 Support

Both macOS and Linux apps fully support the AVA 3-character extension scheme:

### Model Files

| Extension | Format | Runtime |
|-----------|--------|---------|
| .amm | Ava Model MLC | MLC-LLM (TVM) |
| .amg | Ava Model GGUF | llama.cpp |
| .amr | Ava Model liteRT | Google AI Edge |

### Device Libraries

| Extension | Format | Description |
|-----------|--------|-------------|
| .adt | Ava Device TVM | TVM compiled library |
| .adm | Ava Device MLC | MLC-LLM device library |
| .adg | Ava Device GGUF | GGUF native library |
| .adr | Ava Device liteRT | LiteRT delegate |

### Tokenizers

| Extension | Format | Description |
|-----------|--------|-------------|
| .ats | Ava Tokenizer SP | SentencePiece |
| .ath | Ava Tokenizer HF | HuggingFace |
| .atv | Ava Tokenizer Vocab | Vocabulary file |

---

## Application Features

### Tabs

| Tab | Function |
|-----|----------|
| Intent Files | Convert .ava v1.0 → v2.0 |
| AON Compiler | Compile embedding models |
| TVM Compiler | Cross-compile to TVM libraries |
| LLM Processor | Process models to .amm/.amg/.amr |
| Profiler | Benchmark model performance |

### Menu Structure

```
File
├── New Project...
├── Open Project...
├── Import .ava Files...
├── Import AON Model...
├── Import ALM Model...
├── Export Build Report...
└── Quit

Models
├── Download from HuggingFace...
├── Import Local Model...
├── Convert to AON...
├── Convert to ALM...
├── Compile to TVM...
├── Validate Model...
└── Profile Performance...

Build
├── Build All
├── Build AON Models
├── Build TVM Libraries
├── Build ALM Models
├── Clean Build Artifacts
└── Stop Build

Help
├── Documentation
├── Format Reference
│   ├── .ava (Intent Files)
│   ├── .aon (AON NLU Models)
│   ├── .amm (LLM - MLC TVM)
│   ├── .amg (LLM - GGUF llama.cpp)
│   ├── .amr (LLM - LiteRT Google AI Edge)
│   ├── .adt (Device - TVM Runtime)
│   ├── .adm (Device - MLC Library)
│   ├── .adg (Device - GGUF Library)
│   ├── .adr (Device - LiteRT Library)
│   ├── .ats (Tokenizer - SentencePiece)
│   ├── .ath (Tokenizer - HuggingFace)
│   └── .atv (Tokenizer - Vocabulary)
└── About AVA Toolchain
```

---

## Code Signing (macOS)

### Ad-hoc Signing (Development)

```bash
codesign --force --deep --sign - build/AVAToolchain.app
```

### Developer ID Signing (Distribution)

```bash
# Sign app
codesign \
    --force --deep \
    --sign "Developer ID Application: Your Name (TEAM_ID)" \
    --options runtime \
    --timestamp \
    build/AVAToolchain.app

# Notarize
xcrun notarytool submit AVAToolchain.zip \
    --apple-id your.email@company.com \
    --team-id TEAM_ID \
    --password APP_SPECIFIC_PASSWORD \
    --wait

# Staple
xcrun stapler staple build/AVAToolchain.app
```

---

## Troubleshooting

### macOS Build Issues

| Issue | Solution |
|-------|----------|
| Swift not found | `xcode-select --install` |
| Code signing failed | Use ad-hoc signing: `--sign -` |
| DMG creation failed | Check disk space |

### Linux Build Issues

| Issue | Solution |
|-------|----------|
| PyQt6 not found | `pip install PyQt6` |
| PyInstaller fails | Add `--hidden-import` for missing modules |
| fpm not found | `gem install fpm` |

---

## CI/CD Integration

### GitLab CI Example

```yaml
build:macos:
  stage: build
  tags: [macos]
  script:
    - cd scripts/tools/macos-converter
    - ./build-macos.sh --dmg
  artifacts:
    paths:
      - scripts/tools/macos-converter/release/*.dmg

build:linux:
  stage: build
  image: ubuntu:22.04
  script:
    - cd scripts/tools/gui-app
    - pip install -r requirements.txt pyinstaller
    - ./build-linux.sh --all
  artifacts:
    paths:
      - scripts/tools/gui-app/release/*
```

---

## Related Documentation

| Chapter | Topic |
|---------|-------|
| 37 | Universal Format v2.0 |
| 44 | AVA Naming Convention |
| 45 | LLM Naming Standard |
| 54 | Cross-GPU Model Compilation |

---

**Copyright:** © 2025 Intelligent Devices LLC / Manoj Jhawar
**All Rights Reserved**
