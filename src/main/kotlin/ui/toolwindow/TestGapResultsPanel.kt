package io.github.trichogaster.ui.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.trichogaster.i18n.TestGapBundle
import java.awt.datatransfer.StringSelection
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JProgressBar

class TestGapResultsPanel : JPanel(BorderLayout()) {
    private val titleLabel = JBLabel(TestGapBundle.message("action.findLikelyTestGaps.title"), AllIcons.General.Information, JBLabel.LEFT)
    private val progressBar = JProgressBar()
    private val copyChecklistButton = JButton(TestGapBundle.message("toolWindow.analysis.copyChecklist"))
    private val outputPane = JEditorPane("text/html", "")
    private var checklistItems: List<String> = emptyList()

    init {
        border = JBUI.Borders.empty(10)

        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        headerPanel.isOpaque = false
        headerPanel.add(titleLabel)

        val actionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        actionPanel.isOpaque = false

        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        progressBar.preferredSize = java.awt.Dimension(100, 12)
        actionPanel.add(progressBar)

        copyChecklistButton.isEnabled = false
        copyChecklistButton.addActionListener {
            val checklist = checklistItems.joinToString("\n") { "- [ ] $it" }
            CopyPasteManager.getInstance().setContents(StringSelection(checklist))
            titleLabel.text = TestGapBundle.message("toolWindow.analysis.copyChecklistDone")
        }
        actionPanel.add(copyChecklistButton)

        val topPanel = JPanel(BorderLayout())
        topPanel.isOpaque = false
        topPanel.add(headerPanel, BorderLayout.WEST)
        topPanel.add(actionPanel, BorderLayout.EAST)

        outputPane.isEditable = false
        outputPane.text = toHtml(TestGapBundle.message("toolWindow.analysis.status.ready"))

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(outputPane), BorderLayout.CENTER)
    }

    fun renderStatus(statusText: String, statusType: StatusType) {
        titleLabel.icon = when (statusType) {
            StatusType.INFO -> AllIcons.General.Information
            StatusType.LOADING -> AllIcons.Process.Step_passive
            StatusType.ERROR -> AllIcons.General.Error
        }
        titleLabel.text = TestGapBundle.message("action.findLikelyTestGaps.title")
        progressBar.isVisible = statusType == StatusType.LOADING
        copyChecklistButton.isEnabled = false
        checklistItems = emptyList()
        outputPane.text = toHtml(statusText)
        outputPane.caretPosition = 0
    }

    fun renderAnalysis(htmlContent: String, checklistItems: List<String>) {
        titleLabel.icon = AllIcons.General.InspectionsOK
        titleLabel.text = TestGapBundle.message("action.findLikelyTestGaps.title")
        progressBar.isVisible = false
        this.checklistItems = checklistItems
        copyChecklistButton.isEnabled = checklistItems.isNotEmpty()
        outputPane.text = htmlContent
        outputPane.caretPosition = 0
    }

    private fun toHtml(text: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br/>")

        return """
            <html>
              <body style="font-family:Segoe UI, Arial, sans-serif; font-size:12px; margin:8px;">
                $escaped
              </body>
            </html>
        """.trimIndent()
    }

    companion object {
        const val TOOL_WINDOW_ID = "Test Gap Finder"
    }

    enum class StatusType {
        INFO,
        LOADING,
        ERROR
    }
}



