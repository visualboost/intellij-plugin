import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.ProjectDirectories
import java.io.File
import java.net.URL
import java.nio.file.Paths

fun File.openInEditor(project: Project, focus: Boolean = false){
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

fun File.setLineSeparatorToLF(): File{
    if(this.exists().not()) return this

    val adaptedFileContent = this.readText().replace("\r\n", "\n").replace("\r", "\n")
    this.writeText(adaptedFileContent)
    return this
}

fun VirtualFile.toFile(): File{
    val absolutePath = this.url.removePrefix("file://")
    return File(absolutePath)
}

fun VirtualFile.isJavascriptFile(): Boolean{
    return this.extension == "js"
}

fun VirtualFile.isVbExtension(project: Project): Boolean{
    val extensionDirPath = VbProjectSettings.getInstance(project).extensionDirectory ?: return false
    val extensionDir = ProjectDirectories.getExtensionDir(project, extensionDirPath) ?: return false
    val currentFile = this.toFile()
    return currentFile.absolutePath.contains(extensionDir.absolutePath)
}