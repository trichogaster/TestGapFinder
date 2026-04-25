package io.github.trichogaster.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "TestGapFinderLlmSettings", storages = [Storage("test-gap-finder.xml")])
class LlmSettingsState : PersistentStateComponent<LlmSettingsState.State> {
    data class State(
        var apiBaseUrl: String = "https://api.openai.com/v1",
        var apiKey: String = "",
        var modelName: String = "gpt-4o-mini"
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun update(apiBaseUrl: String, apiKey: String, modelName: String) {
        state = state.copy(
            apiBaseUrl = apiBaseUrl.trim(),
            apiKey = apiKey.trim(),
            modelName = modelName.trim()
        )
    }

    companion object {
        fun getInstance(): LlmSettingsState {
            return ApplicationManager.getApplication().getService(LlmSettingsState::class.java)
        }
    }
}

