package io.github.trichogaster.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object LlmResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseFromChatCompletionsResponse(rawResponse: String): LlmAnalysisResult {
        try {
            val root = json.parseToJsonElement(rawResponse).jsonObject
            val content = root.extractMessageContent()
            return parseContractJson(content)
        } catch (t: Throwable) {
            throw LlmResponseParseException("The model response could not be parsed. Please try again.", t)
        }
    }

    private fun parseContractJson(content: String): LlmAnalysisResult {
        val normalized = normalizeJsonContent(content)
        val payload = json.parseToJsonElement(normalized).jsonObject

        val summary = payload.requireString("summary")
        val scenarios = payload.requireArray("scenarios").map { it.toScenario() }
        val coverage = payload.requireObject("coverageAssessment")

        return LlmAnalysisResult(
            summary = summary,
            scenarios = scenarios,
            coverageAssessment = LlmCoverageAssessment(
                likelyCovered = coverage.requireArray("likely_covered").map { it.toCoverageItem() },
                possiblyCovered = coverage.requireArray("possibly_covered").map { it.toCoverageItem() },
                likelyMissing = coverage.requireArray("likely_missing").map { it.toCoverageItem() }
            )
        )
    }

    private fun JsonObject.extractMessageContent(): String {
        val choices = this.requireArray("choices")
        val firstChoice = choices.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("LLM response has no choices.")
        val message = firstChoice.requireObject("message")

        return message.requireString("content")
    }

    private fun normalizeJsonContent(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("```")) {
            return trimmed
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        return trimmed
    }

    private fun JsonElement.toScenario(): LlmScenario {
        val obj = this.jsonObject

        return LlmScenario(
            title = obj.requireString("title"),
            category = obj.requireString("category"),
            priority = obj.requireString("priority"),
            rationale = obj.requireString("rationale"),
            confidence = obj.optionalString("confidence") ?: "medium",
            assumption = obj.optionalString("assumption") ?: ""
        )
    }

    private fun JsonElement.toCoverageItem(): LlmCoverageItem {
        val obj = this.jsonObject

        return LlmCoverageItem(
            title = obj.requireString("title"),
            matchedTests = obj.requireArray("matchedTests").map { it.asString() }
        )
    }

    private fun JsonObject.requireObject(key: String): JsonObject {
        val element = this[key] ?: throw IllegalStateException("Missing JSON object field: $key")
        return element.jsonObject
    }

    private fun JsonObject.requireArray(key: String): JsonArray {
        val element = this[key] ?: throw IllegalStateException("Missing JSON array field: $key")
        return element.jsonArray
    }

    private fun JsonObject.requireString(key: String): String {
        val element = this[key] ?: throw IllegalStateException("Missing JSON string field: $key")
        return element.asString()
    }

    private fun JsonObject.optionalString(key: String): String? {
        val element = this[key] ?: return null
        return element.asString()
    }

    private fun JsonElement.asString(): String {
        return (this as? JsonPrimitive)?.content
            ?: throw IllegalStateException("Expected JSON primitive string but got: $this")
    }
}


