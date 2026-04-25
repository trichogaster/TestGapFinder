package io.github.trichogaster.llm

import io.github.trichogaster.analysis.MethodLlmInput
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OpenAiCompatibleLlmClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()
) : LlmClient {

    override fun suggestTestScenarios(input: MethodLlmInput, config: LlmRequestConfig): String {
        val endpoint = resolveChatCompletionsEndpoint(config.apiBaseUrl)
        val requestBody = buildChatCompletionsRequestBody(input, config.modelName)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "LLM request failed with HTTP ${response.statusCode()}: ${response.body().take(500)}"
            )
        }

        return response.body()
    }

    private fun resolveChatCompletionsEndpoint(apiBaseUrl: String): String {
        val normalizedBaseUrl = apiBaseUrl.trim().trimEnd('/')
        return "$normalizedBaseUrl/chat/completions"
    }

    private fun buildChatCompletionsRequestBody(input: MethodLlmInput, modelName: String): String {
        val prompt = buildPrompt(input)

        return """
            {
              "model": "${escapeJson(modelName)}",
              "messages": [
                {
                  "role": "system",
                  "content": "You are a senior test engineer. Return only strict JSON that follows the requested schema. Never add markdown or prose outside JSON."
                },
                {
                  "role": "user",
                  "content": "${escapeJson(prompt)}"
                }
              ],
              "temperature": 0.2
            }
        """.trimIndent()
    }

    private fun buildPrompt(input: MethodLlmInput): String {
        return """
            Analyze the Java method and produce a structured heuristic assessment.

            IMPORTANT RULES:
            - Do not claim certainty.
            - Assess heuristically.
            - Existing tests are represented only by names, not bodies.
            - Return strict JSON only.
            - Validation can exist in controller/DTO layers (for example via @Valid) and may not appear in service code.
            - Do not mark validation scenarios as likely_missing only because service code has no explicit validation.
            - If upstream validation context is unknown, prefer possibly_covered with a clear assumption.
            - Use exactly these categories: happy_path, boundary, invalid_input, exception_path, edge_case.
            - Use exactly these coverage labels: likely_covered, possibly_covered, likely_missing.
            - Use exactly these priorities: high, medium, low.
            - Include confidence (high|medium|low) and assumption for each scenario.

            REQUIRED JSON SCHEMA:
            ${LlmOutputContract.requiredJsonSchema()}

            COVERAGE GUIDANCE:
            - Decide likely_covered / possibly_covered / likely_missing by matching scenario intent with existing test names only.
            - If evidence is weak, prefer possibly_covered.
            - If no matching test name intent is visible, use likely_missing.
            - For each coverage item, include matchedTests as test names that support the heuristic assessment.
            - If no test name supports an item, matchedTests should be an empty array.

            className: ${input.className}
            methodName: ${input.methodName}
            methodSignature: ${input.methodSignature}

            methodCode:
            ${input.methodCode}

            existingTestNames:
            ${input.testNames.joinToString("\n")}

            extractedSignals:
            ${input.extractedSignals.joinToString("\n")}
        """.trimIndent()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

