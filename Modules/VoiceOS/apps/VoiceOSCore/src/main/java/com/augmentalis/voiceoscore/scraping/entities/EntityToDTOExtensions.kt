/**
 * EntityToDTOExtensions.kt - Conversion functions from entities to DTOs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

import com.augmentalis.database.dto.*

/**
 * Convert ScrapedAppEntity to ScrapedAppDTO
 */
fun ScrapedAppEntity.toDTO() = ScrapedAppDTO(
    appId = this.appId,
    packageName = this.packageName,
    versionCode = this.versionCode,
    versionName = this.versionName,
    appHash = this.appHash,
    isFullyLearned = this.isFullyLearned,
    learnCompletedAt = this.learnCompletedAt,
    scrapingMode = this.scrapingMode,
    scrapeCount = this.scrapeCount,
    elementCount = this.elementCount,
    commandCount = this.commandCount,
    firstScrapedAt = this.firstScrapedAt,
    lastScrapedAt = this.lastScrapedAt
)

/**
 * Convert ScrapedElementEntity to ScrapedElementDTO
 */
fun ScrapedElementEntity.toDTO() = ScrapedElementDTO(
    id = this.id,
    elementHash = this.elementHash,
    appId = this.appId,
    uuid = this.uuid,
    className = this.className,
    viewIdResourceName = this.viewIdResourceName,
    text = this.text,
    contentDescription = this.contentDescription,
    bounds = this.bounds,
    isClickable = this.isClickable,
    isLongClickable = this.isLongClickable,
    isEditable = this.isEditable,
    isScrollable = this.isScrollable,
    isCheckable = this.isCheckable,
    isFocusable = this.isFocusable,
    isEnabled = this.isEnabled,
    depth = this.depth,
    indexInParent = this.indexInParent,
    scrapedAt = this.scrapedAt,
    semanticRole = this.semanticRole,
    inputType = this.inputType,
    visualWeight = this.visualWeight,
    isRequired = this.isRequired,
    formGroupId = this.formGroupId,
    placeholderText = this.placeholderText,
    validationPattern = this.validationPattern,
    backgroundColor = this.backgroundColor,
    screen_hash = this.screen_hash
)

/**
 * Convert ScrapedHierarchyEntity to ScrapedHierarchyDTO
 *
 * Note: Entity uses element IDs which must be converted to hashes before database insertion
 */
fun ScrapedHierarchyEntity.toDTO(parentHash: String, childHash: String) = ScrapedHierarchyDTO(
    id = this.id,
    parentElementHash = parentHash,
    childElementHash = childHash,
    depth = this.depth.toLong(),
    createdAt = this.createdAt
)

/**
 * Convert ElementRelationshipEntity to ElementRelationshipDTO
 */
fun ElementRelationshipEntity.toDTO() = ElementRelationshipDTO(
    id = this.id,
    sourceElementHash = this.sourceElementHash,
    targetElementHash = this.targetElementHash,
    relationshipType = this.relationshipType,
    relationshipData = this.relationshipData,
    confidence = this.confidence.toDouble(),
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
