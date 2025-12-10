# LearnApp - Complete API Reference

**Module Type:** apps
**Generated:** 2025-10-19 22:03:32 PDT
**Timestamp:** 251019-2203
**Location:** `modules/apps/LearnApp`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the LearnApp module.

## Files


### File: `build/generated/source/kapt/debug/com/augmentalis/learnapp/database/dao/LearnAppDao_Impl.java`

**Package:** `com.augmentalis.learnapp.database.dao`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityDeletionOrUpdateAdapter;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - ... and 21 more

---

### File: `build/generated/source/kapt/debug/com/augmentalis/learnapp/database/LearnAppDatabase_Impl.java`

**Package:** `com.augmentalis.learnapp.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.annotation.NonNull;
  - import androidx.room.DatabaseConfiguration;
  - import androidx.room.InvalidationTracker;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomOpenHelper;
  - import androidx.room.migration.AutoMigrationSpec;
  - import androidx.room.migration.Migration;
  - import androidx.room.util.DBUtil;
  - import androidx.room.util.TableInfo;
  - import androidx.sqlite.db.SupportSQLiteDatabase;
  - ... and 14 more

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/dao/LearnAppDao.java`

**Package:** `com.augmentalis.learnapp.database.dao`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/ExplorationSessionEntity.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/ExplorationStatus.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/LearnedAppEntity.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/NavigationEdgeEntity.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/ScreenStateEntity.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/entities/SessionStatus.java`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/LearnAppDatabase.java`

**Package:** `com.augmentalis.learnapp.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/repository/AppStatistics.java`

**Package:** `com.augmentalis.learnapp.database.repository`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/database/repository/LearnAppRepository.java`

**Package:** `com.augmentalis.learnapp.database.repository`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/detection/AppLaunchDetector.java`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/detection/AppLaunchEvent.java`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/detection/LearnedAppTracker.java`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/detection/TrackerStats.java`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/elements/ClassificationStats.java`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/elements/DangerousElementDetector.java`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/elements/ElementClassifier.java`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/elements/LoginScreenDetector.java`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/examples/MetadataNotificationExample.java`

**Package:** `com.augmentalis.learnapp.examples`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/BFSExplorationStrategy.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/DFSExplorationStrategy.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/ExplorationEngine.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/ExplorationStrategy.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/PrioritizedExplorationStrategy.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/ScreenExplorationResult.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/exploration/ScreenExplorer.java`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/fingerprinting/ScreenFingerprinter.java`

**Package:** `com.augmentalis.learnapp.fingerprinting`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/fingerprinting/ScreenStateManager.java`

**Package:** `com.augmentalis.learnapp.fingerprinting`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/fingerprinting/ScreenStateStats.java`

**Package:** `com.augmentalis.learnapp.fingerprinting`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/generation/CommandGenerationStats.java`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/generation/CommandGenerator.java`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/generation/CommandType.java`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/generation/CommandValidationResult.java`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/generation/GeneratedCommand.java`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/integration/LearnAppIntegration.java`

**Package:** `com.augmentalis.learnapp.integration`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/MetadataNotificationItem.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/MetadataNotificationQueue.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/MetadataQuality.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/MetadataSuggestion.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/MetadataSuggestionGenerator.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/SuggestionCategory.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/metadata/SuggestionPriority.java`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ElementClassification.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ElementInfo.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ExplorationProgress.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ExplorationState.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ExplorationStats.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/LoginFieldType.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/NavigationEdge.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/models/ScreenState.java`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/navigation/GraphStats.java`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/navigation/NavigationGraph.java`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/navigation/NavigationGraphBuilder.java`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/navigation/ScreenNode.java`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/overlays/LoginPromptAction.java`

**Package:** `com.augmentalis.learnapp.overlays`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/overlays/LoginPromptConfig.java`

**Package:** `com.augmentalis.learnapp.overlays`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/overlays/LoginPromptOverlay.java`

**Package:** `com.augmentalis.learnapp.overlays`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/overlays/LoginPromptOverlayKt.java`

