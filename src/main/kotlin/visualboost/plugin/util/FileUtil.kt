import com.intellij.ide.projectView.impl.PsiFileUrl
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.ProjectDirectories
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

fun File.openInEditor(project: Project, focus: Boolean = false) {
    val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(this) ?: return
    FileEditorManager.getInstance(project).openFile(file, focus)
}

fun File.create(): File {
    val parent = this.parentFile

    if (parent.exists().not()) {
        parent.mkdirs()
    }

    if (this.exists().not()) {
        this.createNewFile()
    }
    return this
}

fun File.setLineSeparatorToLF(): File {
    if (this.exists().not()) return this

    val adaptedFileContent = this.readText().replace("\r\n", "\n").replace("\r", "\n")
    this.writeText(adaptedFileContent)
    return this
}

fun VirtualFile.toFile(): File {
    val absolutePath = this.url.removePrefix("file://")
    return File(absolutePath)
}

fun VirtualFile.isJavascriptFile(): Boolean {
    return this.extension == "js"
}

/**
 * Validate whether the file is inside the extension directory
 */
fun VirtualFile.isVbExtension(project: Project): Boolean {
    val extensionDirPath =
        Path(VbProjectSettings.getInstance(project).extensionDirPath ?: return false).invariantSeparatorsPathString
    val extensionDir = ProjectDirectories.getExtensionDir(project, extensionDirPath) ?: return false
    val currentFilePath = this.toFile().invariantSeparatorsPath
    return currentFilePath.contains(extensionDir.invariantSeparatorsPath)
}

fun virtualFileFromAction(e: AnActionEvent): VirtualFile? {
    return runReadAction {
        val selectedElement: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return@runReadAction null
        return@runReadAction selectedElement
    }
}

fun selectedNodesFromAction(e: AnActionEvent): List<Any> {
    return e.getData(PlatformCoreDataKeys.SELECTED_ITEMS)?.toList() ?: emptyList()
}

fun selectedFilesFromAction(e: AnActionEvent): List<PsiFileNode> {
    return selectedNodesFromAction(e).filterIsInstance<PsiFileNode>()
}

fun selectedDirectoriesFromAction(e: AnActionEvent): List<PsiDirectoryNode> {
    return selectedNodesFromAction(e).filterIsInstance<PsiDirectoryNode>()
}

///**
// * Validate if the content of virtual file really changed.
// * Means if the document content and the physical file content differs.
// */
//fun VirtualFile.contentChanged(): Boolean{
//    val fileDocumentManager = FileDocumentManager.getInstance()
//    val document = fileDocumentManager.getDocument(this) ?: return false
//
//    val docText = document.text
//    val file = this.toFile()
//
//    if(file.exists().not()) return
//
//    val savedText
//
//}