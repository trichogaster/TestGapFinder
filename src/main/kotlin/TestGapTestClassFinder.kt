package io.github.trichogaster

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

object TestGapTestClassFinder {
    fun findMatchingTestClass(project: Project, productionClass: PsiClass): PsiClass? {
        val productionClassName = productionClass.name ?: return null
        val expectedTestClassNames = listOf(
            "${productionClassName}Test",
            "${productionClassName}Tests"
        )
        val expectedFileNames = expectedTestClassNames.map { "$it.java" }

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val psiManager = PsiManager.getInstance(project)

        for (expectedFileName in expectedFileNames) {
            val candidateFiles = FilenameIndex.getVirtualFilesByName(
                expectedFileName,
                GlobalSearchScope.projectScope(project)
            )

            val testSourceCandidates = candidateFiles.filter { virtualFile ->
                fileIndex.isInTestSourceContent(virtualFile)
            }

            for (virtualFile in testSourceCandidates) {
                val psiJavaFile = psiManager.findFile(virtualFile) as? PsiJavaFile ?: continue
                val matchedClass = psiJavaFile.classes.firstOrNull { psiClass ->
                    psiClass.name in expectedTestClassNames
                }

                if (matchedClass != null) {
                    return matchedClass
                }
            }
        }

        return null
    }

    fun extractTestMethods(testClass: PsiClass): List<TestGapTestMethodInfo> {
        return testClass.methods.mapNotNull { method ->
            if (!isTestMethod(method)) {
                return@mapNotNull null
            }

            TestGapTestMethodInfo(
                methodName = method.name,
                displayName = extractDisplayName(method)
            )
        }
    }

    private fun isTestMethod(method: PsiMethod): Boolean {
        return method.modifierList.annotations.any { annotation ->
            val qualifiedName = annotation.qualifiedName
            qualifiedName == "org.junit.Test" ||
                qualifiedName == "org.junit.jupiter.api.Test" ||
                qualifiedName?.endsWith(".Test") == true ||
                annotation.nameReferenceElement?.referenceName == "Test"
        }
    }

    private fun extractDisplayName(method: PsiMethod): String? {
        val displayNameAnnotation = method.modifierList.annotations.firstOrNull { annotation ->
            val qualifiedName = annotation.qualifiedName
            qualifiedName == "org.junit.jupiter.api.DisplayName" ||
                qualifiedName?.endsWith(".DisplayName") == true ||
                annotation.nameReferenceElement?.referenceName == "DisplayName"
        } ?: return null

        val value = displayNameAnnotation.findDeclaredAttributeValue("value")
            ?: displayNameAnnotation.parameterList.attributes.firstOrNull()?.value
            ?: return null

        return readAnnotationStringValue(value)
    }

    private fun readAnnotationStringValue(value: PsiAnnotationMemberValue): String? {
        val literalExpression = value as? PsiLiteralExpression ?: return value.text
        return literalExpression.value as? String ?: value.text
    }
}

