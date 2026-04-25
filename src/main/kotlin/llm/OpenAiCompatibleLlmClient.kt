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
                  "content": "You are a senior test engineer. Suggest likely important test scenarios as concise bullet points."
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
            Analyze the Java method and suggest likely missing test scenarios.

            className: ${input.className}
            methodName: ${input.methodName}
            methodSignature: ${input.methodSignature}

            methodCode:
            ${input.methodCode}

            existingTestNames:
            ${input.testNames.joinToString("\n")}

            extractedSignals:
            ${input.extractedSignals.joinToString("\n")}

            Return concise bullets grouped by:
            1) likely covered
            2) possibly covered
            3) likely missing
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

