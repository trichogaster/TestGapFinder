package io.github.trichogaster.llm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.github.trichogaster.analysis.MethodLlmInput
import io.github.trichogaster.settings.LlmSettingsState

@Service(Service.Level.APP)
class LlmSuggestionService(
    private val settingsState: LlmSettingsState = LlmSettingsState.getInstance(),
    private val llmClient: LlmClient = OpenAiCompatibleLlmClient()
) {
    fun canCallModel(): Boolean {
        val state = settingsState.state
        return state.apiBaseUrl.isNotBlank() && state.apiKey.isNotBlank() && state.modelName.isNotBlank()
    }

    fun suggestTestScenarios(input: MethodLlmInput): LlmAnalysisResult {
        val state = settingsState.state
        require(canCallModel()) {
            "LLM settings are incomplete. Configure API Base URL, API Key, and Model Name."
        }

        val rawResponse = llmClient.suggestTestScenarios(
            input = input,
            config = LlmRequestConfig(
                apiBaseUrl = state.apiBaseUrl,
                apiKey = state.apiKey,
                modelName = state.modelName
            )
        )

        return LlmResultNormalizer.normalize(
            LlmResponseParser.parseFromChatCompletionsResponse(rawResponse)
        )
    }

    companion object {
        fun getInstance(): LlmSuggestionService {
            return ApplicationManager.getApplication().getService(LlmSuggestionService::class.java)
        }
    }
}

