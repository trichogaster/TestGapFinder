package io.github.trichogaster

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class TestGapToolWindowPanel : JPanel(BorderLayout()) {
    private val outputArea = JBTextArea()

    init {
        border = JBUI.Borders.empty(10)

        outputArea.isEditable = false
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true
        outputArea.text = MyMessageBundle.message("toolWindow.mock.placeholder")

        add(JBScrollPane(outputArea), BorderLayout.CENTER)
    }

    fun render(resultText: String) {
        outputArea.text = resultText
        outputArea.caretPosition = 0
    }

    companion object {
        const val TOOL_WINDOW_ID = "Test Gap Finder"
    }
}


