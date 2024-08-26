package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction
import visualboost.plugin.VbWindowService


class ToolBarLabel : ToolbarLabelAction() {

    override fun actionPerformed(e: AnActionEvent) {}

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.text = "VisualBoost:"
    }



}