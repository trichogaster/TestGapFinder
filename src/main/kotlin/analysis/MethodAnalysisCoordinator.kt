package io.github.trichogaster.analysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import io.github.trichogaster.discovery.TestDiscoveryService
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

        TestGapResultsPresenter.showMockResult(
            project = project,
            llmInput = llmInput,
            matchedTestClassName = matchedTestClassName,
            extractedTestMethods = extractedTestMethods
        )
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


