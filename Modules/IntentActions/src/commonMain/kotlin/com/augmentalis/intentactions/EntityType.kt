package com.augmentalis.intentactions

/**
 * Types of entities that can be extracted from user utterances.
 */
enum class EntityType {
    QUERY,
    URL,
    PHONE_NUMBER,
    RECIPIENT,
    MESSAGE,
    LOCATION,
    TIME,
    DURATION,
    APP_NAME
}