**Package:** `com.augmentalis.learnapp.overlays`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/BoundsSnapshot.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/ElementSnapshot.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/InteractionRecorder.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/RecordedInteraction.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/RecordingSession.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/recording/ScrollDirection.java`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/scrolling/ScrollDetector.java`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/scrolling/ScrollDirection.java`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/scrolling/ScrollExecutor.java`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/scrolling/ScrollStats.java`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/CalibrationMetrics.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/CalibrationVariant.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/ConfidenceCalibrator.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/DetectionRecord.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/FlickerPattern.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/HierarchyAnalysisResult.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/HierarchyContext.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/HierarchyPatternMatch.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/HierarchyPatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/IndicatorWeight.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/LoadingContext.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/LoginContext.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MaterialComponent.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MaterialComponentMatch.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MaterialDesignPatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MaterialVersion.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MultiStateDetectionEngine.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/MultiStateResult.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/NegativeIndicator.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/NegativeIndicatorAnalyzer.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/NegativeIndicatorType.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/PatternScope.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/StateCalibrationProfile.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/StateCombinationRule.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/StateDurationEntry.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/StateMetadata.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/StateMetadataExtractor.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/TemporalStateValidator.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/TemporalValidationResult.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/advanced/UIFramework.java`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/AppState.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/AppStateDetector.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/DialogStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/EmptyStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/ErrorStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/LoadingStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/LoginStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/PermissionStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/detectors/TutorialStateDetector.java`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/matchers/ClassNamePatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/matchers/ResourceIdPatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/matchers/TextPatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/PatternMatcher.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/PatternMatchResult.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/patterns/PatternConstants.java`

**Package:** `com.augmentalis.learnapp.state.patterns`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionContext.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionHelpers.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionPatterns.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionPipeline.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionResult.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectionStrategy.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetector.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectorConfig.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateDetectorFactory.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/StateTransition.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/UIFramework.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/state/UIMetadata.java`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/tracking/CoverageMetrics.java`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/tracking/ExplorationStats.java`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/tracking/ProgressTracker.java`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/tracking/ScreenVisit.java`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/tracking/VisualizationData.java`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/ConsentDialogKt.java`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/ConsentDialogManager.java`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/ConsentResponse.java`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/metadata/InsufficientMetadataNotification.java`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/metadata/ManualLabelDialog.java`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/metadata/ManualLabelDialogKt.java`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/metadata/MetadataNotificationView.java`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/metadata/SuggestionChipAdapter.java`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/ProgressOverlayKt.java`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/ui/ProgressOverlayManager.java`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/MetadataNotificationItem.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/MetadataQuality.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/MetadataQualityLevel.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/MetadataQualityScore.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/MetadataValidator.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/validation/PoorQualityElementInfo.java`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/version/AppVersionInfo.java`

**Package:** `com.augmentalis.learnapp.version`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/version/UpdateStatus.java`

**Package:** `com.augmentalis.learnapp.version`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/version/VersionInfoProvider.java`

**Package:** `com.augmentalis.learnapp.version`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/com/augmentalis/learnapp/version/VersionResult.java`

**Package:** `com.augmentalis.learnapp.version`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/tmp/kapt3/stubs/debug/error/NonExistentClass.java`

