package io.github.trichogaster

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class TestGapMethodLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null

        val method = element.parent as? PsiMethod ?: return null
        if (method.nameIdentifier != element) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Actions.Lightning,
            { MyMessageBundle.message("action.findLikelyTestGaps.gutter.tooltip") },
            { _, clickedElement ->
                val clickedMethod = clickedElement.parent as? PsiMethod
                if (clickedMethod != null) {
                    TestGapMethodPresentation.showExtractedMethodInfo(clickedElement.project, clickedMethod)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { MyMessageBundle.message("action.findLikelyTestGaps.gutter.accessibleName") }
        )
    }
}



