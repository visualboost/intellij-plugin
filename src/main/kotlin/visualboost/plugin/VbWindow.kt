package visualboost.plugin

import com.google.gson.reflect.TypeToken
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefCookie
import com.intellij.ui.jcef.JBCefJSQuery
import kotlinx.coroutines.*
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.handler.CefContextMenuHandler
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import visualboost.plugin.actions.OpenSettingsAction
import visualboost.plugin.api.API
import visualboost.plugin.api.models.JwtContent
import visualboost.plugin.api.models.plan.Plan
import visualboost.plugin.api.models.user.ActivationState
import visualboost.plugin.components.VbWebview
import visualboost.plugin.events.GlobalEvents
import visualboost.plugin.models.GSON
import visualboost.plugin.models.GenerationProcess
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbPluginSettingsConfigurable
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.tasks.impl.pull.queue.PullAfterBuildQueue
import visualboost.plugin.util.CredentialUtil
import visualboost.plugin.util.EnvWriter
import visualboost.plugin.util.showError
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import java.net.URI
import java.net.URLEncoder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


class VbWindow(val project: Project) : CoroutineScope {

    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    lateinit var vbBrowser: VbWebview
    lateinit var jcefBrowser: JBCefBrowser
    lateinit var jsQuery: JBCefJSQuery

    var content: JPanel = JPanel()

    val appSettings = VbAppSettings.getInstance()
    val projectSettings = VbProjectSettings.getInstance(project)

    init {
        content.layout = BorderLayout()
    }

    fun initBrowser(disposable: Disposable) {
        vbBrowser = VbWebview(disposable)
        jcefBrowser = vbBrowser.browser
        jsQuery = JBCefJSQuery.create((jcefBrowser as JBCefBrowserBase))

        GlobalEvents.onSettingsApply = {
            displayWindowContent(it)
        }

        displayWindowContent(appSettings)
        vbBrowser.browser.setErrorPage { code, s, s2 -> "<p>hallo</p>" }
    }

