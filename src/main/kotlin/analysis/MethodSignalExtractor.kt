package io.github.trichogaster.analysis

import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.util.PsiTreeUtil

object MethodSignalExtractor {
    fun extractSignals(method: PsiMethod): List<String> {
        val methodCode = method.body?.text.orEmpty()
        val signals = mutableListOf<String>()

        if (containsNullCheck(methodCode)) {
            signals.add("contains null check")
        }

        if (throwsIllegalArgumentException(method, methodCode)) {
            signals.add("throws IllegalArgumentException")
        }

        val boundaryConstants = extractBoundaryConstants(method)
        if (boundaryConstants.isNotEmpty()) {
            signals.add("has boundary-like constants: ${boundaryConstants.joinToString(", ")}")
        }

        if (methodCode.contains(".orElseThrow(")) {
            signals.add("uses Optional.orElseThrow")
        }

        if (containsBranching(method, methodCode)) {
            signals.add("contains branching")
        }

        if (returnsCollection(method)) {
            signals.add("returns collection")
        }

        return signals
    }

    private fun containsNullCheck(methodCode: String): Boolean {
        return methodCode.contains("== null") ||
                methodCode.contains("!= null") ||
                methodCode.contains("Objects.requireNonNull")
    }

    private fun throwsIllegalArgumentException(method: PsiMethod, methodCode: String): Boolean {
        val declaredException = method.throwsList.referencedTypes.any {
            it.className == "IllegalArgumentException"
        }

        return declaredException || methodCode.contains("IllegalArgumentException")
    }

    private fun extractBoundaryConstants(method: PsiMethod): List<String> {
        val literals = PsiTreeUtil.findChildrenOfType(method, PsiLiteralExpression::class.java)
        return literals
            .mapNotNull { literal ->
                when (val value = literal.value) {
                    is Int, is Long, is Float, is Double, is Short, is Byte -> value.toString()
                    else -> null
                }
            }
            .distinct()
            .take(6)
    }

    private fun containsBranching(method: PsiMethod, methodCode: String): Boolean {
        val hasIf = PsiTreeUtil.findChildOfType(method, PsiIfStatement::class.java) != null
        val hasSwitch = PsiTreeUtil.findChildOfType(method, PsiSwitchStatement::class.java) != null
        val hasTernary = methodCode.contains("?") && methodCode.contains(":")

        return hasIf || hasSwitch || hasTernary
    }

    private fun returnsCollection(method: PsiMethod): Boolean {
        val returnType = method.returnType?.canonicalText ?: return false
        return returnType.startsWith("java.util.Collection") ||
                returnType.startsWith("java.util.List") ||
                returnType.startsWith("java.util.Set") ||
                returnType.startsWith("java.util.Map") ||
                returnType.startsWith("java.lang.Iterable")
    }
}