**Package:** `error`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/database/dao/LearnAppDao.kt`

**Package:** `com.augmentalis.learnapp.database.dao`

**Classes/Interfaces/Objects:**
  - interface LearnAppDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import com.augmentalis.learnapp.database.entities.*

---

### File: `src/main/java/com/augmentalis/learnapp/database/entities/ExplorationSessionEntity.kt`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**
  - data class ExplorationSessionEntity(
  - object SessionStatus 

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/learnapp/database/entities/LearnedAppEntity.kt`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**
  - data class LearnedAppEntity(
  - object ExplorationStatus 

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/learnapp/database/entities/NavigationEdgeEntity.kt`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**
  - data class NavigationEdgeEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/learnapp/database/entities/ScreenStateEntity.kt`

**Package:** `com.augmentalis.learnapp.database.entities`

**Classes/Interfaces/Objects:**
  - data class ScreenStateEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/learnapp/database/LearnAppDatabase.kt`

**Package:** `com.augmentalis.learnapp.database`

**Classes/Interfaces/Objects:**
  - abstract class LearnAppDatabase 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase
  - import com.augmentalis.learnapp.database.dao.LearnAppDao
  - import com.augmentalis.learnapp.database.entities.*

---

### File: `src/main/java/com/augmentalis/learnapp/database/repository/LearnAppRepository.kt`

**Package:** `com.augmentalis.learnapp.database.repository`

**Classes/Interfaces/Objects:**
  - class LearnAppRepository(
  - data class AppStatistics(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.database.dao.LearnAppDao
  - import com.augmentalis.learnapp.database.entities.*
  - import com.augmentalis.learnapp.models.ExplorationStats
  - import com.augmentalis.learnapp.models.ScreenState
  - import com.augmentalis.learnapp.navigation.NavigationGraph
  - import com.augmentalis.learnapp.navigation.ScreenNode
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/learnapp/detection/AppLaunchDetector.kt`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**
  - class AppLaunchDetector(
  - sealed class AppLaunchEvent 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.view.accessibility.AccessibilityEvent
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/learnapp/detection/LearnedAppTracker.kt`

**Package:** `com.augmentalis.learnapp.detection`

**Classes/Interfaces/Objects:**
  - class LearnedAppTracker(private val context
  - data class TrackerStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock

---

### File: `src/main/java/com/augmentalis/learnapp/elements/DangerousElementDetector.kt`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**
  - class DangerousElementDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo

---

### File: `src/main/java/com/augmentalis/learnapp/elements/ElementClassifier.kt`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**
  - class ElementClassifier 
  - data class ClassificationStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementClassification
  - import com.augmentalis.learnapp.models.ElementInfo

---

### File: `src/main/java/com/augmentalis/learnapp/elements/LoginScreenDetector.kt`

**Package:** `com.augmentalis.learnapp.elements`

**Classes/Interfaces/Objects:**
  - class LoginScreenDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo
  - import com.augmentalis.learnapp.models.LoginFieldType

---

### File: `src/main/java/com/augmentalis/learnapp/examples/MetadataNotificationExample.kt`

**Package:** `com.augmentalis.learnapp.examples`

**Classes/Interfaces/Objects:**
  - class MetadataNotificationExample(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.learnapp.metadata.MetadataNotificationQueue
  - import com.augmentalis.learnapp.metadata.MetadataQuality
  - import com.augmentalis.learnapp.models.ElementInfo
  - import com.augmentalis.learnapp.ui.metadata.InsufficientMetadataNotification

---

### File: `src/main/java/com/augmentalis/learnapp/exploration/ExplorationEngine.kt`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**
  - class ExplorationEngine(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.elements.ElementClassifier
  - import com.augmentalis.learnapp.fingerprinting.ScreenStateManager
  - import com.augmentalis.learnapp.models.ExplorationProgress
  - import com.augmentalis.learnapp.models.ExplorationState
  - import com.augmentalis.learnapp.models.ExplorationStats
  - import com.augmentalis.learnapp.navigation.NavigationGraphBuilder
  - import com.augmentalis.learnapp.scrolling.ScrollDetector
  - import com.augmentalis.learnapp.scrolling.ScrollExecutor
  - ... and 14 more

---

### File: `src/main/java/com/augmentalis/learnapp/exploration/ExplorationStrategy.kt`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**
  - interface ExplorationStrategy 
  - class DFSExplorationStrategy 
  - class BFSExplorationStrategy 
  - class PrioritizedExplorationStrategy 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo

---

### File: `src/main/java/com/augmentalis/learnapp/exploration/ScreenExplorer.kt`

**Package:** `com.augmentalis.learnapp.exploration`

**Classes/Interfaces/Objects:**
  - class ScreenExplorer(
  - sealed class ScreenExplorationResult 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.elements.ElementClassifier
  - import com.augmentalis.learnapp.fingerprinting.ScreenStateManager
  - import com.augmentalis.learnapp.models.ElementClassification
  - import com.augmentalis.learnapp.models.ElementInfo
  - import com.augmentalis.learnapp.models.ScreenState
  - import com.augmentalis.learnapp.scrolling.ScrollDetector
  - import com.augmentalis.learnapp.scrolling.ScrollExecutor

---

### File: `src/main/java/com/augmentalis/learnapp/fingerprinting/ScreenFingerprinter.kt`

**Package:** `com.augmentalis.learnapp.fingerprinting`

**Classes/Interfaces/Objects:**
  - class ScreenFingerprinter 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/learnapp/fingerprinting/ScreenStateManager.kt`

**Package:** `com.augmentalis.learnapp.fingerprinting`

**Classes/Interfaces/Objects:**
  - class ScreenStateManager 
  - data class ScreenStateStats(

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.models.ScreenState
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock

---

### File: `src/main/java/com/augmentalis/learnapp/generation/CommandGenerator.kt`

**Package:** `com.augmentalis.learnapp.generation`

**Classes/Interfaces/Objects:**
  - class CommandGenerator 
  - data class GeneratedCommand(
  - enum class CommandType 
  - data class CommandValidationResult(
  - data class CommandGenerationStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/learnapp/integration/LearnAppIntegration.kt`

**Package:** `com.augmentalis.learnapp.integration`

**Classes/Interfaces/Objects:**
  - class LearnAppIntegration private constructor(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.learnapp.database.LearnAppDatabase
  - import com.augmentalis.learnapp.database.repository.LearnAppRepository
  - import com.augmentalis.learnapp.detection.AppLaunchDetector
  - import com.augmentalis.learnapp.detection.LearnedAppTracker
  - import com.augmentalis.learnapp.exploration.DFSExplorationStrategy
  - import com.augmentalis.learnapp.exploration.ExplorationEngine
  - import com.augmentalis.learnapp.exploration.ExplorationStrategy
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/learnapp/metadata/MetadataNotificationQueue.kt`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**
  - class MetadataNotificationQueue(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import com.augmentalis.learnapp.models.ElementInfo
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.PriorityQueue

---

### File: `src/main/java/com/augmentalis/learnapp/metadata/MetadataQuality.kt`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**
  - enum class MetadataQuality 
  - data class MetadataNotificationItem(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo

---

### File: `src/main/java/com/augmentalis/learnapp/metadata/MetadataSuggestionGenerator.kt`

**Package:** `com.augmentalis.learnapp.metadata`

**Classes/Interfaces/Objects:**
  - object MetadataSuggestionGenerator 
  - data class MetadataSuggestion(
  - enum class SuggestionPriority(val level
  - enum class SuggestionCategory 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ElementInfo

---

### File: `src/main/java/com/augmentalis/learnapp/models/ElementClassification.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - sealed class ElementClassification 
  - enum class LoginFieldType 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/models/ElementInfo.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - data class ElementInfo(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/learnapp/models/ExplorationProgress.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - data class ExplorationProgress(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/models/ExplorationState.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - sealed class ExplorationState 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/models/ExplorationStats.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - data class ExplorationStats(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/models/NavigationEdge.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - data class NavigationEdge(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/models/ScreenState.kt`

**Package:** `com.augmentalis.learnapp.models`

**Classes/Interfaces/Objects:**
  - data class ScreenState(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/navigation/NavigationGraph.kt`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**
  - data class NavigationGraph(
  - data class ScreenNode(
  - data class GraphStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.NavigationEdge
  - import com.augmentalis.learnapp.models.ScreenState

---

### File: `src/main/java/com/augmentalis/learnapp/navigation/NavigationGraphBuilder.kt`

**Package:** `com.augmentalis.learnapp.navigation`

**Classes/Interfaces/Objects:**
  - class NavigationGraphBuilder(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.NavigationEdge
  - import com.augmentalis.learnapp.models.ScreenState
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock

---

### File: `src/main/java/com/augmentalis/learnapp/overlays/LoginPromptOverlay.kt`

**Package:** `com.augmentalis.learnapp.overlays`

**Classes/Interfaces/Objects:**
  - sealed class LoginPromptAction 
  - data class LoginPromptConfig(
  - class LoginPromptOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.util.Log
  - import android.view.Gravity
  - import android.view.View
  - import android.view.WindowManager
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - ... and 14 more

---

### File: `src/main/java/com/augmentalis/learnapp/recording/InteractionRecorder.kt`

**Package:** `com.augmentalis.learnapp.recording`

**Classes/Interfaces/Objects:**
  - class InteractionRecorder 
  - sealed class RecordedInteraction 
  - data class ElementSnapshot(
  - data class BoundsSnapshot(
  - enum class ScrollDirection 
  - data class RecordingSession(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.models.ElementInfo
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import org.json.JSONArray
  - import org.json.JSONObject

---

### File: `src/main/java/com/augmentalis/learnapp/scrolling/ScrollDetector.kt`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**
  - class ScrollDetector 
  - enum class ScrollDirection 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/learnapp/scrolling/ScrollExecutor.kt`

**Package:** `com.augmentalis.learnapp.scrolling`

**Classes/Interfaces/Objects:**
  - class ScrollExecutor 
  - data class ScrollStats(

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.models.ElementInfo
  - import kotlinx.coroutines.delay
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/ConfidenceCalibrator.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - data class IndicatorWeight(
  - data class StateCalibrationProfile(
  - data class CalibrationMetrics(
  - data class CalibrationVariant(
  - class ConfidenceCalibrator 
  - data class DetectionRecord(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import kotlin.math.exp

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/HierarchyPatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - data class HierarchyContext(
  - data class HierarchyPatternMatch(
  - enum class PatternScope 
  - data class HierarchyAnalysisResult(
  - class HierarchyPatternMatcher 
  - enum class LoginContext 
  - enum class LoadingContext 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.state.AppState

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/MaterialDesignPatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - enum class MaterialComponent 
  - data class MaterialComponentMatch(
  - class MaterialDesignPatternMatcher 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/MultiStateDetectionEngine.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - data class MultiStateResult(
  - data class StateCombinationRule(
  - class MultiStateDetectionEngine 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionResult

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/NegativeIndicatorAnalyzer.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - data class NegativeIndicator(
  - enum class NegativeIndicatorType 
  - class NegativeIndicatorAnalyzer 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.state.AppState

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/StateMetadata.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - enum class UIFramework 
  - enum class MaterialVersion 
  - data class StateMetadata(
  - class StateMetadataExtractor 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.learnapp.state.AppState

---

### File: `src/main/java/com/augmentalis/learnapp/state/advanced/TemporalStateValidator.kt`

**Package:** `com.augmentalis.learnapp.state.advanced`

**Classes/Interfaces/Objects:**
  - data class StateDurationEntry(
  - data class FlickerPattern(
  - data class TemporalValidationResult(
  - class TemporalStateValidator(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState

---

### File: `src/main/java/com/augmentalis/learnapp/state/AppStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - enum class AppState 
  - data class StateDetectionResult(
  - data class StateTransition(
  - data class StateDetectorConfig(
  - class AppStateDetector(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/DialogStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class DialogStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/EmptyStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class EmptyStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/ErrorStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class ErrorStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/LoadingStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class LoadingStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/LoginStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class LoginStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ClassNamePatternMatcher
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/PermissionStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class PermissionStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/detectors/TutorialStateDetector.kt`

**Package:** `com.augmentalis.learnapp.state.detectors`

**Classes/Interfaces/Objects:**
  - class TutorialStateDetector 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.AppState
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import com.augmentalis.learnapp.state.StateDetectionPatterns
  - import com.augmentalis.learnapp.state.StateDetectionResult
  - import com.augmentalis.learnapp.state.StateDetectionStrategy
  - import com.augmentalis.learnapp.state.StateDetector
  - import com.augmentalis.learnapp.state.matchers.ResourceIdPatternMatcher
  - import com.augmentalis.learnapp.state.matchers.TextPatternMatcher

---

### File: `src/main/java/com/augmentalis/learnapp/state/matchers/ClassNamePatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**
  - class ClassNamePatternMatcher 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.PatternMatcher
  - import com.augmentalis.learnapp.state.PatternMatchResult
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/learnapp/state/matchers/ResourceIdPatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**
  - class ResourceIdPatternMatcher 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.PatternMatcher
  - import com.augmentalis.learnapp.state.PatternMatchResult
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/learnapp/state/matchers/TextPatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state.matchers`

**Classes/Interfaces/Objects:**
  - class TextPatternMatcher 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.PatternMatcher
  - import com.augmentalis.learnapp.state.PatternMatchResult
  - import com.augmentalis.learnapp.state.StateDetectionContext
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/learnapp/state/PatternMatcher.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - interface PatternMatcher 
  - data class PatternMatchResult(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/patterns/PatternConstants.kt`

**Package:** `com.augmentalis.learnapp.state.patterns`

**Classes/Interfaces/Objects:**
  - object PatternConstants 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetectionHelpers.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - object StateDetectionHelpers 
  - enum class UIFramework 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetectionPatterns.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - object StateDetectionPatterns 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetectionPipeline.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - class StateDetectionPipeline(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetectionStrategy.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - interface StateDetectionStrategy 
  - data class StateDetectionContext(
  - data class UIMetadata(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetector.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - interface StateDetector 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/learnapp/state/StateDetectorFactory.kt`

**Package:** `com.augmentalis.learnapp.state`

**Classes/Interfaces/Objects:**
  - object StateDetectorFactory 

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.state.detectors.*

---

### File: `src/main/java/com/augmentalis/learnapp/tracking/ProgressTracker.kt`

**Package:** `com.augmentalis.learnapp.tracking`

**Classes/Interfaces/Objects:**
  - class ProgressTracker(
  - data class CoverageMetrics(
  - data class ScreenVisit(
  - data class ExplorationStats(
  - data class VisualizationData(

**Public Functions:**

**Imports:**
  - import com.augmentalis.learnapp.models.ExplorationProgress
  - import com.augmentalis.learnapp.models.ScreenState
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun ConsentDialog(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.dp
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/learnapp/ui/ConsentDialogManager.kt`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**
  - class ConsentDialogManager(
  - sealed class ConsentResponse 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.provider.Settings
  - import android.view.WindowManager
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.ui.platform.ComposeView
  - import com.augmentalis.learnapp.detection.LearnedAppTracker
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/learnapp/ui/metadata/InsufficientMetadataNotification.kt`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**
  - class InsufficientMetadataNotification(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.view.LayoutInflater
  - import android.view.WindowManager
  - import com.augmentalis.learnapp.metadata.MetadataNotificationItem
  - import com.augmentalis.learnapp.metadata.MetadataNotificationQueue

---

### File: `src/main/java/com/augmentalis/learnapp/ui/metadata/ManualLabelDialog.kt`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**
  - class ManualLabelDialog(

**Public Functions:**

**Imports:**
  - import android.app.Dialog
  - import android.content.Context
  - import android.view.LayoutInflater
  - import android.view.Window
  - import android.widget.TextView
  - import com.augmentalis.learnapp.R
  - import com.augmentalis.learnapp.metadata.MetadataNotificationItem
  - import com.google.android.material.button.MaterialButton
  - import com.google.android.material.chip.Chip
  - import com.google.android.material.chip.ChipGroup
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/learnapp/ui/metadata/MetadataNotificationView.kt`

**Package:** `com.augmentalis.learnapp.ui.metadata`

**Classes/Interfaces/Objects:**
  - class MetadataNotificationView @JvmOverloads constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.AttributeSet
  - import android.view.LayoutInflater
  - import android.view.View
  - import android.widget.FrameLayout
  - import android.widget.ImageButton
  - import android.widget.TextView
  - import androidx.recyclerview.widget.LinearLayoutManager
  - import androidx.recyclerview.widget.RecyclerView
  - import com.augmentalis.learnapp.R
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/learnapp/ui/ProgressOverlay.kt`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun ProgressOverlay(
  - fun ProgressOverlayCompact(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.dp
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/learnapp/ui/ProgressOverlayManager.kt`

**Package:** `com.augmentalis.learnapp.ui`

**Classes/Interfaces/Objects:**
  - class ProgressOverlayManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.view.WindowManager
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.ui.platform.ComposeView
  - import com.augmentalis.learnapp.models.ExplorationProgress

---

### File: `src/main/java/com/augmentalis/learnapp/validation/MetadataQuality.kt`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**
  - enum class MetadataQualityLevel 
  - data class MetadataQualityScore(
  - object MetadataQuality 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/learnapp/validation/MetadataValidator.kt`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**
  - class MetadataValidator 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/learnapp/validation/PoorQualityElementInfo.kt`

**Package:** `com.augmentalis.learnapp.validation`

**Classes/Interfaces/Objects:**
  - data class PoorQualityElementInfo(
  - data class MetadataNotificationItem(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect

---

### File: `src/main/java/com/augmentalis/learnapp/version/VersionInfoProvider.kt`

**Package:** `com.augmentalis.learnapp.version`

**Classes/Interfaces/Objects:**
  - data class AppVersionInfo(
  - sealed class VersionResult 
  - sealed class UpdateStatus 
  - class VersionInfoProvider(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageInfo
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

## Summary

**Total Files:** 227

**Module Structure:**
```
                  build
                    generated
                      ap_generated_sources
                        debug
                          out
                      res
                        pngs
                          debug
                        resValues
                          debug
                      source
                        kapt
                          debug
                            com
                              augmentalis
                                learnapp
                                  database
                                    dao
                        kaptKotlin
                          debug
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
                      annotation_processor_list
                        debug
                          javaPreCompileDebug
                      compile_library_classes_jar
                        debug
                          bundleLibCompileToJarDebug
                      compile_r_class_jar
                        debug
                          generateDebugRFile
                      compile_symbol_list
                        debug
                          generateDebugRFile
                      compiled_local_resources
                        debug
                          compileDebugLibraryResources
                            out
                      data_binding_layout_info_type_package
                        debug
                          packageDebugResources
                            out
                      incremental
                        debug
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
