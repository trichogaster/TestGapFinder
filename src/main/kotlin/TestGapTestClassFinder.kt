package io.github.trichogaster

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
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
}

