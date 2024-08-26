package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import create
import kotlinx.coroutines.*
import openInEditor
import visualboost.plugin.VbWindowService
import visualboost.plugin.api.API
import visualboost.plugin.api.models.model.AllModelsResponseBody
import visualboost.plugin.dialog.FileExistDialog
import visualboost.plugin.dialog.ModelsDialog
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.ProjectDirectories
import visualboost.plugin.util.replaceAll
import java.io.File
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo


class ShowCreateExtensionPopupAction : AnAction(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        val rootDir = vbWindow.getRootDir()?.absolutePath ?: return

        launch {
            val jwt = withContext(Dispatchers.IO) {
                API.fetchToken()
            } ?: return@launch

            val projectConfig = withContext(Dispatchers.IO) {
                val settings = VbProjectSettings.getInstance(project)
                API.getProjectConfig(jwt, settings.projectId ?: return@withContext null)
            } ?: return@launch

            val selectedModel = withContext(Dispatchers.Main) {
                val modelsDialog = ModelsDialog(project, jwt)
                modelsDialog.showAndGet()

                val exitCode = modelsDialog.exitCode
                if (exitCode != DialogWrapper.OK_EXIT_CODE) return@withContext null
                modelsDialog.selectedModel ?: return@withContext null
            } ?: return@launch

            val pkgPath = if (selectedModel.pkg != null) {
                val pkg = selectedModel.pkg
                val paths = mutableListOf<String>()
                if (pkg.namespace != null) {
                    pkg.namespace.split(".").forEach { paths.add(it) }
                }
                paths.add(pkg.name)
                paths.filter { it.isNullOrBlank().not() }
            } else {
                emptyList()
            }

            ProjectDirectories

            val extensionDir = Paths.get(
                rootDir,
                "backend",
                "src",
                projectConfig.directories.routesDirs.extension,
                *pkgPath.toTypedArray(),
                "${selectedModel.name}.js"
            )

            val dbDir = Paths.get(
                rootDir,
                "backend",
                "src",
                projectConfig.directories.dbDir,
                *pkgPath.toTypedArray(),
                selectedModel.name
            )

            val relativePath = dbDir.relativeTo(extensionDir.parent).normalize().invariantSeparatorsPathString

            withContext(Dispatchers.Main) {
                val file = extensionDir.toFile()

                if (file.exists()) {
                    val fileExistsDialog = FileExistDialog(extensionDir.invariantSeparatorsPathString)
                    fileExistsDialog.showAndGet()

                    if (fileExistsDialog.exitCode != DialogWrapper.OK_EXIT_CODE) return@withContext
                    createExtensionFile(file, selectedModel, relativePath, project)
                } else {
                    createExtensionFile(file, selectedModel, relativePath, project)
                }
            }
        }
    }

    private fun createExtensionFile(
        file: File,
        selectedModel: AllModelsResponseBody.Data.Model,
        relativePath: String,
        project: Project
    ) {
        val extensionFile = file.create()
        val extensionTemplate =
            ShowCreateExtensionPopupAction::class.java.getResource("/templates/extension.template")?.readText()
                ?: return
        val adaptedExtensionTemplate = extensionTemplate.replaceAll(
            Pair("\${model_name}", selectedModel.name),
            Pair("\${path_to_model}", relativePath)
        )
        extensionFile.writeText(adaptedExtensionTemplate)
        extensionFile.openInEditor(project, true)
    }

    private fun openFile(project: Project, file: File?, focus: Boolean = false) {
        if (file == null) return
        val file = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return
        FileEditorManager.getInstance(project).openFile(file, focus)
    }

}