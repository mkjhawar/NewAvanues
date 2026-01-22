package com.augmentalis.magicdata

/**
 * Enum representing supported field types in database collections.
 *
 * Used for schema definition to specify the type of data stored in collection fields.
 *
 * @since 1.0.0
 */
enum class FieldType {
    /**
     * String/text field type.
     */
    STRING,

    /**
     * Integer number field type.
     */
    INT,

    /**
     * Floating-point number field type.
     */
    FLOAT,

    /**
     * Boolean (true/false) field type.
     */
    BOOLEAN,

    /**
     * Timestamp/date-time field type.
     */
    TIMESTAMP,

    /**
     * JSON object field type for complex nested data.
     */
    JSON
}
