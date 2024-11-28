package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import selectedDirectoriesFromAction
import selectedFilesFromAction
import virtualFileFromAction
import visualboost.plugin.util.ProjectDirectories
import visualboost.plugin.util.getOrCreateVbIgnoreFile
import visualboost.plugin.util.showInfo
import java.io.File
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo

/**
 * Adds a file to the '.vbignore' file
 */
class AddFileToVbIgnoreAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectDir =
            ProjectDirectories.getProjectDir(project)


        val vbIgnoreFile = project.getOrCreateVbIgnoreFile() ?: throw NullPointerException("Can't create or get '.vbignore' file")
        val vbIgnoreLines = vbIgnoreFile.readLines()

        if (vbIgnoreLines.isNotEmpty()) {
            vbIgnoreFile.appendText("\n")
        }


        val selectedFiles = selectedFilesFromAction(e)
        val selectedDirectories = selectedDirectoriesFromAction(e)
        (selectedFiles + selectedDirectories).forEach {
            //Determine the relative path
            val selectedFile = it.virtualFile ?: return@forEach
            val relativePath = selectedFile.toNioPath().relativeTo(projectDir.toPath())

            //Check if file is already ignored
            val fileAlreadyIgnored = vbIgnoreLines.any { it == relativePath.invariantSeparatorsPathString }
            if (fileAlreadyIgnored) {
                project.showInfo("File Already Ignored", "The selected file has already been marked as ignored. No further action is needed.")
                return@forEach
            }

            vbIgnoreFile.appendText(relativePath.invariantSeparatorsPathString + "\n")
        }

        //Open .vbignore
        val virtualVbIgnore = VirtualFileManager.getInstance().findFileByNioPath(vbIgnoreFile.toPath()) ?: return
        virtualVbIgnore.refresh(true, true)
        FileEditorManager.getInstance(project).openFile(virtualVbIgnore, true)
    }

//    override fun update(e: AnActionEvent) {
//        val selectedFiles = selectedFilesFromAction(e)
//
//        var selectedFile = virtualFileFromAction(e) ?: return
//
//        if(selectedFiles.size == 1){
//
//        }
//
//        selectedFiles.forEach{
//            it.virtualFile.path
//        }
//
//        if(selectedFile.isDirectory){
//            "Ignore directory".also { e.presentation.text = it }
//        }else if(selectedFile.isFile){
//            "Ignore file".also { e.presentation.text = it }
//        }
//    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }


}