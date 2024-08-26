package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import visualboost.plugin.api.API
import visualboost.plugin.dialog.IntroductionDialog
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings


class ShowIntroductionDialogAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        showIntroductionDialog()
    }

    private fun showIntroductionDialog() {
        runInEdt {
            IntroductionDialog().show()
        }
    }

}