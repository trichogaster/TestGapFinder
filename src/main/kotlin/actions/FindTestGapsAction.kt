package io.github.trichogaster.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import io.github.trichogaster.analysis.MethodAnalysisCoordinator
import io.github.trichogaster.i18n.TestGapBundle

class FindTestGapsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: run {
            Messages.showErrorDialog(
                TestGapBundle.message("action.findLikelyTestGaps.error.notInJavaMethod"),
                TestGapBundle.message("action.findLikelyTestGaps.title")
            )
            return
        }

        val methodFromElement = resolveMethodFromElement(e.getData(CommonDataKeys.PSI_ELEMENT))
        val method = methodFromElement ?: resolveMethodFromCaret(e)

        if (method == null) {
            Messages.showErrorDialog(
                project,
                TestGapBundle.message("action.findLikelyTestGaps.error.notInJavaMethod"),
                TestGapBundle.message("action.findLikelyTestGaps.title")
            )
            return
        }

        MethodAnalysisCoordinator.showExtractedMethodInfo(project, method)
    }

    private fun resolveMethodFromElement(element: PsiElement?): PsiMethod? {
        return PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)
    }

    private fun resolveMethodFromCaret(e: AnActionEvent): PsiMethod? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return null
        val elementAtCaret = psiFile.findElementAt(editor.caretModel.offset)

        return PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod::class.java, false)
    }

}


