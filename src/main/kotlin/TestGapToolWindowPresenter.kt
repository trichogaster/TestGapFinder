package io.github.trichogaster

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

object TestGapToolWindowPresenter {
    fun showMockResult(
        project: Project,
        className: String,
        methodName: String,
        methodSignature: String,
        methodBodyText: String,
        matchedTestClassName: String?,
        extractedTestMethods: List<TestGapTestMethodInfo>
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TestGapToolWindowPanel.TOOL_WINDOW_ID)
            ?: return

        val panel = toolWindow.contentManager.contents
            .asSequence()
            .mapNotNull { it.component as? TestGapToolWindowPanel }
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
        extractedTestMethods: List<TestGapTestMethodInfo>
    ): String {
        val bodyPreview = methodBodyText.take(240)
        val truncatedSuffix = if (methodBodyText.length > 240) "..." else ""
        val testContextLine = if (matchedTestClassName != null) {
            MyMessageBundle.message("toolWindow.mock.testClassFound", matchedTestClassName)
        } else {
            MyMessageBundle.message("toolWindow.mock.testClassMissing")
        }
        val existingTestMethodsBlock = buildExistingTestMethodsBlock(
            matchedTestClassName = matchedTestClassName,
            extractedTestMethods = extractedTestMethods
        )

        return """
            ${MyMessageBundle.message("toolWindow.mock.section.methodSummary")}
            - Class: $className
            - Method: $methodName
            - Signature: $methodSignature
            - Body preview: $bodyPreview$truncatedSuffix
            - $testContextLine

            ${MyMessageBundle.message("toolWindow.mock.section.existingTests")}
            $existingTestMethodsBlock
            
            ${MyMessageBundle.message("toolWindow.mock.section.suggestedScenarios")}
            - Happy path with valid inputs
            - Edge case: empty or minimal input values
            - Invalid input should fail fast with clear error
            - Exception path from dependency failure
            
            ${MyMessageBundle.message("toolWindow.mock.section.likelyMissingTests")}
            - Should reject null input with IllegalArgumentException
            - Should handle boundary value at min/max limits
            - Should propagate dependency timeout as domain exception
        """.trimIndent()
    }

    private fun buildExistingTestMethodsBlock(
        matchedTestClassName: String?,
        extractedTestMethods: List<TestGapTestMethodInfo>
    ): String {
        if (matchedTestClassName == null) {
            return "- ${MyMessageBundle.message("toolWindow.mock.testMethods.notAvailable")}"
        }

        if (extractedTestMethods.isEmpty()) {
            return "- ${MyMessageBundle.message("toolWindow.mock.testMethods.noneInMatchedClass")}"
        }

        return extractedTestMethods.joinToString("\n") { testMethod ->
            val displayName = testMethod.displayName
            if (displayName.isNullOrBlank()) {
                "- ${testMethod.methodName}"
            } else {
                "- ${testMethod.methodName} (${MyMessageBundle.message("toolWindow.mock.displayNameLabel")}: $displayName)"
            }
        }
    }
}



