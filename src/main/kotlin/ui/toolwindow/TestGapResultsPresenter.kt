package io.github.trichogaster.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import io.github.trichogaster.analysis.MethodLlmInput
import io.github.trichogaster.discovery.TestMethodDescriptor
import io.github.trichogaster.i18n.TestGapBundle

object TestGapResultsPresenter {
    fun showMockResult(
        project: Project,
        llmInput: MethodLlmInput,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TestGapResultsPanel.TOOL_WINDOW_ID)
            ?: return

        val panel = toolWindow.contentManager.contents
            .asSequence()
            .mapNotNull { it.component as? TestGapResultsPanel }
            .firstOrNull()
            ?: return

        panel.render(
            buildMockOutput(
                llmInput = llmInput,
                matchedTestClassName = matchedTestClassName,
                extractedTestMethods = extractedTestMethods
            )
        )

        toolWindow.show()
        toolWindow.activate(null)
    }

    private fun buildMockOutput(
        llmInput: MethodLlmInput,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ): String {
        val bodyPreview = llmInput.methodCode.take(240)
        val truncatedSuffix = if (llmInput.methodCode.length > 240) "..." else ""
        val testContextLine = if (matchedTestClassName != null) {
            TestGapBundle.message("toolWindow.mock.testClassFound", matchedTestClassName)
        } else {
            TestGapBundle.message("toolWindow.mock.testClassMissing")
        }
        val existingTestMethodsBlock = buildExistingTestMethodsBlock(
            matchedTestClassName = matchedTestClassName,
            extractedTestMethods = extractedTestMethods
        )
        val llmTestNamesBlock = buildSimpleListBlock(
            values = llmInput.testNames,
            emptyMessageKey = "toolWindow.mock.llmInput.noTestNames"
        )
        val llmSignalsBlock = buildSimpleListBlock(
            values = llmInput.extractedSignals,
            emptyMessageKey = "toolWindow.mock.llmInput.noSignals"
        )

        return """
            ${TestGapBundle.message("toolWindow.mock.section.methodSummary")}
            - Class: ${llmInput.className}
            - Method: ${llmInput.methodName}
            - Signature: ${llmInput.methodSignature}
            - Body preview: $bodyPreview$truncatedSuffix
            - $testContextLine

            ${TestGapBundle.message("toolWindow.mock.section.existingTests")}
            $existingTestMethodsBlock

            ${TestGapBundle.message("toolWindow.mock.section.llmInput")}
            - ${TestGapBundle.message("toolWindow.mock.llmInput.className")}: ${llmInput.className}
            - ${TestGapBundle.message("toolWindow.mock.llmInput.methodName")}: ${llmInput.methodName}
            - ${TestGapBundle.message("toolWindow.mock.llmInput.methodSignature")}: ${llmInput.methodSignature}
            - ${TestGapBundle.message("toolWindow.mock.llmInput.methodCodeLength")}: ${llmInput.methodCode.length}
            - ${TestGapBundle.message("toolWindow.mock.llmInput.testNames")}
$llmTestNamesBlock
            - ${TestGapBundle.message("toolWindow.mock.llmInput.extractedSignals")}
$llmSignalsBlock
            
            ${TestGapBundle.message("toolWindow.mock.section.suggestedScenarios")}
            - Happy path with valid inputs
            - Edge case: empty or minimal input values
            - Invalid input should fail fast with clear error
            - Exception path from dependency failure
            
            ${TestGapBundle.message("toolWindow.mock.section.likelyMissingTests")}
            - Should reject null input with IllegalArgumentException
            - Should handle boundary value at min/max limits
            - Should propagate dependency timeout as domain exception
        """.trimIndent()
    }

    private fun buildExistingTestMethodsBlock(
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ): String {
        if (matchedTestClassName == null) {
            return "- ${TestGapBundle.message("toolWindow.mock.testMethods.notAvailable")}"
        }

        if (extractedTestMethods.isEmpty()) {
            return "- ${TestGapBundle.message("toolWindow.mock.testMethods.noneInMatchedClass")}"
        }

        return extractedTestMethods.joinToString("\n") { testMethod ->
            val displayName = testMethod.displayName
            if (displayName.isNullOrBlank()) {
                "- ${testMethod.methodName}"
            } else {
                "- ${testMethod.methodName} (${TestGapBundle.message("toolWindow.mock.displayNameLabel")}: $displayName)"
            }
        }
    }

    private fun buildSimpleListBlock(values: List<String>, emptyMessageKey: String): String {
        if (values.isEmpty()) {
            return "  - ${TestGapBundle.message(emptyMessageKey)}"
        }

        return values.joinToString("\n") { "  - $it" }
    }
}




