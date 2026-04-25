package io.github.trichogaster.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import io.github.trichogaster.analysis.MethodAnalysisCoordinator
import io.github.trichogaster.i18n.TestGapBundle

class MethodTestGapLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null

        val method = element.parent as? PsiMethod ?: return null
        if (method.nameIdentifier != element) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Actions.Lightning,
            { TestGapBundle.message("action.findLikelyTestGaps.gutter.tooltip") },
            { _, clickedElement ->
                val clickedMethod = clickedElement.parent as? PsiMethod
                if (clickedMethod != null) {
                    MethodAnalysisCoordinator.showExtractedMethodInfo(clickedElement.project, clickedMethod)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { TestGapBundle.message("action.findLikelyTestGaps.gutter.accessibleName") }
        )
    }
}




