package io.github.trichogaster.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import io.github.trichogaster.discovery.TestMethodDescriptor
import io.github.trichogaster.i18n.TestGapBundle

object TestGapResultsPresenter {
    fun showMockResult(
        project: Project,
        className: String,
        methodName: String,
        methodSignature: String,
        methodBodyText: String,
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
                className = className,
                methodName = methodName,
                methodSignature = methodSignature,
                methodBodyText = methodBodyText,
                matchedTestClassName = matchedTestClassName,
                extractedTestMethods = extractedTestMethods
            )
        )

        toolWindow.show()
        toolWindow.activate(null)
    }

    private fun buildMockOutput(
        className: String,
        methodName: String,
        methodSignature: String,
        methodBodyText: String,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestMethodDescriptor>
    ): String {
        val bodyPreview = methodBodyText.take(240)
        val truncatedSuffix = if (methodBodyText.length > 240) "..." else ""
        val testContextLine = if (matchedTestClassName != null) {
            TestGapBundle.message("toolWindow.mock.testClassFound", matchedTestClassName)
        } else {
            TestGapBundle.message("toolWindow.mock.testClassMissing")
        }
        val existingTestMethodsBlock = buildExistingTestMethodsBlock(
            matchedTestClassName = matchedTestClassName,
            extractedTestMethods = extractedTestMethods
        )

        return """
            ${TestGapBundle.message("toolWindow.mock.section.methodSummary")}
            - Class: $className
            - Method: $methodName
            - Signature: $methodSignature
            - Body preview: $bodyPreview$truncatedSuffix
            - $testContextLine

            ${TestGapBundle.message("toolWindow.mock.section.existingTests")}
            $existingTestMethodsBlock
            
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
}




