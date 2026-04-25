package io.github.trichogaster

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod

object TestGapMethodPresentation {
    fun showExtractedMethodInfo(project: Project, method: PsiMethod) {
        val containingClass = method.containingClass
        val className = method.containingClass?.qualifiedName
            ?: method.containingClass?.name
            ?: "<unknown-class>"
        val methodName = method.name
        val methodSignature = buildMethodSignature(method)
        val methodBodyText = method.body?.text ?: "<no-body>"
        val matchedTestClassName = containingClass
            ?.let { TestGapTestClassFinder.findMatchingTestClass(project, it) }
            ?.let { it.qualifiedName ?: it.name }

        TestGapToolWindowPresenter.showMockResult(
            project = project,
            className = className,
            methodName = methodName,
            methodSignature = methodSignature,
            methodBodyText = methodBodyText,
            matchedTestClassName = matchedTestClassName
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

