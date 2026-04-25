package io.github.trichogaster.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import io.github.trichogaster.analysis.MethodLlmInput
import io.github.trichogaster.discovery.TestMethodDescriptor
import io.github.trichogaster.i18n.TestGapBundle
import io.github.trichogaster.llm.LlmAnalysisResult
import io.github.trichogaster.llm.LlmCoverageItem

object TestGapResultsPresenter {
    fun showStatus(
        project: Project,
        message: String,
        statusType: TestGapResultsPanel.StatusType = TestGapResultsPanel.StatusType.INFO
    ) {
        val panel = getPanel(project) ?: return
        panel.renderStatus(message, statusType)
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
        val checklistItems = analysisResult.scenarios.map { it.title }
        panel.renderAnalysis(
            htmlContent = buildAnalysisOutputHtml(
                llmInput = llmInput,
                matchedTestClassName = matchedTestClassName,
                extractedTestMethods = extractedTestMethods,
                analysisResult = analysisResult
            ),
            checklistItems = checklistItems
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

    private fun buildAnalysisOutputHtml(
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
            "<li>${escapeHtml(TestGapBundle.message("toolWindow.analysis.noScenarios"))}</li>"
        } else {
            analysisResult.scenarios.joinToString("\n") { scenario ->
                val coverage = findScenarioCoverage(scenario.title, analysisResult)
                val coverageLabel = coverage?.first ?: TestGapBundle.message("toolWindow.analysis.coverage.none")
                val coverageMarker = coverageMarker(coverageLabel)
                val matchedTests = coverage?.second?.matchedTests.orEmpty()
                val matchedTestsText = if (matchedTests.isEmpty()) {
                    "[]"
                } else {
                    matchedTests.joinToString(", ") { escapeHtml(it) }
                }

                """
                    <li>
                      <b>${escapeHtml(scenario.title)}</b><br/>
                      <span><b>$coverageMarker ${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.coverage"))}</b>: ${escapeHtml(coverageLabel)}</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.category"))}: ${escapeHtml(scenario.category)}</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.priority"))}: ${escapeHtml(scenario.priority)}</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.rationale"))}: ${escapeHtml(scenario.rationale)}</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.matchedTests"))}: $matchedTestsText</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.confidence"))}: ${escapeHtml(scenario.confidence)}</span><br/>
                      <span>${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.assumption"))}: ${escapeHtml(scenario.assumption.ifBlank { "-" })}</span>
                    </li>
                """.trimIndent()
            }
        }

        return """
            <html>
              <body style="font-family:Segoe UI, Arial, sans-serif; font-size:12px; margin:8px;">
                <h2>${escapeHtml(TestGapBundle.message("toolWindow.analysis.section.methodSummary"))}</h2>
                <ul>
                  <li><b>Class</b>: ${escapeHtml(llmInput.className)}</li>
                  <li><b>Method</b>: ${escapeHtml(llmInput.methodName)}</li>
                  <li><b>Signature</b>: ${escapeHtml(llmInput.methodSignature)}</li>
                  <li><b>Body preview</b>: ${escapeHtml(bodyPreview + truncatedSuffix)}</li>
                  <li>${escapeHtml(testContextLine)}</li>
                </ul>

                <h2>${escapeHtml(TestGapBundle.message("toolWindow.analysis.section.existingTests"))}</h2>
                <ul>
                  $existingTestMethodsBlock
                </ul>

                <h2>${escapeHtml(TestGapBundle.message("toolWindow.analysis.section.summary"))}</h2>
                <p>${escapeHtml(analysisResult.summary)}</p>

                <h2>${escapeHtml(TestGapBundle.message("toolWindow.analysis.section.scenarios"))}</h2>
                <ul>
                  $scenariosBlock
                </ul>

                <h2>${escapeHtml(TestGapBundle.message("toolWindow.analysis.section.coverage"))}</h2>
                <h3>$MARKER_LIKELY_COVERED ${escapeHtml(TestGapBundle.message("toolWindow.analysis.coverage.likelyCovered"))}</h3>
                <ul>${buildCoverageBlock(analysisResult.coverageAssessment.likelyCovered)}</ul>
                <h3>$MARKER_POSSIBLY_COVERED ${escapeHtml(TestGapBundle.message("toolWindow.analysis.coverage.possiblyCovered"))}</h3>
                <ul>${buildCoverageBlock(analysisResult.coverageAssessment.possiblyCovered)}</ul>
                <h3>$MARKER_LIKELY_MISSING ${escapeHtml(TestGapBundle.message("toolWindow.analysis.coverage.likelyMissing"))}</h3>
                <ul>${buildCoverageBlock(analysisResult.coverageAssessment.likelyMissing)}</ul>
              </body>
            </html>
        """.trimIndent()
    }

    private fun buildCoverageBlock(items: List<LlmCoverageItem>): String {
        if (items.isEmpty()) {
            return "<li>${escapeHtml(TestGapBundle.message("toolWindow.analysis.coverage.none"))}</li>"
        }

        return items.joinToString("\n") { item ->
            val matchedTests = if (item.matchedTests.isEmpty()) "[]" else item.matchedTests.joinToString(", ")
            "<li>${escapeHtml(item.title)} (${escapeHtml(TestGapBundle.message("toolWindow.analysis.field.matchedTests"))}: ${escapeHtml(matchedTests)})</li>"
        }
    }

    private fun buildExistingTestMethodsBlock(
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ): String {
        if (matchedTestClassName == null) {
            return "<li>${escapeHtml(TestGapBundle.message("toolWindow.analysis.testMethods.notAvailable"))}</li>"
        }

        if (extractedTestMethods.isEmpty()) {
            return "<li>${escapeHtml(TestGapBundle.message("toolWindow.analysis.testMethods.noneInMatchedClass"))}</li>"
        }

        return extractedTestMethods.joinToString("\n") { testMethod ->
            val displayName = testMethod.displayName
            if (displayName.isNullOrBlank()) {
                "<li>${escapeHtml(testMethod.methodName)}</li>"
            } else {
                "<li>${escapeHtml(testMethod.methodName)} (${escapeHtml(TestGapBundle.message("toolWindow.analysis.displayNameLabel"))}: ${escapeHtml(displayName)})</li>"
            }
        }
    }

    private fun findScenarioCoverage(
        scenarioTitle: String,
        analysisResult: LlmAnalysisResult
    ): Pair<String, LlmCoverageItem>? {
        analysisResult.coverageAssessment.likelyCovered.firstOrNull {
            it.title.equals(scenarioTitle, ignoreCase = true)
        }?.let { return TestGapBundle.message("toolWindow.analysis.coverage.likelyCovered") to it }

        analysisResult.coverageAssessment.possiblyCovered.firstOrNull {
            it.title.equals(scenarioTitle, ignoreCase = true)
        }?.let { return TestGapBundle.message("toolWindow.analysis.coverage.possiblyCovered") to it }

        analysisResult.coverageAssessment.likelyMissing.firstOrNull {
            it.title.equals(scenarioTitle, ignoreCase = true)
        }?.let { return TestGapBundle.message("toolWindow.analysis.coverage.likelyMissing") to it }

        return null
    }

    private fun coverageMarker(coverageLabel: String): String {
        return when (coverageLabel) {
            TestGapBundle.message("toolWindow.analysis.coverage.likelyCovered") -> MARKER_LIKELY_COVERED
            TestGapBundle.message("toolWindow.analysis.coverage.possiblyCovered") -> MARKER_POSSIBLY_COVERED
            TestGapBundle.message("toolWindow.analysis.coverage.likelyMissing") -> MARKER_LIKELY_MISSING
            else -> MARKER_UNKNOWN
        }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private const val MARKER_LIKELY_COVERED = "\u2705"
    private const val MARKER_POSSIBLY_COVERED = "\u26A0\uFE0F"
    private const val MARKER_LIKELY_MISSING = "\u274C"
    private const val MARKER_UNKNOWN = "\u2022"
}




