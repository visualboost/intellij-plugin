package visualboost.plugin

import com.google.gson.reflect.TypeToken
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandler
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import visualboost.plugin.actions.OpenSettingsAction
import visualboost.plugin.events.GlobalEvents
import visualboost.plugin.icons.IconRes
import visualboost.plugin.models.GSON
import visualboost.plugin.models.GenerationProcess
import visualboost.plugin.settings.VbPluginSettingsConfigurable
import visualboost.plugin.settings.VbPluginSettingsState
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.BufferedReader
import java.io.File
import java.rmi.UnexpectedException
import java.util.concurrent.TimeUnit
import javax.swing.*


class VbWindow(val project: Project) {

    val webView = JBCefBrowser()
    var content: JPanel = JPanel()
    var jsQuery = JBCefJSQuery.create((webView as JBCefBrowserBase))

    val settings = VbPluginSettingsState.getInstance()

    init {
        content.layout = BorderLayout()

        GlobalEvents.onSettingsApply = {
            if (!it.isValid()) {
                showFallbackComponent(project)
            } else {
                showWebviewComponent(project)
            }
        }

        if (!settings.isValid()) {
            showFallbackComponent(project)
        } else {
            showWebviewComponent(project)
        }
    }

    fun isFallbackComponentRendered(): Boolean {
        return content.components.find { it.name == "FallbackComponent" } != null
    }

    fun isWebviewComponentRendered(): Boolean {
        return content.components.find { it.name == "WebviewComponent" } != null
    }

    fun showFallbackComponent(project: Project) {
        if (isWebviewComponentRendered()) {
            content.removeAll()
        }
        val defaultComponent = initFallbackComponent(project)
        content.add(defaultComponent, BorderLayout.CENTER)
        content.updateUI()
    }

    fun showWebviewComponent(project: Project) {
        if (isFallbackComponentRendered()) {
            content.removeAll()
        }
        val browserComponent = initBrowser(project)
        content.add(browserComponent, BorderLayout.CENTER)
        content.updateUI()
    }

    private fun initFallbackComponent(project: Project): JPanel {
        val fallbackComponent = JPanel()
        fallbackComponent.name = "FallbackComponent"

        fallbackComponent.setLayout(GridBagLayout())

        val label = JLabel("Please configure your project before using VisualBoost.")
        val button = JButton("Configure")
        button.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, VbPluginSettingsConfigurable::class.java)
        }

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = Insets(5, 5, 5, 5)

        fallbackComponent.add(label, gbc)
        gbc.gridy = 1

        fallbackComponent.add(button, gbc)

        return fallbackComponent
    }

    private fun initBrowser(project: Project): JComponent {
        //Reload when url changed
        GlobalEvents.onUrlChanged = {
            webView.loadURL(it)
        }

        val vbUrl = settings.url
        webView.loadURL(vbUrl)
        Disposer.register(project, webView)

        initContextMenu()
        initEventListener(project)
        injectJsEvent()
//        initFileDownloadHandler()
//        initDisplayHandle(project)

        webView.component.name = "WebviewComponent"
        return webView.component
    }


