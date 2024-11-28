package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import virtualFileFromAction
import visualboost.plugin.dialog.ConfirmPushVbignoreDialog
import visualboost.plugin.tasks.abs.AsyncTaskQueue
import visualboost.plugin.util.*
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Commit and push the .vbignore file
 */
class CommitAndPushVbIgnoreAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val vbIgnoreFile = project.getOrCreateVbIgnoreFile()
        val vbIgnoreFileIsTracked = project.isTracked(vbIgnoreFile)

        val vbIgnoreHasDiffs = project.hasDiffs(vbIgnoreFile)
        //TODO: check if vbignore is completely new: maybe use git ls-files

        if(vbIgnoreFileIsTracked && !vbIgnoreHasDiffs){
            project.showInfo("No changes detected", "The file .vbignore does not contain any changes.")
            return
        }

        val dialogWrapper = ConfirmPushVbignoreDialog()
        dialogWrapper.showAndGet()

        //Cancel is pressed
        if (dialogWrapper.exitCode != DialogWrapper.OK_EXIT_CODE) return
        //TODO: add steps

        //1. unstage all files
        //2. stage .vbignore
        //3. commit
        //4. push

        object : AsyncTaskQueue(project) {
            init {
                //Set default event handler that logs success and errors
                useDefaultEventHandler()

                register { project, indicator, logger, resultOfPreexecutesTasks ->
                    indicator.text = "Unstage all files"
                    project.unstage()

                    indicator.text = "Staging file .vbignore"
                    val vbIgnoreFile = project.getOrCreateVbIgnoreFile()
                    project.stage(vbIgnoreFile)

                    indicator.text = "Commit .vbignore"
                    project.commit("Adapted .vbignore")

                    indicator.text = "Push .vbignore"
                    project.pushCurrentBranch()

                    null
                }
            }
        }.execute()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val selectedFile = virtualFileFromAction(e)

        if (project == null || selectedFile == null) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.isEnabled = project.isVbignoreFile(selectedFile)

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}