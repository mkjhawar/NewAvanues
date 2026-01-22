package com.augmentalis.magicdata

/**
 * Represents a query for filtering and sorting documents in a collection.
 *
 * Provides a builder pattern for constructing queries with various filters, sorting,
 * and pagination options.
 *
 * Example:
 * ```kotlin
 * // Find all completed tasks
 * val query1 = Query.where("completed", "true")
 *
 * // Find incomplete tasks, ordered by priority, limit 10
 * val query2 = Query(
 *     filters = mapOf("completed" to "false"),
 *     orderBy = "priority",
 *     ascending = false,
 *     limit = 10
 * )
 *
 * // Get all documents
 * val query3 = Query.all()
 * ```
 *
 * @property filters Map of field names to values for filtering
 * @property orderBy Field name to sort by (null = no sorting)
 * @property ascending Sort direction (true = ascending, false = descending)
 * @property limit Maximum number of results to return (null = no limit)
 * @property offset Number of results to skip (null = no offset)
 * @since 1.0.0
 */
data class Query(
    val filters: Map<String, String> = emptyMap(),
    val orderBy: String? = null,
    val ascending: Boolean = true,
    val limit: Int? = null,
    val offset: Int? = null
) {
    companion object {
        /**
         * Creates a query that filters by a single field value.
         *
         * @param field The field name to filter on
         * @param value The value to match
         * @return A new Query with the specified filter
         */
        fun where(field: String, value: String): Query {
            return Query(filters = mapOf(field to value))
        }

        /**
         * Creates a query that matches all documents.
         *
         * @return A new Query with no filters
         */
        fun all(): Query {
            return Query()
        }
    }

    /**
     * Builder class for constructing queries with a fluent API.
     *
     * Example:
     * ```kotlin
     * val query = QueryBuilder()
     *     .where("status", "active")
     *     .orderBy("createdAt", ascending = false)
     *     .limit(20)
     *     .build()
     * ```
     */
    class Builder {
        private val filters = mutableMapOf<String, String>()
        private var orderBy: String? = null
        private var ascending: Boolean = true
        private var limit: Int? = null
        private var offset: Int? = null

        /**
         * Adds a filter to the query.
         *
         * @param field The field name to filter on
         * @param value The value to match
         * @return This builder for chaining
         */
        fun where(field: String, value: String): Builder {
            filters[field] = value
            return this
        }

        /**
         * Sets the sort field and direction.
         *
         * @param field The field name to sort by
         * @param ascending Sort direction (true = ascending, false = descending)
         * @return This builder for chaining
         */
        fun orderBy(field: String, ascending: Boolean = true): Builder {
            this.orderBy = field
            this.ascending = ascending
            return this
        }

        /**
         * Sets the maximum number of results to return.
         *
         * @param count Maximum result count
         * @return This builder for chaining
         */
        fun limit(count: Int): Builder {
            this.limit = count
            return this
        }

        /**
         * Sets the number of results to skip.
         *
         * @param count Number of results to skip
         * @return This builder for chaining
         */
        fun offset(count: Int): Builder {
            this.offset = count
            return this
        }

        /**
         * Builds the final Query object.
         *
         * @return A new Query with the configured parameters
         */
        fun build(): Query {
            return Query(
                filters = filters.toMap(),
                orderBy = orderBy,
                ascending = ascending,
                limit = limit,
                offset = offset
            )
        }
    }
}
