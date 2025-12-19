// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/data/clustering/KMeansClustering.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.clustering

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * K-means clustering for vector embeddings
 *
 * Phase 3.2: Cluster embeddings for fast approximate nearest neighbor search
 *
 * Algorithm:
 * 1. Initialize k random centroids
 * 2. Assign each vector to nearest centroid
 * 3. Recompute centroids as mean of assigned vectors
 * 4. Repeat until convergence
 *
 * Performance:
 * - Clusters: 256 (optimal for 200k chunks)
 * - Iterations: 10-20 typical
 * - Time: O(n * k * d * iterations)
 *   - n = number of vectors
 *   - k = number of clusters
 *   - d = dimensionality (384)
 */
class KMeansClustering(
    private val k: Int = 256, // Number of clusters
    private val maxIterations: Int = 50,
    private val convergenceThreshold: Float = 0.001f,
    private val seed: Long = 42
) {

    private val random = Random(seed)

    /**
     * Cluster vectors using k-means
     *
     * @param vectors List of embedding vectors (each vector is FloatArray)
     * @return ClusteringResult with centroids and assignments
     */
    fun cluster(vectors: List<FloatArray>): ClusteringResult {
        require(vectors.isNotEmpty()) { "Cannot cluster empty vector list" }
        require(k <= vectors.size) { "k ($k) must be <= number of vectors (${vectors.size})" }

        val dimension = vectors.first().size
        require(vectors.all { it.size == dimension }) { "All vectors must have same dimensionality" }

        // Initialize centroids using k-means++
        val centroids = initializeCentroidsKMeansPlusPlus(vectors, dimension)

        // Assignment: which cluster each vector belongs to
        val assignments = IntArray(vectors.size)

        var iteration = 0
        var previousInertia = Float.MAX_VALUE

        while (iteration < maxIterations) {
            // Assign each vector to nearest centroid
            var totalInertia = 0f
            for (i in vectors.indices) {
                val (nearestCluster, distance) = findNearestCentroid(vectors[i], centroids)
                assignments[i] = nearestCluster
                totalInertia += distance * distance
            }

            // Check convergence
            val improvement = (previousInertia - totalInertia) / previousInertia
            if (improvement < convergenceThreshold) {
                break
            }
            previousInertia = totalInertia

            // Recompute centroids
            updateCentroids(vectors, assignments, centroids, dimension)

            iteration++
        }

        // Calculate final cluster sizes
        val clusterSizes = IntArray(k)
        for (assignment in assignments) {
            clusterSizes[assignment]++
        }

        return ClusteringResult(
            centroids = centroids,
            assignments = assignments,
            clusterSizes = clusterSizes,
            iterations = iteration,
            inertia = previousInertia
        )
    }

    /**
     * Initialize centroids using k-means++ algorithm
     *
     * This gives better initial centroids than random selection:
     * 1. Choose first centroid randomly
     * 2. For each subsequent centroid, choose from remaining points
     *    with probability proportional to squared distance to nearest centroid
     */
    private fun initializeCentroidsKMeansPlusPlus(
        vectors: List<FloatArray>,
        dimension: Int
    ): Array<FloatArray> {
        val centroids = Array(k) { FloatArray(dimension) }

        // Choose first centroid randomly
        val firstIndex = random.nextInt(vectors.size)
        vectors[firstIndex].copyInto(centroids[0])

        // Choose remaining centroids
        for (i in 1 until k) {
            val distances = FloatArray(vectors.size)
            var totalDistance = 0f

            // Calculate distance to nearest centroid for each vector
            for (j in vectors.indices) {
                var minDist = Float.MAX_VALUE
                for (c in 0 until i) {
                    val dist = euclideanDistance(vectors[j], centroids[c])
                    if (dist < minDist) {
                        minDist = dist
                    }
                }
                distances[j] = minDist * minDist // Squared distance
                totalDistance += distances[j]
            }

            // Choose next centroid with probability proportional to distance
            var target = random.nextFloat() * totalDistance
            var chosenIndex = 0
            for (j in vectors.indices) {
                target -= distances[j]
                if (target <= 0) {
                    chosenIndex = j
                    break
                }
            }

            vectors[chosenIndex].copyInto(centroids[i])
        }

        return centroids
    }

    /**
     * Update centroids as mean of assigned vectors
     */
    private fun updateCentroids(
        vectors: List<FloatArray>,
        assignments: IntArray,
        centroids: Array<FloatArray>,
        dimension: Int
    ) {
        // Reset centroids
        for (c in centroids) {
            c.fill(0f)
        }

        // Sum vectors in each cluster
        val counts = IntArray(k)
        for (i in vectors.indices) {
            val cluster = assignments[i]
            counts[cluster]++
            for (d in 0 until dimension) {
                centroids[cluster][d] += vectors[i][d]
            }
        }

        // Compute means
        for (c in 0 until k) {
            if (counts[c] > 0) {
                val count = counts[c].toFloat()
                for (d in 0 until dimension) {
                    centroids[c][d] = centroids[c][d] / count
                }
            }
        }
    }

    /**
     * Find nearest centroid to a vector
     *
     * @return Pair of (cluster index, distance)
     */
    fun findNearestCentroid(
        vector: FloatArray,
        centroids: Array<FloatArray>
    ): Pair<Int, Float> {
        var minDist = Float.MAX_VALUE
        var nearestCluster = 0

        for (i in centroids.indices) {
            val dist = euclideanDistance(vector, centroids[i])
            if (dist < minDist) {
                minDist = dist
                nearestCluster = i
            }
        }

        return Pair(nearestCluster, minDist)
    }

    /**
     * Euclidean distance between two vectors
     */
    private fun euclideanDistance(v1: FloatArray, v2: FloatArray): Float {
        var sum = 0f
        for (i in v1.indices) {
            val diff = v1[i] - v2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    /**
     * Cosine similarity between two vectors
     *
     * Used for final ranking within a cluster
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        val denominator = sqrt(norm1 * norm2)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }
}

/**
 * Result of k-means clustering
 */
data class ClusteringResult(
    val centroids: Array<FloatArray>,
    val assignments: IntArray,
    val clusterSizes: IntArray,
    val iterations: Int,
    val inertia: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClusteringResult

        if (!centroids.contentDeepEquals(other.centroids)) return false
        if (!assignments.contentEquals(other.assignments)) return false
        if (!clusterSizes.contentEquals(other.clusterSizes)) return false
        if (iterations != other.iterations) return false
        if (inertia != other.inertia) return false

        return true
    }

    override fun hashCode(): Int {
        var result = centroids.contentDeepHashCode()
        result = 31 * result + assignments.contentHashCode()
        result = 31 * result + clusterSizes.contentHashCode()
        result = 31 * result + iterations
        result = 31 * result + inertia.hashCode()
        return result
    }
}
