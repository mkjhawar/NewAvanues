/**
 * TestTypeAliases.kt - Type aliases for existing types in test environment
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Provides type aliases and imports to resolve compilation issues
 * in test files by mapping to existing production classes.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.voiceos.speech.engines.common.ServiceState

/**
 * Type alias for State enum used in tests
 * Maps to the existing ServiceState.State from production code
 */
typealias State = ServiceState.State

/**
 * Import alias for CommandCache from production code
 */
typealias CommandCache = com.augmentalis.voiceos.speech.engines.common.CommandCache