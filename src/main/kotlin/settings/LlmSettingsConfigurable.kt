package io.github.trichogaster.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import io.github.trichogaster.i18n.TestGapBundle
import javax.swing.JComponent
import javax.swing.JPanel

class LlmSettingsConfigurable : Configurable {
    private val apiBaseUrlField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val modelNameField = JBTextField()
    private var panel: JPanel? = null

    override fun getDisplayName(): String {
        return TestGapBundle.message("settings.llm.displayName")
    }

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(
                    TestGapBundle.message("settings.llm.apiBaseUrl"),
                    apiBaseUrlField,
                    1,
                    false
                )
                .addLabeledComponent(
                    TestGapBundle.message("settings.llm.apiKey"),
                    apiKeyField,
                    1,
                    false
                )
                .addLabeledComponent(
                    TestGapBundle.message("settings.llm.modelName"),
                    modelNameField,
                    1,
                    false
                )
                .addComponentFillVertically(JPanel(), 0)
                .panel
        }

        return panel!!
    }

    override fun isModified(): Boolean {
        val state = LlmSettingsState.getInstance().state
        return apiBaseUrlField.text.trim() != state.apiBaseUrl ||
            String(apiKeyField.password).trim() != state.apiKey ||
            modelNameField.text.trim() != state.modelName
    }

    override fun apply() {
        LlmSettingsState.getInstance().update(
            apiBaseUrl = apiBaseUrlField.text,
            apiKey = String(apiKeyField.password),
            modelName = modelNameField.text
        )
    }

    override fun reset() {
        val state = LlmSettingsState.getInstance().state
        apiBaseUrlField.text = state.apiBaseUrl
        apiKeyField.text = state.apiKey
        modelNameField.text = state.modelName
    }

    override fun disposeUIResources() {
        panel = null
    }
}