    private fun displayWindowContent(settings: VbAppSettings) {
        launch {
            if (!projectSettings.projectTargetIsSet() || !CredentialUtil.isUserLoggedIn() || !projectSettings.projectSelected()) {
                showFallbackComponent(project)
            } else {
                showWebviewComponent(project)
            }
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

        val projectId = projectSettings.projectId
        if (projectId != null) {
            loadAndDisplayProject(projectId)
        }
    }

    fun loadAndDisplayProject(projectId: String) {
        launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    API.fetchToken()
                } ?: return@launch

                //set jwt as session token
                setJwtCookie(token)

                val jwtContent = JwtContent.fromJwt(token)
                val plan = withContext(Dispatchers.IO) {
                    API.getPlan(token, jwtContent.tenantId)
                }
                setPlanCookie(plan)

                val userState = withContext(Dispatchers.IO) {
                    API.getUserState(token, jwtContent.userId)
                }
                setUserActivationState(userState)

                val vbUrl = API.getProjectUrl(projectId)
                jcefBrowser.loadURL(vbUrl)
                content.updateUI()

            } catch (e: Exception) {
                displayServiceIsNotAvailable()
                project.showError("Error", e.message ?: e.stackTraceToString())
            }
        }
    }

    private fun displayServiceIsNotAvailable() {
        val serviceUnavailableHtml = VbWindow::class.java.getResource("/html/service_not_available.html")?.readText() ?: return
        jcefBrowser.loadHTML(serviceUnavailableHtml)
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
        GlobalEvents.onProjectChanged = {
            loadAndDisplayProject(it)
        }

        Disposer.register(project, jcefBrowser)

        initContextMenu()
        initEventListener(project)
        injectJsEvent()

        initLoadHandler()
//        initFileDownloadHandler()
//        initDisplayHandle(project)

        jcefBrowser.component.name = "WebviewComponent"
        return jcefBrowser.component
    }

    private fun initLoadHandler() {
        jcefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandler {
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

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, httpStatusCode: Int) {
                jcefBrowser.zoomLevel = projectSettings.zoomLevel
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?
            ) {
                displayServiceIsNotAvailable()
            }

        }, jcefBrowser.cefBrowser)
    }

    private fun setUserActivationState(userState: ActivationState) {
        jcefBrowser.jbCefCookieManager.setCookie(
            API.getAppUrl(),
            JBCefCookie("state", userState.toString(), API.DOMAIN, "/", true, false)
        ).get()
    }

    /**
     * set authentication cookie
     */
    private fun setJwtCookie(token: String) {
        setCookie("jwt", token)
    }

    private fun setPlanCookie(plan: Plan) {
        val urlEncodedPlan = URLEncoder.encode(plan.toJsonString(), "utf-8")
        setCookie("plan", urlEncodedPlan)
    }

    private fun setCookie(key: String, value: String) {
        val appUrl = URI(API.getAppUrl()).toURL()
        val secure = appUrl.protocol == "https"
        val host = appUrl.host

        jcefBrowser.jbCefCookieManager.setCookie(
            appUrl.toString(),
            JBCefCookie(key, value, host, "/", secure, false)
        ).get()
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
            pullNewVbVersion(result)
            null
        }
    }

    private fun pullNewVbVersion(result: String) {
        val generationProcesses = getGenerationProcess(result)

        val target = projectSettings.target
        if (target == null) {
            project.showError("Error", "No target configured for project. Skip pull repository.")
            return
        }

        /**
         * Validate if the current project has the right target.
         * Example: If The user generated client code but the current intellij project is defined as backend project, we do not want to trigger a git pull.
         */
        val process = generationProcesses.find { it.target == target } ?: return

        /**
         * Check if set project is same as the current VB-Project
         */
        val settingsProjectId = projectSettings.projectId
        if (settingsProjectId != process.projectId) {
            return
        }

        PullAfterBuildQueue(project, process.version, process.branch).execute()
    }

    /**
     * Reload the files from the physical file system
     */
    private fun refreshSourceDir() {
        val sourceDir = getSourceDir() ?: return

        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceDir) ?: return
        virtualSourceDir.refresh(true, true)
    }

    private fun getSourceDir(): File? {
        val rootDir = getRootDir() ?: return null
        val walk = rootDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.onEnter { it.name != "node_modules" }.find {
            it.isDirectory && it.name == "src"
        }
    }

    fun getRootDir(): File? {
        val contentRoots = ProjectRootManager.getInstance(project).contentRoots
        val rootDirAsVf = contentRoots.firstOrNull() ?: return null
        return File(rootDirAsVf.path)
    }

    private fun injectJsEvent() {
        jcefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandler {
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

                jcefBrowser.cefBrowser.executeJavaScript(
                    injectedJavaScript,
                    jcefBrowser.cefBrowser.url, 0
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

        }, jcefBrowser.cefBrowser)
    }

    private fun initContextMenu() {
        jcefBrowser.jbCefClient.addContextMenuHandler(object : CefContextMenuHandler {
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
                    reloadUrl()
                }
                return true
            }

            override fun onContextMenuDismissed(browser: CefBrowser?, frame: CefFrame?) {
            }

        }, jcefBrowser.cefBrowser)
    }

    fun reloadUrl() {
        jcefBrowser.loadURL(jcefBrowser.cefBrowser.url)
    }

    fun zoomIn(zoomStep: Double = 0.1) {
        if (jcefBrowser.zoomLevel >= 2.0) return
        jcefBrowser.zoomLevel += zoomStep
    }

    fun zoomOut(zoomStep: Double = 0.1) {
        if (jcefBrowser.zoomLevel <= 0.3) return
        jcefBrowser.zoomLevel -= zoomStep
    }

    fun getZoomLevel(): Double {
        return jcefBrowser.zoomLevel
    }


    fun getGenerationProcess(processesAsJson: String): List<GenerationProcess> {
        val listType = object : TypeToken<List<GenerationProcess>>() {}.getType()
        return GSON.gson.fromJson(processesAsJson, listType)
    }

    fun triggerSynchronization(fileName: String, fileContent: String) {
        val settings = appSettings
        val vbProjectId = projectSettings.projectId

        /**
         * Avoid Synchronization if projectId is empty
         */
        if (vbProjectId.isNullOrBlank()) {
            project.showError(
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

        jcefBrowser.cefBrowser.executeJavaScript(
            triggerSyncQuery,
            jcefBrowser.cefBrowser.url, 0
        )
    }


}