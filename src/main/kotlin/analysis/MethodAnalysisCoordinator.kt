package io.github.trichogaster.analysis

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import io.github.trichogaster.discovery.TestDiscoveryService
import io.github.trichogaster.llm.LlmSuggestionService
import io.github.trichogaster.ui.toolwindow.TestGapResultsPresenter

object MethodAnalysisCoordinator {
    fun showExtractedMethodInfo(project: Project, method: PsiMethod) {
        val containingClass = method.containingClass
        val className = method.containingClass?.qualifiedName
            ?: method.containingClass?.name
            ?: "<unknown-class>"
        val methodName = method.name
        val methodSignature = buildMethodSignature(method)
        val methodBodyText = method.body?.text ?: "<no-body>"
        val matchedTestClass = containingClass
            ?.let { TestDiscoveryService.findMatchingTestClass(project, it) }
        val matchedTestClassName = matchedTestClass?.let { it.qualifiedName ?: it.name }
        val extractedTestMethods = matchedTestClass
            ?.let { TestDiscoveryService.extractTestMethods(it) }
            .orEmpty()
        val llmInput = MethodLlmInput(
            className = className,
            methodName = methodName,
            methodSignature = methodSignature,
            methodCode = methodBodyText,
            testNames = extractedTestMethods.map { it.methodName },
            extractedSignals = MethodSignalExtractor.extractSignals(method)
        )
        val llmSuggestionService = LlmSuggestionService.getInstance()

        if (!llmSuggestionService.canCallModel()) {
            TestGapResultsPresenter.showStatus(
                project,
                "LLM settings are incomplete. Open Settings > Tools > Test Gap Finder and configure API Base URL, API Key, and Model Name."
            )

            return
        }

        TestGapResultsPresenter.showStatus(project, "Running LLM analysis...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val analysisResult = llmSuggestionService.suggestTestScenarios(llmInput)

                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) {
                        return@invokeLater
                    }

                    TestGapResultsPresenter.showAnalysisResult(
                        project = project,
                        llmInput = llmInput,
                        matchedTestClassName = matchedTestClassName,
                        extractedTestMethods = extractedTestMethods,
                        analysisResult = analysisResult
                    )
                }
            } catch (t: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) {
                        return@invokeLater
                    }

                    TestGapResultsPresenter.showStatus(
                        project,
                        "LLM analysis failed: ${t.message ?: t.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    private fun buildMethodSignature(method: PsiMethod): String {
        val modifiers = method.modifierList.text
        val returnType = method.returnType?.presentableText ?: "void"
        val parameters = method.parameterList.text

        return listOf(modifiers, returnType, "${method.name}$parameters")
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}


