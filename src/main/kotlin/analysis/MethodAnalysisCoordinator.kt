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

        TestGapResultsPresenter.showMockResult(
            project = project,
            className = className,
            methodName = methodName,
            methodSignature = methodSignature,
            methodBodyText = methodBodyText,
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


