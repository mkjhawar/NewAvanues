package com.augmentalis.ai.server.routes

import com.augmentalis.ai.server.di.ServiceModule
import com.augmentalis.ai.server.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(service: ServiceModule) {
    routing {
        // Health check
        get("/health") {
            call.respond(service.getHealth())
        }

        // NLU routes
        route("/nlu") {
            // Classify instruction category
            post("/classify") {
                val request = call.receive<ClassifyRequest>()
                if (request.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text required"))
                    return@post
                }
                val response = service.classify(request)
                call.respond(response)
            }

            // Detect category (alias for classify)
            post("/category") {
                val request = call.receive<ClassifyRequest>()
                if (request.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text required"))
                    return@post
                }
                val response = service.classify(request)
                call.respond(mapOf(
                    "category" to response.category,
                    "confidence" to response.confidence
                ))
            }

            // Extract entities
            post("/entities") {
                val request = call.receive<ExtractEntitiesRequest>()
                if (request.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text required"))
                    return@post
                }
                val classification = service.classify(ClassifyRequest(request.text))
                call.respond(ExtractEntitiesResponse(
                    entities = classification.entities.map {
                        ExtractedEntity(
                            text = it,
                            type = "TECH",
                            confidence = 0.9f
                        )
                    },
                    inferenceTimeMs = classification.inferenceTimeMs
                ))
            }
        }

        // Embedding routes
        route("/embeddings") {
            // Compute embedding
            post("/compute") {
                val request = call.receive<EmbeddingRequest>()
                if (request.text.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text required"))
                    return@post
                }
                val response = service.computeEmbedding(request)
                call.respond(response)
            }

            // Compute similarity
            post("/similarity") {
                val request = call.receive<SimilarityRequest>()
                if (request.text1.isBlank() || request.text2.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text1 and text2 required"))
                    return@post
                }
                val response = service.computeSimilarity(request)
                call.respond(response)
            }
        }

        // Instruction conversion
        route("/instructions") {
            // Convert NL to compact format
            post("/convert") {
                val request = call.receive<ConvertRequest>()
                if (request.input.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "input required"))
                    return@post
                }
                val response = service.convert(request)
                call.respond(response)
            }
        }

        // Root endpoint
        get("/") {
            call.respond(mapOf(
                "service" to "Augmentalis AI Server",
                "version" to "1.0.0",
                "endpoints" to listOf(
                    "GET /health",
                    "POST /nlu/classify",
                    "POST /nlu/category",
                    "POST /nlu/entities",
                    "POST /embeddings/compute",
                    "POST /embeddings/similarity",
                    "POST /instructions/convert"
                )
            ))
        }
    }
}
