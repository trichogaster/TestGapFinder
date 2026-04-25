package io.github.trichogaster.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import io.github.trichogaster.analysis.MethodLlmInput
import io.github.trichogaster.discovery.TestMethodDescriptor
import io.github.trichogaster.i18n.TestGapBundle
import io.github.trichogaster.llm.LlmAnalysisResult
import io.github.trichogaster.llm.LlmCoverageItem

object TestGapResultsPresenter {
    fun showStatus(project: Project, message: String) {
        val panel = getPanel(project) ?: return
        panel.render(message)
        showToolWindow(project)
    }

    fun showAnalysisResult(
        project: Project,
        llmInput: MethodLlmInput,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>,
        analysisResult: LlmAnalysisResult
    ) {
        val panel = getPanel(project) ?: return
        panel.render(
            buildAnalysisOutput(
                llmInput = llmInput,
                matchedTestClassName = matchedTestClassName,
                extractedTestMethods = extractedTestMethods,
                analysisResult = analysisResult
            )
        )

        showToolWindow(project)
    }

    private fun getPanel(project: Project): TestGapResultsPanel? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TestGapResultsPanel.TOOL_WINDOW_ID)
            ?: return null

        return toolWindow.contentManager.contents
            .asSequence()
            .mapNotNull { it.component as? TestGapResultsPanel }
            .firstOrNull()
    }

    private fun showToolWindow(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TestGapResultsPanel.TOOL_WINDOW_ID)
            ?: return

        toolWindow.show()
        toolWindow.activate(null)
    }

    private fun buildAnalysisOutput(
        llmInput: MethodLlmInput,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>,
        analysisResult: LlmAnalysisResult
    ): String {
        val bodyPreview = llmInput.methodCode.take(240)
        val truncatedSuffix = if (llmInput.methodCode.length > 240) "..." else ""
        val testContextLine = if (matchedTestClassName != null) {
            TestGapBundle.message("toolWindow.analysis.testClassFound", matchedTestClassName)
        } else {
            TestGapBundle.message("toolWindow.analysis.testClassMissing")
        }
        val existingTestMethodsBlock = buildExistingTestMethodsBlock(
            matchedTestClassName = matchedTestClassName,
            extractedTestMethods = extractedTestMethods
        )
        val scenariosBlock = if (analysisResult.scenarios.isEmpty()) {
            "- ${TestGapBundle.message("toolWindow.analysis.noScenarios")}"
        } else {
            analysisResult.scenarios.joinToString("\n") { scenario ->
                "- ${scenario.title} [${scenario.category}] [${scenario.priority}] - ${scenario.rationale}"
            }
        }

        return """
            ${TestGapBundle.message("toolWindow.analysis.section.methodSummary")}
            - Class: ${llmInput.className}
            - Method: ${llmInput.methodName}
            - Signature: ${llmInput.methodSignature}
            - Body preview: $bodyPreview$truncatedSuffix
            - $testContextLine

            ${TestGapBundle.message("toolWindow.analysis.section.existingTests")}
            $existingTestMethodsBlock

            ${TestGapBundle.message("toolWindow.analysis.section.summary")}
            - ${analysisResult.summary}

            ${TestGapBundle.message("toolWindow.analysis.section.scenarios")}
            $scenariosBlock

            ${TestGapBundle.message("toolWindow.analysis.section.coverage")}
            ${TestGapBundle.message("toolWindow.analysis.coverage.likelyCovered")}
            ${buildCoverageBlock(analysisResult.coverageAssessment.likelyCovered)}
            ${TestGapBundle.message("toolWindow.analysis.coverage.possiblyCovered")}
            ${buildCoverageBlock(analysisResult.coverageAssessment.possiblyCovered)}
            ${TestGapBundle.message("toolWindow.analysis.coverage.likelyMissing")}
            ${buildCoverageBlock(analysisResult.coverageAssessment.likelyMissing)}
        """.trimIndent()
    }

    private fun buildCoverageBlock(items: List<LlmCoverageItem>): String {
        if (items.isEmpty()) {
            return "- ${TestGapBundle.message("toolWindow.analysis.coverage.none")}"
        }

        return items.joinToString("\n") { item ->
            val matchedTests = if (item.matchedTests.isEmpty()) "[]" else item.matchedTests.joinToString(", ")
            "- ${item.title} (matchedTests: $matchedTests)"
        }
    }

    private fun buildExistingTestMethodsBlock(
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ): String {
        if (matchedTestClassName == null) {
            return "- ${TestGapBundle.message("toolWindow.analysis.testMethods.notAvailable")}" 
        }

        if (extractedTestMethods.isEmpty()) {
            return "- ${TestGapBundle.message("toolWindow.analysis.testMethods.noneInMatchedClass")}" 
        }

        return extractedTestMethods.joinToString("\n") { testMethod ->
            val displayName = testMethod.displayName
            if (displayName.isNullOrBlank()) {
                "- ${testMethod.methodName}"
            } else {
                "- ${testMethod.methodName} (${TestGapBundle.message("toolWindow.analysis.displayNameLabel")}: $displayName)"
            }
        }
    }
}




