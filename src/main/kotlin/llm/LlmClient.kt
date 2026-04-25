package io.github.trichogaster.llm

import io.github.trichogaster.analysis.MethodLlmInput

data class LlmRequestConfig(
    val apiBaseUrl: String,
    val apiKey: String,
    val modelName: String
)

interface LlmClient {
    fun suggestTestScenarios(input: MethodLlmInput, config: LlmRequestConfig): String
}

