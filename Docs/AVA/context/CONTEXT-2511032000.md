# CONTEXT SAVE

**Timestamp:** 2511032000
**Token Count:** ~25000
**Project:** ava
**Task:** Create ModelDownloadManager system for on-demand ML model downloads

## Summary
Creating a comprehensive model download management system to reduce APK size from 160MB to ~8MB by downloading models on-demand instead of bundling them in the APK. This includes download manager, cache manager, config, and download states.

## Analysis Completed
1. Read REGISTRY.md - no existing download manager found
2. Checked related projects (AVAConnect, VoiceAvanue) - no conflicts
3. Examined existing ModelManager in NLU feature - has basic download logic
4. Examined LocalLLMProvider - needs integration with download system
5. Reviewed build.gradle.kts dependencies

## Components to Create
1. ModelDownloadManager.kt - Core download orchestration with Flow-based progress
2. ModelDownloadConfig.kt - Model metadata and configuration
3. DownloadState.kt - Sealed class for download states
4. ModelCacheManager.kt - Local cache management
5. Integration points in existing loaders

## Next Steps
1. Create TodoWrite task plan
2. Create download package structure
3. Implement DownloadState sealed class
4. Implement ModelDownloadConfig data classes
5. Implement ModelCacheManager
6. Implement ModelDownloadManager
7. Update existing model loaders

## Open Questions
None - requirements are clear