//    private fun initDisplayHandle(project: Project) {
//        webView.jbCefClient.addDisplayHandler(object : CefDisplayHandler {
//            override fun onAddressChange(browser: CefBrowser?, frame: CefFrame?, url: String?) {
//            }
//
//            override fun onTitleChange(browser: CefBrowser?, title: String?) {
//            }
//
//            override fun onTooltip(browser: CefBrowser?, text: String?): Boolean {
//                return false
//            }
//
//            override fun onStatusMessage(browser: CefBrowser?, value: String?) {
//            }
//
//            override fun onConsoleMessage(
//                browser: CefBrowser,
//                level: CefSettings.LogSeverity,
//                message: String,
//                source: String,
//                line: Int
//            ): Boolean {
//                if (level == CefSettings.LogSeverity.LOGSEVERITY_ERROR) {
//                    showError(project, "Error during Synchronization", message)
//                }
//                return false
//            }
//
//            override fun onCursorChange(browser: CefBrowser?, cursorType: Int): Boolean {
//                return false
//
//            }
//
//        }, webView.cefBrowser)
//    }


    private fun initEventListener(project: Project) {
        jsQuery.addHandler { result: String ->

            try {
                val generationProcesses = getGenerationProcess(result)

                val target = VbPluginSettingsState.getInstance().target
                if (target == null) {
                    showError(project, "Error", "No target configured for project. Skip pull repository.")
                    return@addHandler null
                }

                /**
                 * Validate if the current project is has the right target.
                 * Example: If The user generated client code but the current intellij project is defined as backend project, we do not want to trigger a git pull.
                 */
                val process = generationProcesses.find { it.target == target } ?: return@addHandler null

                /**
                 * Check if set project is same as the current VB-Project
                 */
                val settings = VbPluginSettingsState.getInstance()
                val settingsProjectId = settings.projectId
                if (settingsProjectId != process.projectId) {
                    return@addHandler null
                }

                val currentBranch = getCurrentBranch(project)
                if (currentBranch == process.branch) {
                    pull(project, process.branch)
                    refreshSourceDir()

                    showInfo(
                        project,
                        "Updated",
                        "Successfully pulled Version ${process.version} from VisualBoost (Branch: ${process.branch}"
                    )
                } else {
                    fetch(project, process.branch)
                    showInfo(
                        project,
                        "Updated",
                        "Successfully fetched Version ${process.version} from VisualBoost. Checkout the branch ${process.branch} to access your changes"
                    )
                }
            } catch (e: Exception) {
                showError(project, "Error", e.message ?: "")
            }

            null
        }
    }

    /**
     * Reload the files from the physical file system
     */
    private fun refreshSourceDir() {
        val contentRoots = ProjectRootManager.getInstance(project).contentRoots
        val rootDirAsVf = contentRoots.firstOrNull() ?: return
        val rootDir = File(rootDirAsVf.path)
        val walk = rootDir.walk(FileWalkDirection.TOP_DOWN)
        val sourceDir = walk.onEnter { it.name != "node_modules" }.find {
            it.isDirectory && it.name == "src"
        } ?: return

        val virtualSourceDir = LocalFileSystem.getInstance().findFileByIoFile(sourceDir) ?: return
        virtualSourceDir.refresh(true, true)
    }

    private fun injectJsEvent() {
        webView.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
            }

            override fun onLoadStart(
                browser: CefBrowser?,
                frame: CefFrame?,
                transitionType: CefRequest.TransitionType?
            ) {
            }

            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                val injectedJavaScript =
                    """
                            window.plugin = 'intellij';
                            window.addEventListener('vb_action_build', function (e) {
                                const processes = e.detail.processes;
                                ${jsQuery.inject("processes")}
                            });
    
                    """.trimIndent()

                webView.cefBrowser.executeJavaScript(
                    injectedJavaScript,
                    webView.cefBrowser.url, 0
                );
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?
            ) {
            }

        }, webView.cefBrowser)
    }

    private fun initContextMenu() {
        webView.jbCefClient.addContextMenuHandler(object : CefContextMenuHandler {
            override fun onBeforeContextMenu(
                browser: CefBrowser,
                frame: CefFrame,
                params: CefContextMenuParams,
                model: CefMenuModel
            ) {
                model.addSeparator()
                val subMenu = model.addSubMenu(50000, "VisualBoost")
                subMenu.addItem(50001, "Reload")
            }

            override fun onContextMenuCommand(
                browser: CefBrowser?,
                frame: CefFrame?,
                params: CefContextMenuParams?,
                commandId: Int,
                eventFlags: Int
            ): Boolean {
                if (commandId == 50001) {
                    webView.loadURL(webView.cefBrowser.url)
                }
                return true
            }

            override fun onContextMenuDismissed(browser: CefBrowser?, frame: CefFrame?) {
            }

        }, webView.cefBrowser)
    }

    fun String.runCommand(project: Project): String {
        val workingDir = File(project.basePath)

        val process = ProcessBuilder(*this.split(" ").toTypedArray())
            .directory(workingDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()

        process.waitFor(20, TimeUnit.SECONDS)
        val returnValue = process.exitValue()

        if (returnValue != 0) {
            val errorMsg = BufferedReader(process.errorStream.reader()).readText()
            throw UnexpectedException(errorMsg)
        }

        val result = BufferedReader(process.inputStream.reader()).readText()
        return result
    }

    fun getGenerationProcess(processesAsJson: String): List<GenerationProcess> {
        val listType = object : TypeToken<List<GenerationProcess>>() {}.getType()
        return GSON.gson.fromJson<List<GenerationProcess>>(processesAsJson, listType)
    }

    fun fetch(project: Project, branch: String) {
        "git fetch origin $branch:$branch".runCommand(project)
    }

    fun pull(project: Project, branch: String) {
        "git pull origin $branch:$branch".runCommand(project)
    }

    fun getCurrentBranch(project: Project): String {
        return "git branch --show-current".runCommand(project).replace("\n", "")
    }

    fun showError(project: Project, title: String, msg: String, actions: List<AnAction> = emptyList()) {
        showNotification(project, title, msg, NotificationType.ERROR, actions = actions)
    }

    fun showInfo(project: Project, title: String, msg: String) {
        showNotification(project, title, msg, NotificationType.INFORMATION, IconRes.CheckIcon)
    }

    private fun showNotification(
        project: Project,
        title: String,
        msg: String,
        type: NotificationType,
        icon: Icon? = null,
        actions: List<AnAction> = emptyList()
    ) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("visualboost.notification")
            .createNotification(title, msg, type)
            .setIcon(icon)

        actions.forEach { notification.addAction(it) }

        notification.notify(project)
    }

    fun triggerSynchronization(fileName: String, fileContent: String) {
        val settings = VbPluginSettingsState.getInstance()
        val vbProjectId = settings.projectId

        /**
         * Avoid Synchronization if projectId is empty
         */
        if (vbProjectId.isBlank()) {
            showError(
                project,
                "Error",
                "No projectId is defined in VisualBoost-Settings. Please provide the id of your VisualBoost-Project to enable Synchronization.",
                actions = listOf(OpenSettingsAction())
            )
            return
        }


        val fileInputAsJson = GSON.gson.toJson(mapOf("fileName" to fileName, "fileContent" to fileContent))

        val triggerSyncQuery =
            """
            try {
                const syncEvent = new CustomEvent("vb_action_sync", { detail : { fileInput: $fileInputAsJson }});
                window.dispatchEvent(syncEvent)
            } catch(e) {
                console.error(e)
            }
                """.trimIndent()

        webView.cefBrowser.executeJavaScript(
            triggerSyncQuery,
            webView.cefBrowser.url, 0
        )
    }
}